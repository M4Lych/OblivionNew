package org.levimc.launcher.oblivion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Bundled cosmetics (assets/oblivion/cosmetics.json) plus a role / entitlement check. */
public final class CosmeticCatalog {
    private CosmeticCatalog() {
    }

    public static List<Cosmetic> load(Context c) {
        List<Cosmetic> out = new ArrayList<>();
        try (InputStream in = c.getAssets().open("oblivion/cosmetics.json")) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            JSONArray arr = new JSONObject(bos.toString("UTF-8")).getJSONArray("cosmetics");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Cosmetic cos = new Cosmetic();
                cos.id = o.getString("id");
                cos.name = o.optString("name", cos.id);
                cos.description = o.optString("description", "");
                cos.type = "cape".equalsIgnoreCase(o.optString("type")) ? Cosmetic.Type.CAPE : Cosmetic.Type.OVERLAY;
                cos.asset = o.optString("asset", null);
                cos.claimOnly = o.optBoolean("claim", false);
                cos.membershipOnly = o.optBoolean("membership", false);
                JSONArray roles = o.optJSONArray("roles");
                if (roles != null) {
                    List<String> keys = new ArrayList<>();
                    for (int r = 0; r < roles.length(); r++) keys.add(roles.optString(r));
                    cos.roles = OblivionRole.parse(keys);
                }
                out.add(cos);
            }
        } catch (Exception ignored) {
        }
        out.addAll(OblivionDlc.cosmetics(c));
        return out;
    }

    /** True if the signed-in account may wear this cosmetic. */
    public static boolean isUnlocked(Cosmetic cos, OblivionSession.Account acc) {
        boolean open = cos.roles.isEmpty() && !cos.claimOnly && !cos.membershipOnly;
        if (open) return true;
        if (acc == null || !acc.fresh()) return false;
        if (acc.owns(cos.id)) return true;
        if (cos.membershipOnly && acc.hasMembership()) return true;
        for (OblivionRole need : cos.roles) {
            for (OblivionRole have : acc.roles) if (have.rank >= need.rank) return true;
        }
        return false;
    }

    public static Bitmap loadBitmap(Context c, Cosmetic cos) {
        try {
            if (cos.file != null) {
                return BitmapFactory.decodeFile(cos.file);
            }
            if (cos.vaultName != null) {
                byte[] data = SecureVault.get(c, cos.vaultName);
                if (data != null) return BitmapFactory.decodeByteArray(data, 0, data.length);
            }
            if (cos.asset != null) {
                try (InputStream in = c.getAssets().open("oblivion/" + cos.asset)) {
                    return BitmapFactory.decodeStream(in);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
