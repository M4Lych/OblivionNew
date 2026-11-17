package org.levimc.launcher.oblivion;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.levimc.launcher.R;

/** Small programmatic view helpers so Oblivion screens share one look without extra XML. */
public final class OUi {
    private OUi() {
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static Typeface font(Context c) {
        try {
            Typeface t = ResourcesCompat.getFont(c, R.font.misans);
            return t != null ? t : Typeface.DEFAULT;
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    /** Chunky display face used for the movie-style headings and buttons. */
    public static Typeface heavy() {
        return Typeface.create("sans-serif-black", Typeface.NORMAL);
    }

    public static TextView glassButton(Context c, CharSequence t, int textSp, View.OnClickListener l) {
        TextView b = new TextView(c);
        b.setText(t);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(textSp);
        b.setTypeface(heavy());
        b.setTextColor(OblivionTheme.text);
        b.setShadowLayer(dp(c, 2), 0, dp(c, 1), 0x99000000);
        GradientDrawable base = rounded(OblivionTheme.glass, OblivionTheme.glassStroke, 7, c);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), base, null));
        b.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        b.setClickable(true);
        pressable(b);
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    /** Small bounce on press so every button feels alive. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    public static void pressable(View v) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    public static GradientDrawable rounded(int fill, int stroke, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) d.setStroke(dp(c, 1), stroke);
        return d;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rounded(OblivionTheme.surface, OblivionTheme.stroke, 14, c));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, 12));
        l.setLayoutParams(lp);
        return l;
    }

    public static TextView title(Context c, CharSequence t) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextColor(OblivionTheme.text);
        v.setTextSize(18);
        v.setTypeface(font(c), Typeface.BOLD);
        return v;
    }

    public static TextView body(Context c, CharSequence t) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextColor(OblivionTheme.textDim);
        v.setTextSize(14);
        v.setTypeface(font(c));
        v.setLineSpacing(0, 1.1f);
        return v;
    }

    public static TextView button(Context c, CharSequence t, boolean primary, View.OnClickListener l) {
        TextView b = new TextView(c);
        b.setText(t);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(14);
        b.setTypeface(font(c), Typeface.BOLD);
        b.setTextColor(primary ? 0xFFFFFFFF : OblivionTheme.text);
        int fill = primary ? OblivionTheme.primary : OblivionTheme.surfaceAlt;
        GradientDrawable base = rounded(fill, primary ? 0 : OblivionTheme.stroke, 10, c);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), base, null));
        int ph = dp(c, 16), pv = dp(c, 12);
        b.setPadding(ph, pv, ph, pv);
        b.setClickable(true);
        b.setFocusable(true);
        if (l != null) b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 8), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    /** Full-width glass row: title, optional subtitle, arrow. */
    public static LinearLayout tile(Context c, CharSequence title, CharSequence subtitle, View.OnClickListener l) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),
                rounded(OblivionTheme.glass, OblivionTheme.glassStroke, 8, c), null));
        row.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextSize(18);
        t.setTypeface(heavy());
        t.setTextColor(OblivionTheme.text);
        texts.addView(t);
        if (subtitle != null && subtitle.length() > 0) {
            TextView st = new TextView(c);
            st.setText(subtitle);
            st.setTextSize(13);
            st.setTypeface(font(c));
            st.setTextColor(OblivionTheme.textDim);
            texts.addView(st);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView arrow = new TextView(c);
        arrow.setText("›");
        arrow.setTextSize(26);
        arrow.setTextColor(OblivionTheme.textDim);
        row.addView(arrow);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, 10));
        row.setLayoutParams(lp);
        row.setClickable(true);
        pressable(row);
        if (l != null) row.setOnClickListener(l);
        return row;
    }

    public static View spacer(Context c, int dpHeight) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dpHeight)));
        return v;
    }
}
