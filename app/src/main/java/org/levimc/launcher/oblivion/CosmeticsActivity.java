package org.levimc.launcher.oblivion;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.levimc.launcher.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Cosmetics: a live 3D preview of the player (skin, worn cosmetics, cape, DLC model) and the catalogue
 * split into Equipped / Available / Locked. Locked items show what unlocks them (role, Oblivion+, claim code).
 */
public class CosmeticsActivity extends OblivionBaseActivity {
    private static int tab = 0;
    private TextView status;

    @Override
    protected int titleRes() {
        return R.string.oblivion_cosmetics;
    }

    @Override
    protected void buildContent(LinearLayout col) {
        OblivionSession.Account acc = OblivionSession.current(this);

        LinearLayout previewCard = OUi.card(this);
        PlayerModelView preview = new PlayerModelView(this);
        Bitmap skin = SkinApply.composed(this);
        if (skin == null) skin = SkinApply.defaultSkin(this);
        Bitmap cape = SkinTools.capeOf(this, SkinApply.wornCosmetics(this));
        PlayerModel custom = OblivionDlc.model(this);
        preview.setContent(custom != null ? custom : PlayerModel.humanoid(SkinTools.isSlim(this), cape != null), skin, cape);
        previewCard.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, OUi.dp(this, 280)));
        col.addView(previewCard);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        int[] labels = {R.string.oblivion_tab_equipped, R.string.oblivion_tab_available, R.string.oblivion_tab_locked};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = OUi.button(this, getString(labels[i]), tab == i, v -> {
                tab = idx;
                rebuild();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(idx == 0 ? 0 : OUi.dp(this, 6), 0, 0, OUi.dp(this, 10));
            t.setLayoutParams(lp);
            tabs.addView(t);
        }
        col.addView(tabs);

        List<String> worn = SkinTools.appliedCosmetics(this);
        int shown = 0;
        for (Cosmetic cos : CosmeticCatalog.load(this)) {
            boolean unlocked = CosmeticCatalog.isUnlocked(cos, acc);
            boolean on = worn.contains(cos.id) && unlocked;
            boolean inTab = tab == 0 ? on : tab == 1 ? (unlocked && !on) : !unlocked;
            if (!inTab) continue;
            shown++;
            LinearLayout card = OUi.card(this);
            card.addView(OUi.title(this, cos.name));
            card.addView(OUi.body(this, cos.description));
            if (!unlocked) {
                TextView lock = OUi.body(this, lockReason(cos));
                lock.setTextColor(OblivionTheme.gold);
                card.addView(lock);
                if (cos.price > 0) {
                    TextView buy = OUi.button(this, getString(R.string.oblivion_buy, cos.price), false, null);
                    buy.setEnabled(false);
                    buy.setAlpha(0.45f);
                    card.addView(buy);
                    card.addView(OUi.body(this, getString(R.string.oblivion_buy_unavailable)));
                }
            } else {
                card.addView(OUi.button(this,
                        getString(on ? R.string.oblivion_cosmetic_remove : R.string.oblivion_cosmetic_wear),
                        !on, v -> toggle(cos)));
            }
            col.addView(card);
        }
        if (shown == 0) col.addView(OUi.body(this, getString(R.string.oblivion_tab_empty)));

        col.addView(OUi.button(this, getString(R.string.oblivion_skin_apply), true, v -> {
            int n = SkinApply.apply(this);
            if (n > 0) Achievements.increment(this, Achievements.SKINS);
            status.setText(n < 0 ? R.string.oblivion_skin_none
                    : n == 0 ? R.string.oblivion_skin_apply_failed : R.string.oblivion_skin_applied);
        }));
        col.addView(OUi.button(this, getString(R.string.oblivion_skin_changer), false,
                v -> startActivity(new Intent(this, SkinChangerActivity.class))));
        status = OUi.body(this, "");
        status.setPadding(0, OUi.dp(this, 10), 0, 0);
        col.addView(status);
    }

    private String lockReason(Cosmetic cos) {
        if (cos.claimOnly) return getString(R.string.oblivion_lock_claim);
        if (cos.membershipOnly) return getString(R.string.oblivion_lock_membership);
        if (!cos.roles.isEmpty()) return getString(R.string.oblivion_lock_role, cos.roles.get(0).label(this));
        return "";
    }

    private void toggle(Cosmetic cos) {
        List<String> ids = new ArrayList<>(SkinTools.appliedCosmetics(this));
        if (ids.contains(cos.id)) {
            ids.remove(cos.id);
        } else {
            if (cos.type == Cosmetic.Type.CAPE) {
                for (Cosmetic other : CosmeticCatalog.load(this)) {
                    if (other.type == Cosmetic.Type.CAPE) ids.remove(other.id);
                }
            }
            ids.add(cos.id);
            Achievements.increment(this, Achievements.COSMETICS);
        }
        SkinTools.setAppliedCosmetics(this, ids);
        rebuild();
    }
}
