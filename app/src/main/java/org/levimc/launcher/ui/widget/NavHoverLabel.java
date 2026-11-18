package org.levimc.launcher.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.levimc.launcher.R;

/** Shows the name of a rail icon beside it while it is hovered (mouse, stylus, trackpad) or long-pressed. */
public final class NavHoverLabel {
    private static PopupWindow current;
    private static final Runnable NO_OP = () -> { };

    private NavHoverLabel() {
    }

    public static void attach(View anchor) {
        if (anchor == null) return;
        anchor.setOnHoverListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_HOVER_ENTER) show(v);
            else if (action == MotionEvent.ACTION_HOVER_EXIT) dismiss();
            return false;
        });
        anchor.setOnLongClickListener(v -> {
            show(v);
            v.postDelayed(NavHoverLabel::dismiss, 1600);
            return true;
        });
    }

    public static void show(View anchor) {
        CharSequence name = anchor.getContentDescription();
        if (name == null || name.length() == 0 || !anchor.isAttachedToWindow()) return;
        dismiss();
        Context ctx = anchor.getContext();
        float d = ctx.getResources().getDisplayMetrics().density;
        TextView label = new TextView(ctx);
        label.setText(name);
        label.setAllCaps(true);
        label.setLetterSpacing(0.14f);
        label.setTextSize(11.5f);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(0xFFEDE6EA);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setSingleLine(true);
        label.setBackgroundResource(R.drawable.bg_nav_hover_label);
        label.setPadding(Math.round(14 * d), 0, Math.round(14 * d), 0);
        int height = Math.round(34 * d);
        label.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        PopupWindow popup = new PopupWindow(label, label.getMeasuredWidth(), height, false);
        popup.setTouchable(false);
        popup.setClippingEnabled(false);
        popup.setElevation(6 * d);
        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        int x = loc[0] + anchor.getWidth() + Math.round(6 * d);
        int y = loc[1] + (anchor.getHeight() - height) / 2;
        try {
            popup.showAtLocation(anchor.getRootView(), Gravity.NO_GRAVITY, x, y);
            current = popup;
        } catch (RuntimeException ignored) {
            current = null;
        }
    }

    public static void dismiss() {
        PopupWindow popup = current;
        current = null;
        if (popup != null) {
            try {
                popup.dismiss();
            } catch (RuntimeException ignored) {
                NO_OP.run();
            }
        }
    }
}
