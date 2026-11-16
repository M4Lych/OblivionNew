"""Oblivion API: Discord login -> roles/membership, one-time claim codes, protected package delivery.
Run: uvicorn main:app --host 0.0.0.0 --port 8080   (put it behind HTTPS)"""
import os
import re
import time
from urllib.parse import urlencode

import httpx
from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import FileResponse, JSONResponse, RedirectResponse
from pydantic import BaseModel

import core

app = FastAPI(title="Oblivion API")
DISCORD = "https://discord.com/api/v10"
CLIENT_ID = os.getenv("DISCORD_CLIENT_ID", "")
CLIENT_SECRET = os.getenv("DISCORD_CLIENT_SECRET", "")
BOT_TOKEN = os.getenv("DISCORD_BOT_TOKEN", "")
GUILD_ID = os.getenv("GUILD_ID", "")
BASE = os.getenv("PUBLIC_BASE_URL", "").rstrip("/")
ADMIN_KEY = os.getenv("ADMIN_KEY", "")
PACKAGES_DIR = os.getenv("PACKAGES_DIR", "packages")


def error(status, message):
    return JSONResponse({"error": message}, status_code=status)


def session_for(authorization):
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(401, "missing token")
    token = authorization[7:].strip()
    con = core.db()
    row = con.execute("SELECT * FROM sessions WHERE token=?", (token,)).fetchone()
    if not row:
        raise HTTPException(401, "invalid session")
    return con, row


@app.get("/v1/auth/discord/login")
def login(redirect: str = "oblivion://auth"):
    if not redirect.startswith("oblivion://"):
        raise HTTPException(400, "bad redirect")
    state = core.new_token()
    con = core.db()
    con.execute("INSERT INTO oauth_state(state, redirect, created) VALUES(?,?,?)", (state, redirect, time.time()))
    q = urlencode({"client_id": CLIENT_ID, "response_type": "code", "scope": "identify guilds.members.read",
                   "redirect_uri": BASE + "/v1/auth/discord/callback", "state": state, "prompt": "none"})
    return RedirectResponse("https://discord.com/oauth2/authorize?" + q)


@app.get("/v1/auth/discord/callback")
async def callback(code: str = "", state: str = ""):
    con = core.db()
    st = con.execute("SELECT * FROM oauth_state WHERE state=?", (state,)).fetchone()
    if not st or time.time() - st["created"] > 600:
        raise HTTPException(400, "state expired")
    con.execute("DELETE FROM oauth_state WHERE state=?", (state,))
    async with httpx.AsyncClient(timeout=15) as http:
        tok = await http.post(DISCORD + "/oauth2/token", data={
            "client_id": CLIENT_ID, "client_secret": CLIENT_SECRET, "grant_type": "authorization_code",
            "code": code, "redirect_uri": BASE + "/v1/auth/discord/callback"})
        if tok.status_code != 200:
            raise HTTPException(400, "discord token exchange failed")
        auth = {"Authorization": "Bearer " + tok.json()["access_token"]}
        me = (await http.get(DISCORD + "/users/@me", headers=auth)).json()
        member = await http.get(f"{DISCORD}/users/@me/guilds/{GUILD_ID}/member", headers=auth)
    if member.status_code != 200:
        return RedirectResponse(st["redirect"] + "?error=not_in_server")
    m = member.json()
    joined = None
    try:
        from datetime import datetime
        joined = datetime.fromisoformat(m["joined_at"].replace("Z", "+00:00")).timestamp()
    except Exception:
        pass
    roles = core.map_roles(m.get("roles", []), joined)
    avatar = (f"https://cdn.discordapp.com/avatars/{me['id']}/{me['avatar']}.png" if me.get("avatar") else "")
    token = core.new_token()
    import json
    con.execute("INSERT INTO sessions(token,discord_id,username,avatar,roles,created) VALUES(?,?,?,?,?,?)",
                (token, me["id"], me.get("global_name") or me["username"], avatar, json.dumps(roles), time.time()))
    return RedirectResponse(st["redirect"] + "?" + urlencode({"token": token}))


async def fresh_roles(row):
    """Re-reads the member's roles with the bot token (if configured) so removed roles take effect."""
    import json
    if not BOT_TOKEN or not GUILD_ID:
        return json.loads(row["roles"])
    async with httpx.AsyncClient(timeout=15) as http:
        r = await http.get(f"{DISCORD}/guilds/{GUILD_ID}/members/{row['discord_id']}",
                           headers={"Authorization": "Bot " + BOT_TOKEN})
    if r.status_code != 200:
        return []
    m = r.json()
    return core.map_roles(m.get("roles", []))


@app.get("/v1/me")
async def me(authorization: str = Header(None)):
    con, row = session_for(authorization)
    roles = await fresh_roles(row)
    ents = [r["entitlement"] for r in con.execute(
        "SELECT entitlement FROM entitlements WHERE discord_id=?", (row["discord_id"],))]
    return core.sign_envelope({"discord_id": row["discord_id"], "username": row["username"],
                               "avatar_url": row["avatar"], "roles": roles, "entitlements": ents,
                               "issued_at": int(time.time())})


class ClaimBody(BaseModel):
    code: str


@app.post("/v1/claim")
def claim(body: ClaimBody, authorization: str = Header(None)):
    con, row = session_for(authorization)
    if not re.fullmatch(r"[A-Z0-9-]{6,40}", body.code.strip().upper()):
        return error(400, "invalid code format")
    res = core.redeem(con, body.code, row["discord_id"])
    if res is None:
        return error(409, "code is invalid or already used")
    return core.sign_envelope({"ok": True, "title": res["title"], "entitlement": res["entitlement"],
                               "package_id": res["package_id"]})


@app.get("/v1/packages/{package_id}")
def package(package_id: str, authorization: str = Header(None)):
    con, row = session_for(authorization)
    if not re.fullmatch(r"[A-Za-z0-9_.-]{1,64}", package_id):
        raise HTTPException(400, "bad id")
    owned = con.execute("SELECT 1 FROM entitlements WHERE discord_id=? AND entitlement=?",
                        (row["discord_id"], "pkg:" + package_id)).fetchone()
    path = os.path.join(PACKAGES_DIR, package_id)
    if not owned or not os.path.isfile(path):
        raise HTTPException(403, "not entitled")
    return FileResponse(path, media_type="application/octet-stream")


class NewCodes(BaseModel):
    count: int = 1
    title: str
    entitlement: str = ""
    package_id: str = ""


@app.post("/admin/codes")
def admin_codes(body: NewCodes, x_admin_key: str = Header(None)):
    if not ADMIN_KEY or x_admin_key != ADMIN_KEY:
        raise HTTPException(403, "forbidden")
    con = core.db()
    return {"codes": core.make_codes(con, max(1, min(body.count, 500)), body.title, body.entitlement,
                                     body.package_id, "admin-api")}
