# Hosted Runtime Test Matrix

Status when this source bundle was created:

| Check | Result |
|---|---|
| Manifest XML parse | PASS |
| Manifest declares app component factory | PASS |
| Hosted Activity uses isolated `:minecraft` process | PASS |
| Native forwarder metadata matches CMake library | PASS |
| Java and C source files present | PASS |
| No LeviLauncher Java package reference in prototype | PASS |
| Python source preflight | PASS |
| Gradle Android build | NOT RUN — Gradle executable and Android SDK/NDK are not available in this execution environment |
| C compiler / Android NDK compile | NOT RUN — Android NDK headers and toolchain are not available here |
| APK install on device | NOT RUN |
| Minecraft launch on device | NOT RUN |

A source-level PASS is not a runtime PASS. Record the exact device and Minecraft version before interpreting any device result.
