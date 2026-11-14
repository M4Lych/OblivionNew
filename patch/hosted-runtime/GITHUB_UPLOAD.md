# Hosted Runtime inside the consolidated OblivionNew source package

The full project archive contains the existing Oblivion Android source plus this standalone hosted-runtime proof-of-concept. `hosted-runtime/` is intentionally a separate Gradle project; it is not wired into the production app launch path.

## Recommended branch workflow

Keep `main` unchanged. Create a working branch named `Patch-Rebuild` from `main` (or use `Patch` if that is the branch you intend to update). Extract the complete ZIP at the repository root so that `app/`, `minecraft/`, `oblivion_controls/`, `scripts/`, `hosted-runtime/`, and `.github/` are directly inside the repository root.

## Termux commands

```bash
cd ~/OblivionNew-work
git switch main
git pull --ff-only origin main
git switch -c Patch-Rebuild
```

If `Patch-Rebuild` already exists, use `git switch Patch-Rebuild` instead of creating it again. Merge/extract the archive into this repository root, then run both source checks:

```bash
python3 scripts/verify_oblivion_source.py
python3 hosted-runtime/scripts/verify_source.py
git status --short
```

Review the changed files before committing. The root `android.yml` workflow builds the main client, while `oblivion-hosted-runtime.yml` independently builds only the experimental hosted-runtime module. Both workflows listen to `Patch` and `Patch-Rebuild` pushes.

A successful source preflight is not proof of Gradle compilation or game launch. The hosted-runtime APK and the production app both still require build validation; Minecraft launch must be tested on a real ARM64 device with a recorded Minecraft version.

## Safety and scope

- Do not upload Minecraft APKs, game libraries, account files, signing keys, or tokens.
- The separate Discord Partner SDK is licensed separately and is not included in this source archive. Provide an authorized compatible SDK through the documented local dependency route before attempting a production APK build.
- No repository push or commit is performed by this package.
