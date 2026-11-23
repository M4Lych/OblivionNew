package org.levimc.launcher.ui.stage;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;

public final class StageView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String TAG = "OblivionStage";

    private SurfaceTexture surface;
    private int surfaceWidth;
    private int surfaceHeight;
    private RenderThread thread;
    private String packId = "default";
    private int fps = 30;
    private float downX;
    private float downY;

    public StageView(Context context) {
        super(context);
        setOpaque(false);
        setSurfaceTextureListener(this);
    }

    public synchronized void configure(String id, int frameRate) {
        boolean restart = !id.equals(packId);
        packId = id;
        fps = frameRate;
        if (thread != null) thread.setFps(frameRate);
        if (restart && surface != null) {
            stopThread();
            startThread();
        }
    }

    private synchronized void startThread() {
        if (surface == null || thread != null) return;
        thread = new RenderThread(getContext().getApplicationContext(), surface, packId, fps);
        thread.setSize(surfaceWidth, surfaceHeight);
        thread.start();
    }

    private synchronized void stopThread() {
        RenderThread t = thread;
        thread = null;
        if (t == null) return;
        t.finish();
        try {
            t.join(1500);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
        synchronized (this) {
            surface = st;
            surfaceWidth = width;
            surfaceHeight = height;
        }
        startThread();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
        synchronized (this) {
            surfaceWidth = width;
            surfaceHeight = height;
            if (thread != null) thread.setSize(width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
        stopThread();
        synchronized (this) {
            surface = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture st) {
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        RenderThread t = thread;
        if (t != null) t.setPaused(visibility != VISIBLE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int w = getWidth();
        int h = getHeight();
        RenderThread t = thread;
        if (w == 0 || h == 0 || t == null) return false;
        float nx = e.getX() / w * 2f - 1f;
        float ny = -(e.getY() / h * 2f - 1f);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                t.look(nx, ny);
                return true;
            case MotionEvent.ACTION_MOVE:
                t.look(nx, ny);
                return true;
            case MotionEvent.ACTION_UP:
                if (Math.abs(e.getX() - downX) < 24f && Math.abs(e.getY() - downY) < 24f) t.react();
                t.look(0f, 0f);
                return true;
            case MotionEvent.ACTION_CANCEL:
                t.look(0f, 0f);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent e) {
        RenderThread t = thread;
        int w = getWidth();
        int h = getHeight();
        if (t != null && w > 0 && h > 0 && e.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
            t.look(e.getX() / w * 2f - 1f, -(e.getY() / h * 2f - 1f));
            return true;
        }
        return super.onGenericMotionEvent(e);
    }

    private static final class RenderThread extends Thread {
        private final Context context;
        private final SurfaceTexture texture;
        private final String packId;
        private volatile boolean running = true;
        private volatile boolean paused;
        private volatile int width;
        private volatile int height;
        private volatile int frameRate;
        private volatile StageAnimator animator;
        private volatile float pendingX;
        private volatile float pendingY;
        private volatile boolean pendingReact;

        RenderThread(Context context, SurfaceTexture texture, String packId, int frameRate) {
            super("OblivionStage");
            this.context = context;
            this.texture = texture;
            this.packId = packId;
            this.frameRate = frameRate;
        }

        void setSize(int w, int h) {
            width = w;
            height = h;
        }

        void setFps(int fps) {
            frameRate = fps;
        }

        void setPaused(boolean value) {
            paused = value;
        }

        void look(float x, float y) {
            pendingX = x;
            pendingY = y;
            StageAnimator a = animator;
            if (a != null) a.setLook(x, y);
        }

        void react() {
            pendingReact = true;
            StageAnimator a = animator;
            if (a != null) a.react();
        }

        void finish() {
            running = false;
        }

        private static EGLConfig choose(EGLDisplay display, int samples) {
            int[] attrs = new int[]{
                    EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 16,
                    EGL14.EGL_SAMPLES, samples,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] count = new int[1];
            if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0) || count[0] <= 0) return null;
            return configs[0];
        }

        @Override
        public void run() {
            EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
            EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
            StageRenderer renderer = null;
            try {
                if (display == EGL14.EGL_NO_DISPLAY) return;
                int[] version = new int[2];
                if (!EGL14.eglInitialize(display, version, 0, version, 1)) return;
                EGLConfig config = choose(display, 4);
                if (config == null) config = choose(display, 0);
                if (config == null) return;
                eglContext = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT,
                        new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE}, 0);
                if (eglContext == EGL14.EGL_NO_CONTEXT) return;
                eglSurface = EGL14.eglCreateWindowSurface(display, config, texture, new int[]{EGL14.EGL_NONE}, 0);
                if (eglSurface == EGL14.EGL_NO_SURFACE) return;
                if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) return;

                ModelPack pack = ModelPack.open(context, packId);
                ObmModel model = ObmModel.parse(ObmModel.readAll(pack.source.open(pack.meshFile)));
                renderer = new StageRenderer(model, pack);
                renderer.init();
                StageAnimator anim = new StageAnimator(model, pack);
                anim.setLook(pendingX, pendingY);
                if (pendingReact) anim.react();
                animator = anim;

                long last = System.nanoTime();
                while (running) {
                    if (paused || width <= 0 || height <= 0) {
                        Thread.sleep(120);
                        last = System.nanoTime();
                        continue;
                    }
                    long frameStart = System.nanoTime();
                    float dt = Math.min(0.1f, (frameStart - last) / 1.0e9f);
                    last = frameStart;
                    anim.update(dt);
                    renderer.draw(anim, width, height);
                    if (!EGL14.eglSwapBuffers(display, eglSurface)) break;
                    long spent = (System.nanoTime() - frameStart) / 1000000L;
                    long target = 1000L / Math.max(1, frameRate);
                    if (spent < target) Thread.sleep(target - spent);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                Log.w(TAG, "stage renderer stopped: " + t);
            } finally {
                animator = null;
                try {
                    if (renderer != null) renderer.release();
                } catch (Throwable ignored) {
                }
                if (display != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                    if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface);
                    if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext);
                }
            }
        }
    }
}
