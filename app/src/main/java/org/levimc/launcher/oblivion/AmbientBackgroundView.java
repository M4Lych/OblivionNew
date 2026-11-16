package org.levimc.launcher.oblivion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/** Dark gothic backdrop with slowly rising embers. Drawn in code so it needs no image assets. */
public class AmbientBackgroundView extends View {
    private static final int COUNT = 46;
    private final Paint bgPaint = new Paint();
    private final Paint glowPaint = new Paint();
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] x = new float[COUNT];
    private final float[] y = new float[COUNT];
    private final float[] r = new float[COUNT];
    private final float[] v = new float[COUNT];
    private final int[] col = new int[COUNT];
    private final Random rnd = new Random(7);
    private boolean animate = true;
    private long last;

    public AmbientBackgroundView(Context c) {
        this(c, null);
    }

    public AmbientBackgroundView(Context c, AttributeSet a) {
        super(c, a);
        OblivionTheme.ensureLoaded(c);
    }

    public void setAnimated(boolean on) {
        animate = on;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{OblivionTheme.bg, 0xFF120A22, 0xFF2A0A24}, new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP));
        glowPaint.setShader(new RadialGradient(w * 0.5f, h * 0.95f, Math.max(w, h) * 0.6f,
                0x66C2185B, 0x00000000, Shader.TileMode.CLAMP));
        int[] palette = {OblivionTheme.gold, OblivionTheme.accent, OblivionTheme.primary};
        for (int i = 0; i < COUNT; i++) {
            x[i] = rnd.nextFloat() * w;
            y[i] = rnd.nextFloat() * h;
            r[i] = 1.2f + rnd.nextFloat() * 2.6f;
            v[i] = 0.015f + rnd.nextFloat() * 0.05f;
            col[i] = palette[rnd.nextInt(palette.length)];
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        canvas.drawRect(0, 0, w, h, bgPaint);
        canvas.drawRect(0, 0, w, h, glowPaint);
        long now = System.nanoTime();
        float dt = last == 0 ? 16f : Math.min(50f, (now - last) / 1_000_000f);
        last = now;
        for (int i = 0; i < COUNT; i++) {
            if (animate) {
                y[i] -= v[i] * dt;
                x[i] += (float) Math.sin((y[i] + i * 30) / 60f) * 0.15f;
                if (y[i] < -10) {
                    y[i] = h + 10;
                    x[i] = rnd.nextFloat() * w;
                }
            }
            float a = Math.max(0f, Math.min(1f, y[i] / h));
            dot.setColor(col[i]);
            dot.setAlpha((int) (40 + 150 * a));
            canvas.drawCircle(x[i], y[i], r[i], dot);
        }
        if (animate) postInvalidateOnAnimation();
    }
}
