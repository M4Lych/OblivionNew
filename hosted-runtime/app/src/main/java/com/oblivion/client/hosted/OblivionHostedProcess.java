package com.oblivion.client.hosted;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;

/** Owns the installed Minecraft package context and its PathClassLoader. */
public final class OblivionHostedProcess {
    public static final String MINECRAFT_PACKAGE = "com.mojang.minecraftpe";
    public static final String MINECRAFT_ACTIVITY = "com.mojang.minecraftpe.MainActivity";
    private static final String TAG = "OblivionHosted";

    private static volatile Context minecraftContext;
    private static volatile ClassLoader minecraftClassLoader;
    private static volatile String nativeLibraryPath;

    private OblivionHostedProcess() {}

    public static synchronized void initialize(Context hostContext) throws Exception {
        if (minecraftContext != null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw new IllegalStateException("Android 10+ is required for pre-create context adaptation");
        }

        Context gameContext = hostContext.createPackageContext(
                MINECRAFT_PACKAGE,
                Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        ApplicationInfo gameInfo = gameContext.getApplicationInfo();
        String libDir = gameInfo.nativeLibraryDir;
        if (libDir == null || libDir.isEmpty()) {
            throw new IllegalStateException("Minecraft nativeLibraryDir is empty");
        }
        File realLibrary = new File(libDir, "libminecraftpe.so");
        if (!realLibrary.isFile() || !realLibrary.canRead()) {
            throw new IllegalStateException("Installed Minecraft native library is not readable: " + realLibrary);
        }

        ClassLoader gameLoader = gameContext.getClassLoader();
        Class<?> mainActivity = Class.forName(MINECRAFT_ACTIVITY, false, gameLoader);
        if (!android.app.Activity.class.isAssignableFrom(mainActivity)) {
            throw new IllegalStateException("Minecraft MainActivity is not an Android Activity");
        }

        OblivionNativeBridge.load(realLibrary.getAbsolutePath());
        minecraftContext = gameContext;
        minecraftClassLoader = gameLoader;
        nativeLibraryPath = realLibrary.getAbsolutePath();
        Thread.currentThread().setContextClassLoader(gameLoader);

        Log.i(TAG, "Game package context ready: " + gameInfo.packageName);
        Log.i(TAG, "Minecraft class loader: " + gameLoader.getClass().getName());
        Log.i(TAG, "Minecraft native library path resolved");
    }

    public static Context requireMinecraftContext() {
        Context value = minecraftContext;
        if (value == null) throw new IllegalStateException("Minecraft package context is not initialized");
        return value;
    }

    public static ClassLoader requireMinecraftClassLoader() {
        ClassLoader value = minecraftClassLoader;
        if (value == null) throw new IllegalStateException("Minecraft class loader is not initialized");
        return value;
    }

    public static String requireNativeLibraryPath() {
        String value = nativeLibraryPath;
        if (value == null) throw new IllegalStateException("Minecraft native library path is not initialized");
        return value;
    }

    static boolean isMinecraftActivity(android.app.Activity activity) {
        return MINECRAFT_ACTIVITY.equals(activity.getClass().getName());
    }

    static int getMinecraftActivityTheme(Context hostContext) throws PackageManager.NameNotFoundException {
        android.content.ComponentName name = new android.content.ComponentName(
                MINECRAFT_PACKAGE, MINECRAFT_ACTIVITY);
        android.content.pm.ActivityInfo info = hostContext.getPackageManager().getActivityInfo(
                name, PackageManager.GET_META_DATA);
        return info.getThemeResource();
    }
}
