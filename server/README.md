# Oblivion server (reference implementation, untested in production)

Provides what a client alone cannot do safely: **Discord roles / membership**, **one-time claim codes**,
and **protected package delivery**. The Android app only talks to it through `oblivion_api_base`.

1. Create a Discord application; enable OAuth2 and add the redirect `<PUBLIC_BASE_URL>/v1/auth/discord/callback`.
   Create a bot, enable the *Server Members* intent, invite it to your server.
2. `cp .env.example .env` and fill it in (role ids -> Oblivion roles in `ROLE_MAP`).
3. `pip install -r requirements.txt && python gen_keys.py` -> copy the printed public key.
4. In the app set (`app/src/main/res/values/oblivion_config.xml`): `oblivion_api_base`, `oblivion_server_pubkey`,
   `oblivion_discord_invite`, ...
5. `uvicorn main:app --port 8080` (behind HTTPS) and `python bot.py`.

Security notes: the app verifies a signature on every role answer, but a modified APK can still skip checks, so
anything valuable must be enforced here (claim codes are redeemed atomically; packages are only served to
entitled accounts). Files the game must read cannot be fully hidden once Minecraft loads them.
