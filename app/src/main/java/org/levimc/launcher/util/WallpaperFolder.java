package org.levimc.launcher.util;

import android.content.Context;

import java.util.function.Consumer;

/** Oblivion: wallpaper rotation was removed; kept as a no-op shim for BaseActivity. */
public final class WallpaperFolder {
    private WallpaperFolder() {
    }

    public static void next(Context context, Consumer<Boolean> callback) {
        if (callback != null) callback.accept(false);
    }

    public static void rotateOnLaunch(Context context) {
    }

    public static int count(Context context, String treeUri) {
        return 0;
    }
}
