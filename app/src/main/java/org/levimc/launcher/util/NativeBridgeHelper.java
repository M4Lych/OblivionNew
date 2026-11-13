package org.levimc.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Keep;

import com.oblivion.client.core.minecraft.OblivionApplication;

/**
 * JNI compatibility entry point for the closed LeviLauncher libgxcore.so binary.
 *
 * The binary is built against the original org.levimc.launcher.util.NativeBridgeHelper
 * class name. Keep this class and the four native method signatures stable even though
 * the application itself uses the com.oblivion.client namespace.
 */
@Keep
public final class NativeBridgeHelper {
    public static final NativeBridgeHelper INSTANCE = new NativeBridgeHelper();

    private NativeBridgeHelper() {}

    @Keep
    public static Context getAppContext() {
        return OblivionApplication.getContext();
    }

    @Keep
    public static boolean scanImage(String inputPath) {
        return nativeScanImage(inputPath);
    }

    @Keep
    public static boolean rewriteImage(String inputPath, String outputPath) {
        return nativeRewriteImage(inputPath, outputPath);
    }

    @Keep
    public static boolean bootstrapGxCore() {
        try {
            nativeBootstrapGxCore();
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    @Keep
    public static String loadMinecraftRuntime(String libraryPath) {
        return nativeLoadMinecraftRuntime(libraryPath);
    }

    @Keep
    public static SharedPreferences getPrefs(String name) {
        return getAppContext().getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    @Keep
    public static boolean getBoolean(String prefsName, String key, boolean defaultValue) {
        try {
            return getPrefs(prefsName).getBoolean(key, defaultValue);
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }

    @Keep
    public static void putBoolean(String prefsName, String key, boolean value) {
        try {
            getPrefs(prefsName).edit().putBoolean(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    @Keep
    public static String getString(String prefsName, String key, String defaultValue) {
        try {
            return getPrefs(prefsName).getString(key, defaultValue);
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }

    @Keep
    public static void putString(String prefsName, String key, String value) {
        try {
            getPrefs(prefsName).edit().putString(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    @Keep
    public static boolean isLauncherManagedLoginEnabled() {
        return com.oblivion.client.util.NativeBridgeHelper.isLauncherManagedLoginEnabled();
    }

    @Keep
    public static void showInvalidLicenseOverlay() {
        com.oblivion.client.util.NativeBridgeHelper.showInvalidLicenseOverlay();
    }

    // These signatures are registered by JNI_OnLoad in libgxcore.so.
    @Keep
    private static native boolean nativeScanImage(String inputPath);

    @Keep
    private static native boolean nativeRewriteImage(String inputPath, String outputPath);

    @Keep
    private static native void nativeBootstrapGxCore();

    @Keep
    private static native String nativeLoadMinecraftRuntime(String libraryPath);
}
