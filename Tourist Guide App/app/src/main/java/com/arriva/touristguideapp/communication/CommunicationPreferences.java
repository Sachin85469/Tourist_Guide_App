package com.arriva.touristguideapp.communication;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.arriva.touristguideapp.communication.languages.LanguageRegistry;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Favorites, recent phrases, translation history, and pinned language pair.
 */
public class CommunicationPreferences {

    private static final String PREFS = "communication_prefs";
    private static final String KEY_FAVORITES = "favorite_phrase_ids";
    private static final String KEY_RECENT_PHRASES = "recent_phrase_ids";
    private static final String KEY_RECENT_TRANSLATIONS = "recent_translations";
    private static final String KEY_SOURCE_LANG = "source_lang_code";
    private static final String KEY_TARGET_LANG = "target_lang_code";
    private static final int MAX_RECENT = 15;

    private final SharedPreferences prefs;

    public CommunicationPreferences(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public LanguageConfig getSourceLanguage() {
        return LanguageRegistry.requireFromCode(prefs.getString(KEY_SOURCE_LANG, "en"));
    }

    @NonNull
    public LanguageConfig getTargetLanguage() {
        return LanguageRegistry.requireFromCode(prefs.getString(KEY_TARGET_LANG, "mr"));
    }

    public void saveLanguagePair(@NonNull LanguageConfig source, @NonNull LanguageConfig target) {
        prefs.edit()
                .putString(KEY_SOURCE_LANG, source.getLanguageCode())
                .putString(KEY_TARGET_LANG, target.getLanguageCode())
                .apply();
    }

    public void swapLanguages() {
        LanguageConfig source = getSourceLanguage();
        LanguageConfig target = getTargetLanguage();
        saveLanguagePair(target, source);
    }

    public boolean isFavorite(@NonNull String phraseId) {
        return getFavoriteIds().contains(phraseId);
    }

    public void toggleFavorite(@NonNull String phraseId) {
        Set<String> favorites = new LinkedHashSet<>(getFavoriteIds());
        if (favorites.contains(phraseId)) {
            favorites.remove(phraseId);
        } else {
            favorites.add(phraseId);
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    @NonNull
    public Set<String> getFavoriteIds() {
        Set<String> stored = prefs.getStringSet(KEY_FAVORITES, null);
        return stored == null ? Collections.emptySet() : new LinkedHashSet<>(stored);
    }

    public void recordPhrasePlayed(@NonNull String phraseId) {
        List<String> recent = new ArrayList<>(getRecentPhraseIds());
        recent.remove(phraseId);
        recent.add(0, phraseId);
        trimAndSave(KEY_RECENT_PHRASES, recent);
    }

    @NonNull
    public List<String> getRecentPhraseIds() {
        return readList(KEY_RECENT_PHRASES);
    }

    public void addTranslationHistory(@NonNull String sourceText,
                                      @NonNull String translatedText,
                                      @NonNull String sourceCode,
                                      @NonNull String targetCode) {
        String entry = sourceCode + "|" + targetCode + "|" + sourceText + "|||" + translatedText;
        List<String> history = new ArrayList<>(readList(KEY_RECENT_TRANSLATIONS));
        history.remove(entry);
        history.add(0, entry);
        trimAndSave(KEY_RECENT_TRANSLATIONS, history);
    }

    @NonNull
    public List<TranslationHistoryEntry> getTranslationHistory() {
        List<TranslationHistoryEntry> entries = new ArrayList<>();
        for (String raw : readList(KEY_RECENT_TRANSLATIONS)) {
            TranslationHistoryEntry entry = TranslationHistoryEntry.fromRaw(raw);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    @NonNull
    private List<String> readList(@NonNull String key) {
        String raw = prefs.getString(key, "");
        List<String> list = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private void trimAndSave(@NonNull String key, @NonNull List<String> values) {
        if (values.size() > MAX_RECENT) {
            values = values.subList(0, MAX_RECENT);
        }
        JSONArray array = new JSONArray();
        for (String v : values) {
            array.put(v);
        }
        prefs.edit().putString(key, array.toString()).apply();
    }

    public static final class TranslationHistoryEntry {
        @NonNull
        public final String sourceCode;
        @NonNull
        public final String targetCode;
        @NonNull
        public final String sourceText;
        @NonNull
        public final String translatedText;

        public TranslationHistoryEntry(@NonNull String sourceCode,
                                       @NonNull String targetCode,
                                       @NonNull String sourceText,
                                       @NonNull String translatedText) {
            this.sourceCode = sourceCode;
            this.targetCode = targetCode;
            this.sourceText = sourceText;
            this.translatedText = translatedText;
        }

        @Nullable
        static TranslationHistoryEntry fromRaw(@NonNull String raw) {
            String[] parts = raw.split("\\|\\|\\|", 2);
            if (parts.length != 2) {
                return null;
            }
            String[] meta = parts[0].split("\\|", 3);
            if (meta.length != 3) {
                return null;
            }
            return new TranslationHistoryEntry(meta[0], meta[1], meta[2], parts[1]);
        }
    }
}
