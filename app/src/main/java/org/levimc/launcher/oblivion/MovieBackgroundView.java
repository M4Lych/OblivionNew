package org.levimc.launcher.oblivion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.io.File;
import java.util.Random;

/**
 * Bright, blocky "adventure movie" backdrop drawn in code: aurora sky, three parallax layers of voxel cliffs,
 * mint tree canopies and twinkling sparkles. An OblivionDLC can replace it with an image.
 */
public class MovieBackgroundView extends View {
    private static final int LAYERS = 3;
    private final Paint fill = new Paint();
    private final Paint sky = new Paint();
    private final Paint shade = new Paint();
    private final Paint aurora = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path ribbon = new Path();
    private final Random rnd = new Random(26);
    private int[][] heights = new int[LAYERS][];
    private boolean[][] canopy = new boolean[LAYERS][];
    private float[] sx, sy, sv, sp;
    private boolean animate = true;
    private Bitmap override;
    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();
    private static final int[] LAYER_COLORS = {0xFFB4587A, 0xFFD6707F, 0xFF2FB7A6};
    private static final int[] TOP_COLORS = {0xFFC9739A, 0xFFEE8C92, 0xFF7FE8D4};
    private static final float[] SPEED = {6f, 14f, 30f};
    private static final int COLUMNS = 64;

    public MovieBackgroundView(Context c) {
        this(c, null);
    }

    public MovieBackgroundView(Context c, AttributeSet a) {
        super(c, a);
        for (int l = 0; l < LAYERS; l++) {
            heights[l] = new int[COLUMNS];
            canopy[l] = new boolean[COLUMNS];
            int h = 4 + rnd.nextInt(4);
            for (int i = 0; i < COLUMNS; i++) {
                int step = rnd.nextInt(3) - 1;
                if (rnd.nextInt(3) != 0) step = 0;
                h = Math.max(2, Math.min(11, h + step));
                heights[l][i] = h;
                canopy[l][i] = rnd.nextInt(6) == 0;
            }
        }
        sx = new float[48];
        sy = new float[48];
        sv = new float[48];
        sp = new float[48];
        for (int i = 0; i < sx.length; i++) {
            sx[i] = rnd.nextFloat();
            sy[i] = rnd.nextFloat();
            sv[i] = 0.004f + rnd.nextFloat() * 0.012f;
            sp[i] = rnd.nextFloat() * 6.28f;
        }
        loadOverride(c);
    }

    private void loadOverride(Context c) {
        try {
            File f = OblivionDlc.asset(c, "home_background");
            if (f != null) override = BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Throwable ignored) {
        }
    }

    public void setAnimated(boolean on) {
        animate = on;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        sky.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF6FD9D2, 0xFFA8C8E8, 0xFFF6A9C0, 0xFFFFD6B0}, new float[]{0f, 0.35f, 0.7f, 1f},
                Shader.TileMode.CLAMP));
        shade.setShader(new LinearGradient(0, h * 0.45f, 0, h,
                0x00000000, 0xB0000000, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        double t = System.nanoTime() / 1e9;
        if (override != null) {
            float s = Math.max(w / (float) override.getWidth(), h / (float) override.getHeight());
            int sw = Math.round(w / s), sh = Math.round(h / s);
            srcRect.set((override.getWidth() - sw) / 2, (override.getHeight() - sh) / 2,
                    (override.getWidth() + sw) / 2, (override.getHeight() + sh) / 2);
            dstRect.set(0, 0, w, h);
            canvas.drawBitmap(override, srcRect, dstRect, null);
        } else {
            canvas.drawRect(0, 0, w, h, sky);
            drawAurora(canvas, w, h, t);
            float block = h / 14f;
            for (int l = 0; l < LAYERS; l++) drawLayer(canvas, w, h, l, block, t);
            drawSparkles(canvas, w, h, t);
        }
        canvas.drawRect(0, h * 0.45f, w, h, shade);
        if (animate) postInvalidateOnAnimation();
    }

    private void drawAurora(Canvas canvas, int w, int h, double t) {
        int[] cols = {0x6632F0B4, 0x55FF7EB6, 0x4458D8FF};
        for (int r = 0; r < cols.length; r++) {
            ribbon.reset();
            float base = h * (0.18f + 0.09f * r);
            ribbon.moveTo(0, base);
            for (int x = 0; x <= w; x += 24) {
                float y = base + (float) Math.sin(x / (w * 0.18f) + t * (0.35 + 0.1 * r) + r * 2) * h * 0.05f;
                ribbon.lineTo(x, y);
            }
            ribbon.lineTo(w, base + h * 0.16f);
            ribbon.lineTo(0, base + h * 0.16f);
            ribbon.close();
            aurora.setShader(new LinearGradient(0, base, 0, base + h * 0.16f, cols[r], 0x00000000,
                    Shader.TileMode.CLAMP));
            canvas.drawPath(ribbon, aurora);
        }
        aurora.setShader(null);
    }

    private void drawLayer(Canvas canvas, int w, int h, int l, float block, double t) {
        float colW = Math.max(block * 1.4f, w / (float) (COLUMNS - 8));
        float span = colW * COLUMNS;
        float off = animate ? (float) ((t * SPEED[l]) % span) : 0f;
        float baseY = h * (0.62f + 0.12f * l);
        for (int copy = -1; copy <= 1; copy++) {
            for (int i = 0; i < COLUMNS; i++) {
                float x = i * colW - off + copy * span;
                if (x > w || x + colW < 0) continue;
                float top = baseY - heights[l][i] * block * (0.9f + 0.25f * l);
                fill.setColor(LAYER_COLORS[l]);
                canvas.drawRect(x, top, x + colW + 1, h, fill);
                fill.setColor(TOP_COLORS[l]);
                canvas.drawRect(x, top, x + colW + 1, top + block * 0.35f, fill);
                if (canopy[l][i]) {
                    fill.setColor(l == 2 ? 0xFF9BF2DF : 0xFFF2C8D6);
                    canvas.drawRect(x - block * 0.2f, top - block * 1.4f, x + colW + block * 0.2f, top, fill);
                    fill.setColor(0xFFFFFFFF);
                    canvas.drawRect(x, top - block * 1.4f, x + colW * 0.5f, top - block * 1.0f, fill);
                }
            }
        }
    }

    private void drawSparkles(Canvas canvas, int w, int h, double t) {
        for (int i = 0; i < sx.length; i++) {
            float y = (sy[i] - (animate ? (float) (t * sv[i]) : 0f)) % 1f;
            if (y < 0) y += 1f;
            float a = 0.5f + 0.5f * (float) Math.sin(t * 2 + sp[i]);
            fill.setColor(((int) (60 + 170 * a) << 24) | 0xFFFFFF);
            float s = h * 0.006f * (1f + (i % 3));
            canvas.drawRect(sx[i] * w, y * h, sx[i] * w + s, y * h + s, fill);
        }
    }
}
