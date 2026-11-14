package com.oblivion.client.hosted;

/** JNI bridge for the small, version-independent native-activity forwarder. */
public final class OblivionNativeBridge {
    private OblivionNativeBridge() {}

    public static void load(String realMinecraftLibrary) {
        System.loadLibrary("oblivion_minecraftpe");
        nativeSetRealLibraryPath(realMinecraftLibrary);
    }

    private static native void nativeSetRealLibraryPath(String path);
}
