package org.levimc.launcher.oblivion;

import android.graphics.Bitmap;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.levimc.launcher.R;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsActivity extends OblivionBaseActivity {
    private SkinSpriteView preview;
    private TextView status;

    @Override
    protected int titleRes() {
        return R.string.oblivion_cosmetics;
    }

    @Override
    protected void buildContent(LinearLayout col) {
        OblivionSession.Account acc = OblivionSession.current(this);
        LinearLayout previewCard = OUi.card(this);
        preview = new SkinSpriteView(this);
        previewCard.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, OUi.dp(this, 240)));
        col.addView(previewCard);

        List<String> worn = SkinTools.appliedCosmetics(this);
        for (Cosmetic cos : CosmeticCatalog.load(this)) {
            boolean unlocked = CosmeticCatalog.isUnlocked(cos, acc);
            boolean on = worn.contains(cos.id) && unlocked;
            LinearLayout card = OUi.card(this);
            card.addView(OUi.title(this, cos.name));
            card.addView(OUi.body(this, cos.description));
            if (!unlocked) {
                TextView lock = OUi.body(this, lockReason(cos));
                lock.setTextColor(OblivionTheme.gold);
                card.addView(lock);
            }
            TextView btn = OUi.button(this,
                    getString(on ? R.string.oblivion_cosmetic_remove : R.string.oblivion_cosmetic_wear),
                    !on, v -> toggle(cos));
            btn.setEnabled(unlocked);
            btn.setAlpha(unlocked ? 1f : 0.4f);
            card.addView(btn);
            col.addView(card);
        }
        col.addView(OUi.button(this, getString(R.string.oblivion_skin_apply), true, v -> {
            int n = SkinApply.apply(this);
            status.setText(n < 0 ? R.string.oblivion_skin_none
                    : n == 0 ? R.string.oblivion_skin_apply_failed : R.string.oblivion_skin_applied);
        }));
        status = OUi.body(this, "");
        status.setPadding(0, OUi.dp(this, 10), 0, 0);
        col.addView(status);
        refreshPreview();
    }

    private String lockReason(Cosmetic cos) {
        if (cos.claimOnly) return getString(R.string.oblivion_lock_claim);
        if (cos.membershipOnly) return getString(R.string.oblivion_lock_membership);
        if (!cos.roles.isEmpty()) {
            return getString(R.string.oblivion_lock_role, cos.roles.get(0).label(this));
        }
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
        }
        SkinTools.setAppliedCosmetics(this, ids);
        rebuild();
    }

    private void refreshPreview() {
        Bitmap composed = SkinApply.composed(this);
        preview.setSkin(composed, SkinTools.isSlim(this));
        if (composed == null) status.setText(R.string.oblivion_skin_none);
    }
}
