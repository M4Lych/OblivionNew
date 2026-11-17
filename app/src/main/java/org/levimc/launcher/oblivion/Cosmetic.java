package org.levimc.launcher.oblivion;

import java.util.ArrayList;
import java.util.List;

public final class Cosmetic {
    public enum Type {OVERLAY, CAPE}

    public String id;
    public String name;
    public String description;
    public Type type = Type.OVERLAY;
    /** Bundled asset path under assets/oblivion/ (e.g. cosmetics/mask.png). */
    public String asset;
    /** Vault entry name when the image was delivered by the server (claim / package). */
    public String vaultName;
    /** Roles allowed to wear it (empty = everyone). */
    public List<OblivionRole> roles = new ArrayList<>();
    /** Needs a claim code / server entitlement with this id. */
    public boolean claimOnly;
    public boolean membershipOnly;
    /** Absolute file path when the image comes from an installed OblivionDLC. */
    public String file;
    public int price;
}
