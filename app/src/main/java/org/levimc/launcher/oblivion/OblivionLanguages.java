package org.levimc.launcher.oblivion;

import java.util.Locale;

/** The 20 languages offered by Oblivion (codes are BCP-47 tags understood by AppCompat per-app locales). */
public final class OblivionLanguages {
    public static final String[] CODES = {
            "en", "zh-CN", "hi", "es", "fr", "ar", "bn", "pt", "ru", "ur",
            "id", "de", "ja", "tr", "vi", "ko", "it", "th", "pl", "fa"
    };
    public static final String[] NAMES = {
            "English", "中文 (简体)", "हिन्दी", "Español", "Français", "العربية", "বাংলা", "Português", "Русский", "اردو",
            "Bahasa Indonesia", "Deutsch", "日本語", "Türkçe", "Tiếng Việt", "한국어", "Italiano", "ไทย", "Polski", "فارسی"
    };

    private OblivionLanguages() {
    }

    public static int indexOf(String tag) {
        if (tag == null) return 0;
        String lang = Locale.forLanguageTag(tag).getLanguage();
        if (lang.equals("in")) lang = "id";
        for (int i = 0; i < CODES.length; i++) {
            if (Locale.forLanguageTag(CODES[i]).getLanguage().equals(lang)) return i;
        }
        return 0;
    }
}
