package org.levimc.launcher.oblivion;

import android.content.Context;
import android.graphics.Bitmap;

import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.core.versions.VersionManager;

import java.util.ArrayList;
import java.util.List;

/** Builds the final skin (base + worn cosmetics the account may use) and installs it into the selected instance. */
public final class SkinApply {
    private SkinApply() {
    }

    public static List<Cosmetic> wornCosmetics(Context c) {
        OblivionSession.Account acc = OblivionSession.current(c);
        List<String> ids = SkinTools.appliedCosmetics(c);
        List<Cosmetic> worn = new ArrayList<>();
        for (Cosmetic cos : CosmeticCatalog.load(c)) {
            if (ids.contains(cos.id) && CosmeticCatalog.isUnlocked(cos, acc)) worn.add(cos);
        }
        return worn;
    }

    public static Bitmap composed(Context c) {
        Bitmap base = SkinTools.loadBaseSkin(c);
        if (base == null) return null;
        return SkinTools.withCosmetics(c, base, wornCosmetics(c));
    }

    public static GameVersion selectedVersion(Context c) {
        try {
            VersionManager vm = VersionManager.getIfInitialized();
            if (vm == null) vm = VersionManager.get(c);
            return vm.getSelectedVersion();
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the number of instance folders written, 0 if nothing could be written, -1 if no skin is set. */
    public static int apply(Context c) {
        Bitmap composed = composed(c);
        if (composed == null) return -1;
        Bitmap cape = SkinTools.capeOf(c, wornCosmetics(c));
        return SkinTools.writePack(c, selectedVersion(c), "Oblivion Skin", composed, SkinTools.isSlim(c), cape);
    }
}
