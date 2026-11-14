package com.oblivion.client.ui.widget;

import android.content.Context;
import android.view.View;

import com.oblivion.client.R;
import com.oblivion.client.core.versions.GameVersion;
import com.oblivion.client.settings.FeatureSettings;
import com.oblivion.client.util.LauncherStorage;

import java.util.function.Consumer;

public final class ContentStorageSelector {
    private ContentStorageSelector() {
    }

    public static String label(Context context, FeatureSettings.StorageType type) {
        return switch (type) {
            case INTERNAL -> context.getString(R.string.storage_internal);
            case EXTERNAL -> context.getString(R.string.storage_external);
            case VERSION_ISOLATION_INTERNAL -> context.getString(R.string.storage_version_isolation)
                    + " (" + context.getString(R.string.storage_internal) + ")";
            case VERSION_ISOLATION, VERSION_ISOLATION_EXTERNAL -> context.getString(R.string.storage_version_isolation)
                    + " (" + context.getString(R.string.storage_external) + ")";
        };
    }

    public static void show(View anchor, GameVersion version, FeatureSettings.StorageType current,
                            Consumer<FeatureSettings.StorageType> onSelect) {
        if (version == null) return;
        FeatureSettings.StorageType[] choices = {
                LauncherStorage.normalizeContentStorageType(FeatureSettings.StorageType.INTERNAL, version.versionIsolation),
                LauncherStorage.normalizeContentStorageType(FeatureSettings.StorageType.EXTERNAL, version.versionIsolation)
        };
        FeatureSettings.StorageType selected = LauncherStorage.normalizeContentStorageType(current, version.versionIsolation);
        LauncherPopup popup = new LauncherPopup(anchor).width(340)
                .section(anchor.getContext().getString(R.string.content_ui_storage_location));
        for (int i = 0; i < choices.length; i++) {
            popup.choice(i, label(anchor.getContext(), choices[i]), choices[i] == selected);
        }
        popup.onSelect(id -> {
            if (id >= 0 && id < choices.length && choices[id] != selected) onSelect.accept(choices[id]);
        }).show();
    }
}
