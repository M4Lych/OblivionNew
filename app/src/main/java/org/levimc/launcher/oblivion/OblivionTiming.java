package org.levimc.launcher.oblivion;

/**
 * Animation lengths. Edit these constants to change how long the intro and the loading animation play.
 * An OblivionDLC package may override them with "intro_ms" / "loading_ms" in dlc.json.
 */
public final class OblivionTiming {
    /** Intro animation when the client is opened (ms). */
    public static volatile long INTRO_MS = 5000L;
    /** Minimum time the loading animation plays after LAUNCH before Minecraft is shown (ms). */
    public static volatile long LOADING_MS = 10000L;

    private OblivionTiming() {
    }

    public static long remainingLoadingMs(long startedAtElapsedMs) {
        long elapsed = android.os.SystemClock.elapsedRealtime() - startedAtElapsedMs;
        return Math.max(0L, LOADING_MS - elapsed);
    }
}
