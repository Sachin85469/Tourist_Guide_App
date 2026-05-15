package com.arriva.touristguideapp.data.phrasebook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Firestore document shape for {@link PhrasebookFirestoreContract#COLLECTION_PHRASEBOOK}.
 */
public class PhraseDto {

    @NonNull
    private String id = "";
    @Nullable
    private String baseText;
    @Nullable
    private String baseLanguage;
    @Nullable
    private String category;
    @Nullable
    private String englishText;

    public PhraseDto() {
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @Nullable
    public String getBaseText() {
        return baseText;
    }

    public void setBaseText(@Nullable String baseText) {
        this.baseText = baseText;
    }

    @Nullable
    public String getBaseLanguage() {
        return baseLanguage;
    }

    public void setBaseLanguage(@Nullable String baseLanguage) {
        this.baseLanguage = baseLanguage;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    @Nullable
    public String getEnglishText() {
        return englishText;
    }

    public void setEnglishText(@Nullable String englishText) {
        this.englishText = englishText;
    }

    @NonNull
    public String resolveBaseText() {
        if (baseText != null && !baseText.trim().isEmpty()) {
            return baseText.trim();
        }
        if (englishText != null && !englishText.trim().isEmpty()) {
            return englishText.trim();
        }
        return "";
    }

    @NonNull
    public String resolveBaseLanguage() {
        if (baseLanguage != null && !baseLanguage.trim().isEmpty()) {
            return baseLanguage.trim();
        }
        return "en";
    }

    public boolean isValid() {
        return !resolveBaseText().isEmpty()
                && category != null && !category.trim().isEmpty();
    }
}
