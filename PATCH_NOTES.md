# Oblivion — GXCore JNI compatibility test patch

## What this patch tests

The `libgxcore.so` shipped in Oblivion is byte-for-byte identical to the LeviLauncher binary. It exposes `JNI_OnLoad`, but no exported `Java_*` JNI entry points. The matching LeviLauncher source declares the four `native*` methods on `org.levimc.launcher.util.NativeBridgeHelper`; Oblivion had moved that class to `com.oblivion.client.util.NativeBridgeHelper` while retaining the same closed native binary.

That package mismatch is a high-confidence JNI compatibility hypothesis for the observed `JNI_ERR returned from JNI_OnLoad`. This patch adds a compatibility class at the original LeviLauncher class path, with the same native method names and JVM descriptors, and forwards the Oblivion helper's calls through it.

## Files

- `app/src/main/java/org/levimc/launcher/util/NativeBridgeHelper.java` — compatibility class matching the upstream JNI owner name and native signatures.
- `app/src/main/java/com/oblivion/client/util/NativeBridgeHelper.kt` — delegates native operations through the compatibility class while keeping Oblivion's existing app-facing helper.
- `app/proguard-rules.pro` — keeps the compatibility class and its native entry points from being renamed or removed.

## Validation status

- Java shim syntax/type check against Android API stubs: PASS.
- Kotlin helper syntax/type check against minimal Android/AndroidX stubs: PASS.
- Gradle Android build: NOT RUN.
- Installation and physical-device launch: NOT RUN.

This is a controlled diagnostic patch, not a verified runtime fix. Build it and test on the target device before treating the issue as resolved.

## Separate Discord SDK finding

`LeviLib.zip` contains native `.so` files, including `libdiscord_partner_sdk.so`; it does not contain the Discord Social SDK AAR or its Java classes. The current LeviLauncher APK contains `com.discord.socialsdk.*` and `com.discord.org.webrtc.*` classes; the inspected Oblivion APK does not. The partner SDK native library exports JNI functions for those Java classes. This may be a separate compatibility gap. This patch does not invent or reconstruct the separately licensed SDK Java layer.

If the JNI error remains after this test, the next step is to use an authorized `discord_partner_sdk.aar` matching the upstream pinned SDK, then capture the complete `JNI_OnLoad`/linker log again.
