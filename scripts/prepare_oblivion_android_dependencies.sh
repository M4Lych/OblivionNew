#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CPP="$ROOT/app/src/main/cpp"
PRELOADER_SHA="cc08b40a31471d3e2dedb267f5f17b28da2b0604"
HTTPCLIENT_SHA="f8ae1d9cf931d9d55542865eee395adca5bdbe41"

fetch_pinned_repo() {
  local url="$1" destination="$2" sha="$3"
  if [[ -d "$destination/.git" ]]; then
    git -C "$destination" fetch --depth=1 origin "$sha"
    git -C "$destination" checkout --detach "$sha"
  else
    rm -rf "$destination"
    git clone --filter=blob:none "$url" "$destination"
    git -C "$destination" fetch --depth=1 origin "$sha"
    git -C "$destination" checkout --detach "$sha"
  fi
}

mkdir -p "$CPP"
fetch_pinned_repo "https://github.com/LiteLDev/preloader-android.git" "$CPP/preloader" "$PRELOADER_SHA"
fetch_pinned_repo "https://github.com/microsoft/libHttpClient.git" "$CPP/libHttpClient" "$HTTPCLIENT_SHA"

git -C "$CPP/libHttpClient" submodule sync --recursive
git -C "$CPP/libHttpClient" submodule update --init --recursive

# Restore missing upstream Zstd CMake build configuration.
ZSTD_VENDOR="$CPP/worldtools/vendor/zstd"

if [[ ! -f "$ZSTD_VENDOR/build/cmake/CMakeLists.txt" ]]; then
  echo "Restoring Zstd 1.5.7 CMake configuration..."

  ZSTD_TMP="$(mktemp -d)"
  trap 'rm -rf "$ZSTD_TMP"' EXIT

  git clone --depth=1 --branch v1.5.7 \
    https://github.com/facebook/zstd.git "$ZSTD_TMP"

  mkdir -p "$ZSTD_VENDOR/build"

  rm -rf "$ZSTD_VENDOR/build/cmake"

  cp -a "$ZSTD_TMP/build/cmake" \
    "$ZSTD_VENDOR/build/cmake"

  rm -rf "$ZSTD_TMP"
  trap - EXIT
fi

if [[ ! -f "$ZSTD_VENDOR/build/cmake/CMakeLists.txt" ]]; then
  echo "::error::Zstd CMake configuration is missing."
  exit 1
fi

echo "Zstd CMake configuration is ready."

# LeviLamina's Android preloader exports JNI symbols using its upstream application package.
# Oblivion changed the Java namespace, so adapt only this pinned bridge source at build time.
INPUT_JNI="$CPP/preloader/src/pl/runtime/InputJni.cpp"
if [[ ! -f "$INPUT_JNI" ]]; then
  echo "::error::Pinned Android preloader InputJni.cpp was not found."
  exit 1
fi
sed -i 's/Java_org_levimc_launcher_preloader_PreloaderInput_/Java_com_oblivion_client_preloader_PreloaderInput_/g' "$INPUT_JNI"

# Catch an upstream source layout/API change before starting the expensive native build.
if grep -q 'Java_org_levimc_launcher_preloader_PreloaderInput_' "$INPUT_JNI"; then
  echo "::error::JNI package bridge rename was incomplete."
  exit 1
fi

echo "Pinned native dependencies are ready for Oblivion."
