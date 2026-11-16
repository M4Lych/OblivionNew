package org.levimc.launcher.oblivion;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.levimc.launcher.R;

import java.io.InputStream;

public class SkinChangerActivity extends OblivionBaseActivity {
    private SkinSpriteView preview;
    private TextView slimButton;
    private TextView status;

    private final ActivityResultLauncher<String> picker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    Bitmap b = BitmapFactory.decodeStream(in);
                    Bitmap n = b == null ? null : SkinTools.normalize(b);
                    if (n == null) {
                        Toast.makeText(this, R.string.oblivion_skin_bad_size, Toast.LENGTH_LONG).show();
                        return;
                    }
                    SkinTools.saveBaseSkin(this, n);
                    refreshPreview();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.oblivion_skin_bad_size, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected int titleRes() {
        return R.string.oblivion_skin_changer;
    }

    @Override
    protected void buildContent(LinearLayout col) {
        LinearLayout card = OUi.card(this);
        preview = new SkinSpriteView(this);
        card.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, OUi.dp(this, 280)));
        card.addView(OUi.body(this, getString(R.string.oblivion_skin_hint)));
        col.addView(card);

        col.addView(OUi.button(this, getString(R.string.oblivion_skin_pick), true, v -> picker.launch("image/png")));
        slimButton = OUi.button(this, "", false, v -> {
            SkinTools.setSlim(this, !SkinTools.isSlim(this));
            refreshPreview();
        });
        col.addView(slimButton);
        col.addView(OUi.button(this, getString(R.string.oblivion_skin_apply), true, v -> applyNow()));
        col.addView(OUi.button(this, getString(R.string.oblivion_cosmetics),
                false, v -> startActivity(new android.content.Intent(this, CosmeticsActivity.class))));
        status = OUi.body(this, "");
        status.setPadding(0, OUi.dp(this, 10), 0, 0);
        col.addView(status);
        refreshPreview();
    }

    private void refreshPreview() {
        boolean slim = SkinTools.isSlim(this);
        slimButton.setText(slim ? R.string.oblivion_skin_model_slim : R.string.oblivion_skin_model_classic);
        Bitmap composed = SkinApply.composed(this);
        preview.setSkin(composed, slim);
        status.setText(composed == null ? getString(R.string.oblivion_skin_none) : "");
    }

    private void applyNow() {
        int n = SkinApply.apply(this);
        if (n < 0) status.setText(R.string.oblivion_skin_none);
        else if (n == 0) status.setText(R.string.oblivion_skin_apply_failed);
        else status.setText(R.string.oblivion_skin_applied);
    }
}
