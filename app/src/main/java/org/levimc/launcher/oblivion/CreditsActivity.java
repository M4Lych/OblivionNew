package org.levimc.launcher.oblivion;

import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.activities.AboutActivity;

/** Oblivion Credits: the Oblivion modder first, then the projects Oblivion is built on. */
public class CreditsActivity extends OblivionBaseActivity {
    @Override
    protected int titleRes() {
        return R.string.oblivion_credits_title;
    }

    @Override
    protected void buildContent(LinearLayout col) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_oblivion_logo);
        logo.setBackground(OUi.rounded(0xFF000000, 0, 18, this));
        int p = OUi.dp(this, 12);
        logo.setPadding(p, p, p, p);
        head.addView(logo, new LinearLayout.LayoutParams(OUi.dp(this, 84), OUi.dp(this, 84)));
        TextView name = OUi.title(this, "Oblivion");
        name.setTextSize(30);
        name.setTypeface(OUi.heavy());
        name.setPadding(0, OUi.dp(this, 8), 0, OUi.dp(this, 16));
        head.addView(name);
        col.addView(head);

        LinearLayout modder = OUi.card(this);
        modder.addView(OUi.title(this, getString(R.string.oblivion_dev_name)));
        modder.addView(OUi.body(this, getString(R.string.oblivion_credits_modder_role)));
        link(modder, R.string.oblivion_dev_github, "GitHub");
        link(modder, R.string.oblivion_discord_invite, "Discord");
        link(modder, R.string.oblivion_social_youtube, "YouTube");
        link(modder, R.string.oblivion_social_instagram, "Instagram");
        link(modder, R.string.oblivion_social_tiktok, "TikTok");
        col.addView(modder);

        LinearLayout base = OUi.card(this);
        base.addView(OUi.title(this, getString(R.string.oblivion_credits_built_on)));
        base.addView(OUi.body(this, getString(R.string.oblivion_credits_built_on_text)));
        base.addView(OUi.button(this, "LeviLauncher — LiteLDev", false,
                v -> open("https://github.com/LiteLDev/LeviLaunchroid")));
        base.addView(OUi.button(this, "LeviDroid-Modder — sang765", false,
                v -> open("https://github.com/sang765/LeviDroid-Modder")));
        base.addView(OUi.button(this, getString(R.string.oblivion_credits_contributors), true,
                v -> startActivity(new Intent(this, AboutActivity.class))));
        col.addView(base);

        TextView foot = OUi.body(this, getString(R.string.oblivion_credits_license));
        foot.setGravity(android.view.Gravity.CENTER);
        col.addView(foot);
    }

    private void link(LinearLayout card, int urlRes, String label) {
        String url = getString(urlRes).trim();
        if (!url.startsWith("http")) return;
        card.addView(OUi.button(this, label, false, v -> open(url)));
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }
}
