package org.levimc.launcher.oblivion;

import android.content.Intent;
import android.widget.LinearLayout;

import androidx.appcompat.widget.SwitchCompat;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.activities.InstancesActivity;
import org.levimc.launcher.ui.activities.SettingsActivity;

/** Settings hub: Oblivion client options, theme, and shortcuts to the launcher/instance settings. */
public class SettingsHubActivity extends OblivionBaseActivity {
    @Override
    protected int titleRes() {
        return R.string.settings;
    }

    @Override
    protected void buildContent(LinearLayout col) {
        col.addView(OUi.tile(this, getString(R.string.oblivion_set_launcher),
                getString(R.string.oblivion_set_launcher_sub),
                v -> startActivity(new Intent(this, SettingsActivity.class))));
        col.addView(OUi.tile(this, getString(R.string.oblivion_set_instances),
                getString(R.string.oblivion_set_instances_sub),
                v -> startActivity(new Intent(this, InstancesActivity.class))));

        LinearLayout theme = OUi.card(this);
        theme.addView(OUi.title(this, getString(R.string.oblivion_theme)));
        String cur = OblivionTheme.preset(this);
        theme.addView(OUi.button(this, getString(R.string.oblivion_theme_movie)
                + (OblivionTheme.PRESET_MOVIE.equals(cur) ? "  ✓" : ""), OblivionTheme.PRESET_MOVIE.equals(cur),
                v -> {
                    OblivionTheme.setPreset(this, OblivionTheme.PRESET_MOVIE);
                    rebuild();
                }));
        theme.addView(OUi.button(this, getString(R.string.oblivion_theme_mujica)
                + (OblivionTheme.PRESET_MUJICA.equals(cur) ? "  ✓" : ""), OblivionTheme.PRESET_MUJICA.equals(cur),
                v -> {
                    OblivionTheme.setPreset(this, OblivionTheme.PRESET_MUJICA);
                    rebuild();
                }));
        col.addView(theme);

        LinearLayout client = OUi.card(this);
        client.addView(OUi.title(this, getString(R.string.oblivion_client_settings)));
        addSwitch(client, R.string.oblivion_set_dungeon_home, OblivionSettings.DUNGEON_HOME);
        addSwitch(client, R.string.oblivion_set_ambient, OblivionSettings.AMBIENT_ANIM);
        addSwitch(client, R.string.oblivion_set_loading_video, OblivionSettings.LOADING_VIDEO);
        addSwitch(client, R.string.oblivion_set_auto_skin, OblivionSettings.AUTO_APPLY_SKIN);
        addSwitch(client, R.string.oblivion_set_badge, OblivionSettings.SHOW_BADGE);
        addSwitch(client, R.string.oblivion_set_watermark, OblivionSettings.INGAME_WATERMARK);
        client.addView(OUi.body(this, getString(R.string.oblivion_set_restart_hint)));
        col.addView(client);
    }

    private void addSwitch(LinearLayout parent, int label, String key) {
        SwitchCompat sw = new SwitchCompat(this);
        sw.setText(label);
        sw.setTextColor(OblivionTheme.text);
        sw.setTypeface(OUi.font(this));
        sw.setChecked(OblivionSettings.get(this, key));
        sw.setPadding(0, OUi.dp(this, 8), 0, OUi.dp(this, 8));
        sw.setOnCheckedChangeListener((b, on) -> OblivionSettings.set(this, key, on));
        parent.addView(sw);
    }
}
