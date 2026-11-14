package com.oblivion.client.ui.dialogs.gameversionselect;

import com.oblivion.client.core.versions.GameVersion;

import java.util.ArrayList;
import java.util.List;

public class VersionGroup {
    public String versionCode;
    public List<GameVersion> versions = new ArrayList<>();

    public VersionGroup(String code) {
        this.versionCode = code;
    }
}