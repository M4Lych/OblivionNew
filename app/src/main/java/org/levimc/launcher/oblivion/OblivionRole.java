package org.levimc.launcher.oblivion;

import android.content.Context;

import org.levimc.launcher.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum OblivionRole {
    OBLIVION("oblivion", 100, R.string.oblivion_role_oblivion, 0xFFD4AF37),
    ADMIN("admin", 90, R.string.oblivion_role_admin, 0xFFE53935),
    STAFF("staff", 80, R.string.oblivion_role_staff, 0xFFFB8C00),
    CREATOR("creator", 60, R.string.oblivion_role_creator, 0xFFEC407A),
    PARTNER("partner", 55, R.string.oblivion_role_partner, 0xFF26C6DA),
    OBLIVION_PLUS("oblivion_plus", 40, R.string.oblivion_role_plus, 0xFFB388FF),
    MEMBER("member", 20, R.string.oblivion_role_member, 0xFF90A4AE),
    NEWCOMER("newcomer", 10, R.string.oblivion_role_newcomer, 0xFF78909C);

    public final String key;
    public final int rank;
    public final int labelRes;
    public final int color;

    OblivionRole(String key, int rank, int labelRes, int color) {
        this.key = key;
        this.rank = rank;
        this.labelRes = labelRes;
        this.color = color;
    }

    public String label(Context c) {
        return c.getString(labelRes);
    }

    public static OblivionRole fromKey(String key) {
        if (key == null) return null;
        String k = key.trim().toLowerCase(Locale.ROOT).replace('+', '_').replace('-', '_').replace(' ', '_');
        if (k.equals("oblivion_") || k.equals("oblivionplus")) k = "oblivion_plus";
        if (k.equals("new_comer") || k.equals("new_commer") || k.equals("newcommer")) k = "newcomer";
        for (OblivionRole r : values()) if (r.key.equals(k)) return r;
        return null;
    }

    public static List<OblivionRole> parse(List<String> keys) {
        List<OblivionRole> out = new ArrayList<>();
        if (keys == null) return out;
        for (String s : keys) {
            OblivionRole r = fromKey(s);
            if (r != null && !out.contains(r)) out.add(r);
        }
        return out;
    }

    public static OblivionRole highest(List<OblivionRole> roles) {
        OblivionRole best = null;
        if (roles == null) return null;
        for (OblivionRole r : roles) if (best == null || r.rank > best.rank) best = r;
        return best;
    }

    /** Membership perks: Oblivion+ and every staff-level role. */
    public static boolean hasMembership(List<OblivionRole> roles) {
        if (roles == null) return false;
        for (OblivionRole r : roles) if (r == OBLIVION_PLUS || r.rank >= STAFF.rank) return true;
        return false;
    }

    public static boolean isStaff(List<OblivionRole> roles) {
        if (roles == null) return false;
        for (OblivionRole r : roles) if (r.rank >= STAFF.rank) return true;
        return false;
    }
}
