package org.levimc.launcher.oblivion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.levimc.launcher.R;
import org.levimc.launcher.core.auth.MsftAccountStore;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.ui.activities.SettingsActivity;

import java.util.List;

/**
 * Dungeons-style start screen: the player (gamertag + current skin) in the middle, START GAME bottom-left,
 * quick buttons bottom-right. It only forwards taps to the existing launcher controls, so the launch
 * pipeline itself is unchanged.
 */
public class DungeonHomeView extends FrameLayout {
    private final Runnable startGame;
    private final Runnable pickVersion;
    private final Runnable exitApp;
    private TextView nameView;
    private TextView roleView;
    private TextView versionView;
    private SkinSpriteView sprite;

    public static void attach(Activity activity, Runnable startGame, Runnable pickVersion, Runnable exitApp) {
        if (!OblivionSettings.get(activity, OblivionSettings.DUNGEON_HOME)) return;
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        DungeonHomeView v = new DungeonHomeView(activity, startGame, pickVersion, exitApp);
        content.addView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private DungeonHomeView(Context c, Runnable startGame, Runnable pickVersion, Runnable exitApp) {
        super(c);
        this.startGame = startGame;
        this.pickVersion = pickVersion;
        this.exitApp = exitApp;
        OblivionTheme.ensureLoaded(c);
        setClickable(true);
        build(c);
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void build(Context c) {
        AmbientBackgroundView bg = new AmbientBackgroundView(c);
        bg.setAnimated(OblivionSettings.get(c, OblivionSettings.AMBIENT_ANIM));
        addView(bg, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // centre: skin sprite + name plate
        LinearLayout centre = new LinearLayout(c);
        centre.setOrientation(LinearLayout.VERTICAL);
        centre.setGravity(Gravity.CENTER_HORIZONTAL);
        sprite = new SkinSpriteView(c);
        centre.addView(sprite, new LinearLayout.LayoutParams(OUi.dp(c, 190), 0, 1f));
        LinearLayout plate = new LinearLayout(c);
        plate.setOrientation(LinearLayout.VERTICAL);
        plate.setGravity(Gravity.CENTER_HORIZONTAL);
        plate.setBackground(OUi.rounded(0xCC120A22, OblivionTheme.stroke, 6, c));
        plate.setPadding(OUi.dp(c, 18), OUi.dp(c, 8), OUi.dp(c, 18), OUi.dp(c, 10));
        nameView = OUi.title(c, "");
        nameView.setTextSize(17);
        plate.addView(nameView);
        roleView = OUi.body(c, "");
        roleView.setTextSize(12);
        plate.addView(roleView);
        TextView switchSkin = OUi.button(c, c.getString(R.string.oblivion_switch_skin), false,
                v -> getContext().startActivity(new Intent(getContext(), SkinChangerActivity.class)));
        switchSkin.setAllCaps(true);
        plate.addView(switchSkin);
        centre.addView(plate, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LayoutParams cp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
        cp.setMargins(0, OUi.dp(c, 12), 0, OUi.dp(c, 12));
        addView(centre, cp);

        // bottom-left: version + START GAME
        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setBackground(OUi.rounded(0xAA120A22, OblivionTheme.stroke, 6, c));
        left.setPadding(OUi.dp(c, 10), OUi.dp(c, 8), OUi.dp(c, 10), OUi.dp(c, 10));
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        versionView = OUi.body(c, "");
        versionView.setTextColor(OblivionTheme.text);
        versionView.setTypeface(OUi.font(c), Typeface.BOLD);
        row.addView(versionView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView change = OUi.body(c, c.getString(R.string.oblivion_change).toUpperCase());
        change.setTextColor(OblivionTheme.gold);
        change.setTypeface(OUi.font(c), Typeface.BOLD);
        change.setOnClickListener(v -> pickVersion.run());
        row.addView(change);
        left.addView(row);
        TextView start = OUi.button(c, c.getString(R.string.oblivion_start_game), true, v -> {
            if (OblivionSettings.get(getContext(), OblivionSettings.AUTO_APPLY_SKIN)) {
                try {
                    SkinApply.apply(getContext());
                } catch (Exception ignored) {
                }
            }
            startGame.run();
        });
        start.setAllCaps(true);
        start.setTextSize(18);
        start.setBackground(OUi.rounded(0xFF3F8F4F, 0xFF7BD08A, 4, c));
        start.setMinWidth(OUi.dp(c, 220));
        left.addView(start);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        lp.setMargins(OUi.dp(c, 16), 0, 0, OUi.dp(c, 16));
        addView(left, lp);

        // bottom-right quick buttons
        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.HORIZONTAL);
        right.addView(small(c, R.string.oblivion_hub, v -> getContext().startActivity(new Intent(getContext(), OblivionHubActivity.class))));
        right.addView(small(c, R.string.oblivion_dashboard, v -> setVisibility(GONE)));
        right.addView(small(c, R.string.settings, v -> getContext().startActivity(new Intent(getContext(), SettingsActivity.class))));
        right.addView(small(c, R.string.exit, v -> exitApp.run()));
        LayoutParams rp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        rp.setMargins(0, 0, OUi.dp(c, 16), OUi.dp(c, 16));
        addView(right, rp);
        refresh();
    }

    private TextView small(Context c, int label, View.OnClickListener l) {
        TextView t = OUi.button(c, c.getString(label), false, l);
        t.setTextSize(12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(OUi.dp(c, 6), 0, 0, 0);
        t.setLayoutParams(lp);
        return t;
    }

    public void refresh() {
        Context c = getContext();
        String name = c.getString(R.string.oblivion_player_default);
        try {
            List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(c);
            for (MsftAccountStore.MsftAccount a : list) {
                if (a.active) {
                    if (a.xboxGamertag != null && !a.xboxGamertag.isEmpty()) name = a.xboxGamertag;
                    else if (a.minecraftUsername != null && !a.minecraftUsername.isEmpty()) name = a.minecraftUsername;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        nameView.setText(name);
        OblivionSession.Account acc = OblivionSession.current(c);
        OblivionRole top = acc == null ? null : acc.highestRole();
        if (top != null && OblivionSettings.get(c, OblivionSettings.SHOW_BADGE)) {
            roleView.setText(top.label(c) + (acc.hasMembership() ? "  •  Oblivion+" : ""));
            roleView.setTextColor(top.color);
            roleView.setVisibility(VISIBLE);
        } else {
            roleView.setVisibility(GONE);
        }
        GameVersion v = SkinApply.selectedVersion(c);
        String label = v == null ? c.getString(R.string.oblivion_no_version)
                : (v.displayName != null && !v.displayName.isEmpty() ? v.displayName : v.directoryName);
        versionView.setText(label);
        Bitmap skin = SkinApply.composed(c);
        sprite.setSkin(skin != null ? skin : defaultSkin(), SkinTools.isSlim(c));
    }

    private Bitmap defaultSkin() {
        try (java.io.InputStream in = getContext().getAssets().open("oblivion/default_skin.png")) {
            return SkinTools.normalize(android.graphics.BitmapFactory.decodeStream(in));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) refresh();
    }
}
