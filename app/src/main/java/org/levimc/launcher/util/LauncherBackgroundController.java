package org.levimc.launcher.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import java.util.function.Consumer;

/**
 * Oblivion: the animated/video launcher background of the upstream project was removed.
 * This class keeps the same static entry points as no-ops so the game launch pipeline
 * (which still calls them around process start/stop) does not need to change.
 */
public final class LauncherBackgroundController {
    private LauncherBackgroundController() {
    }

    public static boolean isMinecraftBlocked() {
        return false;
    }

    public static void resume(Activity activity) {
    }

    public static void stopped(Activity activity) {
    }

    public static void navigating(Activity activity) {
    }

    public static void navigationFailed(Activity activity) {
    }

    public static void refreshEffects() {
    }

    public static boolean isImporting() {
        return false;
    }

    public static void backgroundCleared() {
    }

    public static void suspendForMinecraft() {
    }

    public static void awaitShutdown() throws InterruptedException {
    }

    public static void allowLauncherPlayback() {
    }

    public static void launchFailed(Activity activity) {
    }

    public static void importBackground(Context context, Uri uri, Consumer<Boolean> callback) {
        if (callback != null) callback.accept(false);
    }
}
