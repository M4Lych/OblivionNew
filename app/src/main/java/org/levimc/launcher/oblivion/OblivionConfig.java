package org.levimc.launcher.oblivion;

import android.content.Context;

import org.levimc.launcher.R;

public final class OblivionConfig {
    private OblivionConfig() {
    }

    public static String apiBase(Context c) {
        String v = c.getString(R.string.oblivion_api_base).trim();
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        return v;
    }

    public static boolean isServerConfigured(Context c) {
        return apiBase(c).startsWith("http");
    }

    public static String serverPublicKey(Context c) {
        return c.getString(R.string.oblivion_server_pubkey).trim();
    }

    public static String newsUrl(Context c) {
        return c.getString(R.string.oblivion_news_url).trim();
    }

    public static String discordInvite(Context c) {
        return c.getString(R.string.oblivion_discord_invite).trim();
    }

    public static String authScheme() {
        return "oblivion";
    }

    public static String authHost() {
        return "auth";
    }
}
