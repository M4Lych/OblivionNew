# Oblivion Android CI repair — run 43640439827

## Root cause

The native build stopped during CMake configuration at `worldtools/CMakeLists.txt:14`.
That file calls `add_subdirectory(vendor/zstd/build/cmake)`, but the repository did not contain
`app/src/main/cpp/worldtools/vendor/zstd/build/cmake/`. The Zstandard C sources were present;
the CMake build integration directory was omitted from the Oblivion source tree.

## Repair included

- Restored the matching `build/cmake` directory from the supplied LeviLauncher source archive.
- Updated `.gitignore` so this required vendored CMake directory is retained by Git.
- Added an early CI check for the Zstandard CMakeLists file before Gradle starts.
- Kept the previous Android SDK package-list fix.

## Apply to the GitHub repository

Extract this ZIP into the repository root, preserving paths, then stage and push the changes:

```sh
git add .gitignore .github/workflows/android.yml app/src/main/cpp/worldtools/vendor/zstd/build/cmake
git status --short
git commit -m "Restore vendored zstd CMake files for Android build"
git push origin Patch-empty
```

## Verification

A host CMake configure of `app/src/main/cpp/worldtools` completed successfully after the missing
directory was restored. This verifies that the reported missing-directory configure error is fixed.
It does **not** verify the full Android/NDK build or APK runtime; rerun GitHub Actions for that.
