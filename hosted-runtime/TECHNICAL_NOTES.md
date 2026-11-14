# Technical Notes

## Execution flow

1. Android creates the app Application in `:minecraft`.
2. `OblivionApplication` initializes the installed Minecraft package context using `CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY`.
3. `OblivionHostedProcess` obtains the Minecraft PathClassLoader, checks that the real `MainActivity` is an Activity, and records the absolute path of `libminecraftpe.so`.
4. `OblivionNativeBridge` loads the app-owned forwarder and sends it the real library path.
5. Android asks `OblivionHostedAppComponentFactory` to instantiate the manifest Activity. The factory creates the class from Minecraft's loader.
6. On API 29+, the pre-create callback swaps the Activity base Context to a wrapper that exposes Minecraft code/resources while keeping writable storage under the host package.
7. Android's NativeActivity machinery loads `liboblivion_minecraftpe.so`; the exported native Activity entry point opens the installed library and forwards `ANativeActivity_onCreate`.

## Why there is no copied MainActivity

The app does not compile or package the Minecraft Java Activity. It resolves the class from the installed official package at runtime. This is the key architectural difference from the current static `MinecraftActivity : com.mojang.minecraftpe.MainActivity` integration.

## Deliberately excluded

This prototype does not copy upstream hook code or manipulate Minecraft process memory. It only attempts to construct the installed Activity and forward native startup. It does not bypass Play Store licensing or package identity checks.

## Critical test points

- Whether package context creation is allowed on the target Android/Minecraft build.
- Whether the installed package exposes the expected Java Activity and readable native library path.
- Whether Android accepts the Activity instance from the Minecraft class loader.
- Whether the base Context reflection is permitted by the OS build.
- Whether `libminecraftpe.so` exports the native Activity entry point and can be opened in the hosted process.
- Whether the game can access its expected writable storage under Oblivion's package sandbox.
