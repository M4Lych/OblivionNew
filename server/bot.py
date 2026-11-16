"""Oblivion Discord bot. /code create (staff) generates one-time claim codes; /whois shows Oblivion roles.
Run next to main.py:  python bot.py   (needs the 'Server Members' intent enabled for the bot)"""
import os

import discord
from discord import app_commands

import core

GUILD = discord.Object(id=int(os.getenv("GUILD_ID", "0")))
intents = discord.Intents.default()
intents.members = True
client = discord.Client(intents=intents)
tree = app_commands.CommandTree(client)


def roles_of(member):
    return core.map_roles([str(r.id) for r in member.roles])


@tree.command(name="code", description="Create one-time Oblivion claim codes (staff only)", guild=GUILD)
@app_commands.describe(count="How many codes", title="What the code gives", entitlement="Cosmetic id, e.g. mask_gold",
                       package_id="Optional protected package id")
async def code(inter: discord.Interaction, count: int, title: str, entitlement: str = "", package_id: str = ""):
    if not (set(roles_of(inter.user)) & core.CODE_CREATOR_ROLES):
        await inter.response.send_message("You are not allowed to create codes.", ephemeral=True)
        return
    con = core.db()
    codes = core.make_codes(con, max(1, min(count, 50)), title, entitlement, package_id, str(inter.user.id))
    await inter.response.send_message("```\n" + "\n".join(codes) + "\n```", ephemeral=True)


@tree.command(name="whois", description="Show a member's Oblivion roles", guild=GUILD)
async def whois(inter: discord.Interaction, member: discord.Member):
    await inter.response.send_message(f"{member.display_name}: {', '.join(roles_of(member))}", ephemeral=True)


@client.event
async def on_ready():
    await tree.sync(guild=GUILD)
    print("Oblivion bot ready as", client.user)


client.run(os.getenv("DISCORD_BOT_TOKEN", ""))
