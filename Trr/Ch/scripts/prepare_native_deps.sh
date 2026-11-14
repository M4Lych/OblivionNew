#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NATIVE="$ROOT/app/src/main/cpp"

clone_or_update() {
  local repository="$1"
  local destination="$2"
  local ref="$3"
  if [[ -d "$destination/.git" ]]; then
    git -C "$destination" fetch --depth=1 origin "$ref"
    git -C "$destination" checkout --force FETCH_HEAD
  else
    rm -rf "$destination"
    git clone --depth=1 --branch "$ref" "$repository" "$destination"
  fi
}

clone_or_update \
  "https://github.com/LiteLDev/preloader-android.git" \
  "$NATIVE/preloader" \
  "0.2.3"

clone_or_update \
  "https://github.com/microsoft/libHttpClient.git" \
  "$NATIVE/libHttpClient" \
  "main"

[[ -f "$NATIVE/preloader/CMakeLists.txt" ]] || { echo "preloader CMakeLists.txt missing" >&2; exit 1; }
[[ -f "$NATIVE/libHttpClient/Build/libHttpClient.Android/CMakeLists.txt" ]] || {
  echo "libHttpClient Android CMakeLists.txt missing" >&2
  exit 1
}

echo "Native dependencies are ready."
