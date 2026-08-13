package com.arriva.touristguideapp.communication.languages;

import androidx.annotation.NonNull;

import com.google.mlkit.nl.translate.TranslateLanguage;

import java.util.Locale;

/**
 * Immutable definition for a supported app language.
 * Add new languages in {@link LanguageRegistry} only — no UI or translation hardcoding elsewhere.
 */
public final class LanguageConfig {

    @NonNull
    private final String displayName;
    @NonNull
    private final String languageCode;
    @NonNull
    private final String localeCode;
    @NonNull
    private final String speechRecognizerCode;
    @NonNull
    private final Locale ttsLocale;
    @NonNull
    private final String mlKitLanguageCode;

    public LanguageConfig(@NonNull String displayName,
                          @NonNull String languageCode,
                          @NonNull String localeCode,
                          @NonNull String speechRecognizerCode,
                          @NonNull Locale ttsLocale,
                          @NonNull String mlKitLanguageCode) {
        this.displayName = displayName;
        this.languageCode = languageCode;
        this.localeCode = localeCode;
        this.speechRecognizerCode = speechRecognizerCode;
        this.ttsLocale = ttsLocale;
        this.mlKitLanguageCode = mlKitLanguageCode;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getLanguageCode() {
        return languageCode;
    }

    @NonNull
    public String getLocaleCode() {
        return localeCode;
    }

    @NonNull
    public String getSpeechRecognizerCode() {
        return speechRecognizerCode;
    }

    @NonNull
    public Locale getTtsLocale() {
        return ttsLocale;
    }

    @NonNull
    public String getMlKitLanguageCode() {
        return mlKitLanguageCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LanguageConfig)) {
            return false;
        }
        LanguageConfig that = (LanguageConfig) o;
        return languageCode.equals(that.languageCode);
    }

    @Override
    public int hashCode() {
        return languageCode.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return displayName;
    }

    /** Factory for common Indian languages using ML Kit codes. */
    @NonNull
    public static LanguageConfig of(@NonNull String displayName,
                                    @NonNull String code,
                                    @NonNull String mlKitCode) {
        String localeTag = code + "-IN";
        return new LanguageConfig(
                displayName,
                code,
                localeTag,
                localeTag,
                Locale.forLanguageTag(localeTag),
                mlKitCode
        );
    }

    @NonNull
    public static LanguageConfig english() {
        return new LanguageConfig(
                "English",
                "en",
                "en-IN",
                "en-IN",
                Locale.forLanguageTag("en-IN"),
                TranslateLanguage.ENGLISH
        );
    }
}
