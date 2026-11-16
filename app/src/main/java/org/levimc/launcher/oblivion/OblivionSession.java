package org.levimc.launcher.oblivion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Holds the Oblivion account (Discord link, roles, membership, entitlements) fetched from the server. */
public final class OblivionSession {
    public static final class Account {
        public String discordId = "";
        public String username = "";
        public String avatarUrl = "";
        public List<OblivionRole> roles = new ArrayList<>();
        public List<String> entitlements = new ArrayList<>();
        public boolean verified;
        public long fetchedAt;

        public OblivionRole highestRole() {
            return OblivionRole.highest(roles);
        }

        public boolean hasMembership() {
            return fresh() && OblivionRole.hasMembership(roles);
        }

        public boolean isStaff() {
            return fresh() && OblivionRole.isStaff(roles);
        }

        public boolean owns(String entitlementId) {
            return fresh() && entitlements.contains(entitlementId);
        }

        /** Perks only count while the signed server answer is recent (7 days) so removed roles expire. */
        public boolean fresh() {
            return System.currentTimeMillis() - fetchedAt < 7L * 24 * 3600 * 1000;
        }
    }

    public interface Callback {
        void done(Account account, String error);
    }

    private static final String PREFS = "oblivion_session";
    private static final String KEY_TOKEN = "token_enc";
    private static final String KEY_ACCOUNT = "account_json";

    private OblivionSession() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String token(Context c) {
        String enc = prefs(c).getString(KEY_TOKEN, null);
        return enc == null ? null : SecureVault.decryptString(enc);
    }

    public static boolean isLinked(Context c) {
        return token(c) != null;
    }

    public static Account current(Context c) {
        String json = prefs(c).getString(KEY_ACCOUNT, null);
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            Account a = new Account();
            a.discordId = o.optString("discordId");
            a.username = o.optString("username");
            a.avatarUrl = o.optString("avatarUrl");
            a.verified = o.optBoolean("verified");
            a.fetchedAt = o.optLong("fetchedAt");
            a.roles = OblivionRole.parse(toList(o.optJSONArray("roles")));
            a.entitlements = toList(o.optJSONArray("entitlements"));
            return a;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> toList(JSONArray arr) {
        List<String> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) out.add(arr.optString(i));
        return out;
    }

    private static void save(Context c, Account a) {
        try {
            JSONObject o = new JSONObject();
            o.put("discordId", a.discordId);
            o.put("username", a.username);
            o.put("avatarUrl", a.avatarUrl);
            o.put("verified", a.verified);
            o.put("fetchedAt", a.fetchedAt);
            JSONArray roles = new JSONArray();
            for (OblivionRole r : a.roles) roles.put(r.key);
            o.put("roles", roles);
            JSONArray ent = new JSONArray();
            for (String s : a.entitlements) ent.put(s);
            o.put("entitlements", ent);
            prefs(c).edit().putString(KEY_ACCOUNT, o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static void beginLogin(Activity activity) {
        String url = OblivionConfig.apiBase(activity) + "/v1/auth/discord/login?redirect="
                + Uri.encode(OblivionConfig.authScheme() + "://" + OblivionConfig.authHost());
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    public static void handleCallback(Context c, Uri uri, Callback cb) {
        String token = uri == null ? null : uri.getQueryParameter("token");
        if (token == null || token.isEmpty()) {
            if (cb != null) cb.done(null, "missing token");
            return;
        }
        String enc = SecureVault.encryptString(token);
        if (enc == null) {
            if (cb != null) cb.done(null, "secure storage unavailable");
            return;
        }
        prefs(c).edit().putString(KEY_TOKEN, enc).apply();
        refresh(c, cb);
    }

    public static void refresh(Context c, Callback cb) {
        String token = token(c);
        if (token == null) {
            if (cb != null) cb.done(null, "not linked");
            return;
        }
        Context app = c.getApplicationContext();
        OblivionApi.get(app, "/v1/me", token, new OblivionApi.JsonCb() {
            @Override
            public void ok(JSONObject json) {
                boolean[] verified = new boolean[1];
                JSONObject p = OblivionApi.openEnvelope(app, json, verified);
                if (p == null) {
                    if (cb != null) cb.done(null, "bad signature");
                    return;
                }
                Account a = new Account();
                a.discordId = p.optString("discord_id");
                a.username = p.optString("username");
                a.avatarUrl = p.optString("avatar_url");
                a.roles = OblivionRole.parse(toList(p.optJSONArray("roles")));
                a.entitlements = toList(p.optJSONArray("entitlements"));
                a.verified = verified[0];
                a.fetchedAt = System.currentTimeMillis();
                save(app, a);
                if (cb != null) cb.done(a, null);
            }

            @Override
            public void fail(String message) {
                if (cb != null) cb.done(null, message);
            }
        });
    }

    public static void logout(Context c) {
        prefs(c).edit().clear().apply();
    }
}
