package org.levimc.launcher.oblivion;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.activities.MainActivity;

import java.io.File;

/**
 * Intro played when the client is opened. Length = OblivionTiming.INTRO_MS (5 s by default).
 * The main screen loads only after it finished. A DLC can supply intro_video instead of the built-in animation.
 */
public class OblivionIntroActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean finished;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        OblivionTheme.ensureLoaded(this);
        OblivionDlc.applyActive(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat ctl = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ctl.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
        ctl.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // not skippable: the main screen loads when the animation is done
            }
        });

        long total = OblivionTiming.INTRO_MS;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        File video = OblivionDlc.asset(this, "intro_video");
        if (video != null) {
            VideoView vv = new VideoView(this);
            vv.setVideoURI(Uri.fromFile(video));
            vv.setOnPreparedListener((MediaPlayer mp) -> {
                mp.setLooping(true);
                mp.setVolume(0f, 0f);
            });
            vv.setOnErrorListener((mp, w, e) -> true);
            root.addView(vv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            vv.start();
        } else {
            MovieBackgroundView bg = new MovieBackgroundView(this);
            bg.setAlpha(0f);
            root.addView(bg, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            bg.animate().alpha(1f).setDuration(Math.max(600, total / 3)).start();

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            ImageView ring = new ImageView(this);
            GradientDrawable rd = new GradientDrawable();
            rd.setShape(GradientDrawable.OVAL);
            rd.setColor(Color.TRANSPARENT);
            rd.setStroke(OUi.dp(this, 3), 0xFFFFFFFF);
            ring.setBackground(rd);
            FrameLayout stack = new FrameLayout(this);
            ImageView logo = new ImageView(this);
            logo.setImageResource(R.drawable.ic_oblivion_logo);
            logo.setBackground(OUi.rounded(0xFF000000, 0, 26, this));
            int pad = OUi.dp(this, 16);
            logo.setPadding(pad, pad, pad, pad);
            stack.addView(ring, new FrameLayout.LayoutParams(OUi.dp(this, 140), OUi.dp(this, 140), Gravity.CENTER));
            stack.addView(logo, new FrameLayout.LayoutParams(OUi.dp(this, 120), OUi.dp(this, 120), Gravity.CENTER));
            col.addView(stack, new LinearLayout.LayoutParams(OUi.dp(this, 190), OUi.dp(this, 190)));
            TextView name = new TextView(this);
            name.setText("Oblivion");
            name.setTextSize(40);
            name.setTypeface(OUi.heavy());
            name.setTextColor(0xFFFFFFFF);
            name.setShadowLayer(OUi.dp(this, 4), 0, OUi.dp(this, 2), 0xAA000000);
            col.addView(name);
            TextView sub = new TextView(this);
            sub.setText(R.string.oblivion_intro_tagline);
            sub.setTextSize(13);
            sub.setTypeface(OUi.font(this));
            sub.setTextColor(0xE6FFFFFF);
            col.addView(sub);
            root.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            logo.setAlpha(0f);
            logo.setScaleX(0.5f);
            logo.setScaleY(0.5f);
            name.setAlpha(0f);
            name.setTranslationY(OUi.dp(this, 18));
            sub.setAlpha(0f);
            ring.setAlpha(0f);
            ring.setScaleX(0.6f);
            ring.setScaleY(0.6f);
            long t1 = Math.max(500, total * 22 / 100);
            logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(total / 12).setDuration(t1)
                    .setInterpolator(new OvershootInterpolator(1.4f)).start();
            ring.animate().alpha(0.9f).scaleX(1.25f).scaleY(1.25f).setStartDelay(total / 8).setDuration(total * 45 / 100)
                    .withEndAction(() -> ring.animate().alpha(0f).setDuration(total / 5).start()).start();
            name.animate().alpha(1f).translationY(0f).setStartDelay(total * 30 / 100).setDuration(total * 20 / 100).start();
            sub.animate().alpha(1f).setStartDelay(total * 48 / 100).setDuration(total * 20 / 100).start();
        }
        setContentView(root);

        handler.postDelayed(() -> root.animate().alpha(0f).setDuration(350).withEndAction(this::proceed).start(),
                Math.max(500, total - 350));
    }

    private void proceed() {
        if (finished) return;
        finished = true;
        Intent next = OnboardingActivity.isNeeded(this)
                ? new Intent(this, OnboardingActivity.class)
                : new Intent(this, MainActivity.class);
        startActivity(next);
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
