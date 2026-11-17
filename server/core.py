"""Shared logic for the Oblivion API and the Discord bot."""
import base64
import json
import os
import secrets
import sqlite3
import time

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from dotenv import load_dotenv

load_dotenv()

DB_PATH = os.getenv("DB_PATH", "oblivion.db")
ROLE_MAP = json.loads(os.getenv("ROLE_MAP", "{}"))
CODE_CREATOR_ROLES = {r.strip() for r in os.getenv("CODE_CREATOR_ROLES", "oblivion,admin,staff").split(",") if r.strip()}
RANKS = {"oblivion": 100, "admin": 90, "staff": 80, "creator": 60, "partner": 55,
         "oblivion_plus": 40, "member": 20, "newcomer": 10}


def db():
    con = sqlite3.connect(DB_PATH, isolation_level=None, timeout=30)
    con.row_factory = sqlite3.Row
    con.executescript("""
    CREATE TABLE IF NOT EXISTS sessions(token TEXT PRIMARY KEY, discord_id TEXT, username TEXT, avatar TEXT,
        roles TEXT, created REAL);
    CREATE TABLE IF NOT EXISTS codes(code TEXT PRIMARY KEY, title TEXT, entitlement TEXT, package_id TEXT,
        used_by TEXT, used_at REAL, created_by TEXT, created REAL);
    CREATE TABLE IF NOT EXISTS entitlements(discord_id TEXT, entitlement TEXT, PRIMARY KEY(discord_id, entitlement));
    CREATE TABLE IF NOT EXISTS friends(a TEXT, b TEXT, status TEXT, PRIMARY KEY(a, b));
    CREATE TABLE IF NOT EXISTS parties(id TEXT PRIMARY KEY, name TEXT, owner TEXT, created REAL);
    CREATE TABLE IF NOT EXISTS party_members(party_id TEXT, discord_id TEXT, PRIMARY KEY(party_id, discord_id));
    CREATE TABLE IF NOT EXISTS messages(id INTEGER PRIMARY KEY AUTOINCREMENT, party_id TEXT, discord_id TEXT,
        username TEXT, text TEXT, ts REAL);
    CREATE TABLE IF NOT EXISTS dlc(id TEXT PRIMARY KEY, name TEXT, description TEXT, package_id TEXT);
    CREATE TABLE IF NOT EXISTS oauth_state(state TEXT PRIMARY KEY, redirect TEXT, created REAL);
    """)
    return con


def map_roles(discord_role_ids, joined_ts=None):
    roles = {ROLE_MAP[r] for r in discord_role_ids if r in ROLE_MAP}
    if not roles:
        roles.add("member")
    if roles == {"member"} and joined_ts and time.time() - joined_ts < 14 * 86400:
        roles = {"newcomer"}
    return sorted(roles, key=lambda r: -RANKS.get(r, 0))


def _key():
    with open(os.getenv("SIGNING_KEY_PATH", "signing_key.pem"), "rb") as f:
        return serialization.load_pem_private_key(f.read(), password=None)


def sign_envelope(payload: dict) -> dict:
    text = json.dumps(payload, separators=(",", ":"), ensure_ascii=False)
    sig = _key().sign(text.encode("utf-8"), ec.ECDSA(hashes.SHA256()))
    return {"payload": text, "sig": base64.b64encode(sig).decode()}


def new_token() -> str:
    return secrets.token_urlsafe(32)


def make_codes(con, count, title, entitlement, package_id, created_by):
    out = []
    for _ in range(count):
        code = "-".join(secrets.token_hex(2).upper() for _ in range(3))
        con.execute("INSERT INTO codes(code,title,entitlement,package_id,created_by,created) VALUES(?,?,?,?,?,?)",
                    (code, title, entitlement, package_id or "", created_by, time.time()))
        out.append(code)
    return out


def redeem(con, code, discord_id):
    """Atomic one-time redemption. Returns the code row or None if unknown/already used."""
    code = code.strip().upper()
    cur = con.execute("UPDATE codes SET used_by=?, used_at=? WHERE code=? AND used_by IS NULL",
                      (discord_id, time.time(), code))
    if cur.rowcount != 1:
        return None
    row = con.execute("SELECT * FROM codes WHERE code=?", (code,)).fetchone()
    if row["entitlement"]:
        con.execute("INSERT OR IGNORE INTO entitlements(discord_id, entitlement) VALUES(?,?)",
                    (discord_id, row["entitlement"]))
    if row["package_id"]:
        con.execute("INSERT OR IGNORE INTO entitlements(discord_id, entitlement) VALUES(?,?)",
                    (discord_id, "pkg:" + row["package_id"]))
    return row


def name_of(con, discord_id):
    row = con.execute("SELECT username FROM sessions WHERE discord_id=? ORDER BY created DESC LIMIT 1",
                      (discord_id,)).fetchone()
    return row["username"] if row else discord_id


def are_friends(con, a, b):
    row = con.execute("SELECT 1 FROM friends WHERE status='accepted' AND ((a=? AND b=?) OR (a=? AND b=?))",
                      (a, b, b, a)).fetchone()
    return row is not None
