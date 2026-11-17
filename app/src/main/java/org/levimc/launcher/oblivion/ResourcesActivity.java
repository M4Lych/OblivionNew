package org.levimc.launcher.oblivion;

import android.content.Intent;
import android.widget.LinearLayout;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.activities.ContentListActivity;
import org.levimc.launcher.ui.activities.ContentManagementActivity;
import org.levimc.launcher.ui.activities.CurseForgeActivity;
import org.levimc.launcher.ui.activities.ModsFullscreenActivity;

/** Resources hub: import/export, OblivionMod, CurseForge and resource packs. */
public class ResourcesActivity extends OblivionBaseActivity {
    @Override
    protected int titleRes() {
        return R.string.oblivion_resources;
    }

    @Override
    protected void buildContent(LinearLayout col) {
        col.addView(OUi.tile(this, getString(R.string.oblivion_res_import_export),
                getString(R.string.oblivion_res_import_export_sub),
                v -> startActivity(new Intent(this, ContentManagementActivity.class))));
        col.addView(OUi.tile(this, "OblivionMod", getString(R.string.oblivion_res_mods_sub),
                v -> startActivity(new Intent(this, OblivionModActivity.class))));
        col.addView(OUi.tile(this, "CurseForge", getString(R.string.oblivion_res_curseforge_sub),
                v -> startActivity(new Intent(this, CurseForgeActivity.class))));
        col.addView(OUi.tile(this, getString(R.string.oblivion_res_packs),
                getString(R.string.oblivion_res_packs_sub), v -> {
                    Intent i = new Intent(this, ContentListActivity.class);
                    i.putExtra(ContentListActivity.EXTRA_CONTENT_TYPE, ContentListActivity.TYPE_RESOURCE_PACKS);
                    startActivity(i);
                }));
        col.addView(OUi.tile(this, getString(R.string.oblivion_res_native_mods),
                getString(R.string.oblivion_res_native_mods_sub),
                v -> startActivity(new Intent(this, ModsFullscreenActivity.class))));
    }
}
