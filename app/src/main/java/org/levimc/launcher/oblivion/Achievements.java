package org.levimc.launcher.oblivion;

import android.content.Context;
import android.content.SharedPreferences;

/** Client-side achievements (counters kept on the device). Minecraft's own Xbox achievements are not read. */
public final class Achievements {
    public static final String LAUNCHES = "launches";
    public static final String SKINS = "skins_applied";
    public static final String CODES = "codes_claimed";
    public static final String DLC = "dlc_installed";
    public static final String COSMETICS = "cosmetics_worn";

    public static final class Def {
        public final String counter;
        public final int goal;
        public final int titleRes;

        Def(String counter, int goal, int titleRes) {
            this.counter = counter;
            this.goal = goal;
            this.titleRes = titleRes;
        }
    }

    public static final Def[] ALL = {
            new Def(LAUNCHES, 1, org.levimc.launcher.R.string.oblivion_ach_first_launch),
            new Def(LAUNCHES, 25, org.levimc.launcher.R.string.oblivion_ach_launch_25),
            new Def(SKINS, 1, org.levimc.launcher.R.string.oblivion_ach_skin),
            new Def(COSMETICS, 1, org.levimc.launcher.R.string.oblivion_ach_cosmetic),
            new Def(CODES, 1, org.levimc.launcher.R.string.oblivion_ach_code),
            new Def(DLC, 1, org.levimc.launcher.R.string.oblivion_ach_dlc),
    };

    private Achievements() {
    }

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("oblivion_achievements", Context.MODE_PRIVATE);
    }

    public static int get(Context c, String counter) {
        return p(c).getInt(counter, 0);
    }

    public static void increment(Context c, String counter) {
        p(c).edit().putInt(counter, get(c, counter) + 1).apply();
    }
}
