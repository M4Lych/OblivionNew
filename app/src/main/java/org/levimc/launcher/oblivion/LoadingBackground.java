package org.levimc.launcher.oblivion;

import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;

import org.levimc.launcher.R;
import org.levimc.launcher.core.versions.GameVersion;
import org.levimc.launcher.util.InstanceShortcutManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Background for the "Starting Minecraft" screen: a custom video (filesDir/oblivion/loading.mp4 or
 * assets/oblivion/loading.mp4) when present, otherwise the animated Oblivion backdrop.
 * Purely visual - it never touches the launch steps.
 */
public final class LoadingBackground {
    private LoadingBackground() {
    }

    public static File userVideo(Activity a) {
        File d = new File(a.getFilesDir(), "oblivion");
        if (!d.isDirectory()) d.mkdirs();
        return new File(d, "loading.mp4");
    }

    public static void attach(Activity activity) {
        try {
            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null || content.getChildCount() == 0) return;
            View root = content.getChildAt(0);
            root.setBackgroundColor(Color.TRANSPARENT);
            OblivionTheme.ensureLoaded(activity);

            File video = null;
            if (OblivionSettings.get(activity, OblivionSettings.LOADING_VIDEO)) video = resolveVideo(activity);
            View bg;
            if (video != null) {
                VideoView vv = new VideoView(activity);
                vv.setVideoURI(Uri.fromFile(video));
                vv.setOnPreparedListener((MediaPlayer mp) -> {
                    mp.setLooping(true);
                    mp.setVolume(0f, 0f);
                });
                vv.setOnErrorListener((mp, what, extra) -> true);
                vv.start();
                bg = vv;
            } else {
                AmbientBackgroundView ambient = new AmbientBackgroundView(activity);
                ambient.setAnimated(OblivionSettings.get(activity, OblivionSettings.AMBIENT_ANIM));
                bg = ambient;
            }
            content.addView(bg, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Throwable ignored) {
        }
    }

    private static File resolveVideo(Activity a) {
        File user = userVideo(a);
        if (user.isFile() && user.length() > 0) return user;
        try {
            String[] list = a.getAssets().list("oblivion");
            if (list != null) {
                for (String n : list) {
                    if (n.equals("loading.mp4")) {
                        File cache = new File(a.getCacheDir(), "oblivion_loading.mp4");
                        if (!cache.isFile()) {
                            try (InputStream in = a.getAssets().open("oblivion/loading.mp4");
                                 FileOutputStream out = new FileOutputStream(cache)) {
                                byte[] buf = new byte[16384];
                                int r;
                                while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
                            }
                        }
                        return cache;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Uses the Oblivion logo instead of the Minecraft cube unless the instance has its own icon. */
    public static void applyLogo(ImageView logo, GameVersion version) {
        try {
            String saved = version == null ? null
                    : InstanceShortcutManager.getSavedIconUri(logo.getContext(), version);
            if (saved == null || saved.isEmpty()) logo.setImageResource(R.drawable.ic_leaf_logo);
        } catch (Throwable ignored) {
        }
    }
}
