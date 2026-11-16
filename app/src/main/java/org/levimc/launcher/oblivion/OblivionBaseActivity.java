package org.levimc.launcher.oblivion;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Shared chrome for Oblivion screens: animated backdrop, header with back button and a scrolling column. */
public abstract class OblivionBaseActivity extends AppCompatActivity {
    private LinearLayout column;

    protected abstract @StringRes int titleRes();

    protected abstract void buildContent(LinearLayout column);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        OblivionTheme.ensureLoaded(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(new AmbientBackgroundView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        int pad = OUi.dp(this, 16);
        header.setPadding(pad, OUi.dp(this, 10), pad, OUi.dp(this, 6));
        TextView back = OUi.title(this, "‹");
        back.setTextSize(30);
        back.setPadding(0, 0, OUi.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        header.addView(back);
        TextView title = OUi.title(this, getString(titleRes()));
        title.setTextSize(20);
        header.addView(title);
        page.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(pad, OUi.dp(this, 6), pad, OUi.dp(this, 24));
        scroll.addView(column, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        buildContent(column);
    }

    protected void rebuild() {
        column.removeAllViews();
        buildContent(column);
    }

    protected LinearLayout column() {
        return column;
    }

    protected View divider() {
        return OUi.spacer(this, 6);
    }
}
