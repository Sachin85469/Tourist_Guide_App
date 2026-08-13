package com.arriva.touristguideapp.communication.languages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.nl.translate.TranslateLanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry of supported languages. Extend this list to add new languages app-wide.
 */
public final class LanguageRegistry {

    private static final List<LanguageConfig> LANGUAGES;
    private static final Map<String, LanguageConfig> BY_CODE;

    static {
        List<LanguageConfig> list = new ArrayList<>();
        list.add(LanguageConfig.english());
        list.add(LanguageConfig.of("Marathi", "mr", TranslateLanguage.MARATHI));
        list.add(LanguageConfig.of("Hindi", "hi", TranslateLanguage.HINDI));
        list.add(LanguageConfig.of("Gujarati", "gu", TranslateLanguage.GUJARATI));
        list.add(LanguageConfig.of("Tamil", "ta", TranslateLanguage.TAMIL));
        list.add(LanguageConfig.of("Bengali", "bn", TranslateLanguage.BENGALI));
        list.add(LanguageConfig.of("Telugu", "te", TranslateLanguage.TELUGU));
        list.add(LanguageConfig.of("Kannada", "kn", TranslateLanguage.KANNADA));
        list.add(LanguageConfig.of("Malayalam", "ml", "ml"));
        list.add(LanguageConfig.of("Punjabi", "pa", "pa"));
        list.add(LanguageConfig.of("Urdu", "ur", TranslateLanguage.URDU));

        LANGUAGES = Collections.unmodifiableList(list);
        Map<String, LanguageConfig> map = new LinkedHashMap<>();
        for (LanguageConfig config : list) {
            map.put(config.getLanguageCode(), config);
        }
        BY_CODE = Collections.unmodifiableMap(map);
    }

    private LanguageRegistry() {
    }

    @NonNull
    public static List<LanguageConfig> getSupportedLanguages() {
        return LANGUAGES;
    }

    @NonNull
    public static String[] getDisplayNames() {
        String[] names = new String[LANGUAGES.size()];
        for (int i = 0; i < LANGUAGES.size(); i++) {
            names[i] = LANGUAGES.get(i).getDisplayName();
        }
        return names;
    }

    @Nullable
    public static LanguageConfig fromCode(@Nullable String code) {
        if (code == null) {
            return null;
        }
        return BY_CODE.get(code);
    }

    @NonNull
    public static LanguageConfig requireFromCode(@NonNull String code) {
        LanguageConfig config = fromCode(code);
        if (config == null) {
            return LanguageConfig.english();
        }
        return config;
    }

    public static int indexOf(@NonNull LanguageConfig config) {
        return LANGUAGES.indexOf(config);
    }

    @NonNull
    public static LanguageConfig fromDisplayName(@NonNull String displayName) {
        for (LanguageConfig config : LANGUAGES) {
            if (config.getDisplayName().equals(displayName)) {
                return config;
            }
        }
        return LanguageConfig.english();
    }
}
