package com.arriva.touristguideapp.data.phrasebook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Phrase with predefined translations for high stability and instant access.
 */
public class Phrase {

    @NonNull
    private final String id;
    @NonNull
    private final String englishText;
    @NonNull
    private final String hindiText;
    @NonNull
    private final String marathiText;
    @NonNull
    private final String category;

    public Phrase(@NonNull String id,
                  @NonNull String englishText,
                  @NonNull String hindiText,
                  @NonNull String marathiText,
                  @NonNull String category) {
        this.id = id;
        this.englishText = englishText;
        this.hindiText = hindiText;
        this.marathiText = marathiText;
        this.category = category;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getEnglishText() {
        return englishText;
    }

    @NonNull
    public String getHindiText() {
        return hindiText;
    }

    @NonNull
    public String getMarathiText() {
        return marathiText;
    }

    @NonNull
    public String getEnglish() {
        return englishText;
    }

    @NonNull
    public String getHindi() {
        return hindiText;
    }

    @NonNull
    public String getMarathi() {
        return marathiText;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    @NonNull
    public String getTranslation(@NonNull String targetLangCode) {
        if ("mr".equalsIgnoreCase(targetLangCode)) {
            return marathiText;
        } else if ("hi".equalsIgnoreCase(targetLangCode)) {
            return hindiText;
        }
        return englishText;
    }

    public boolean matchesQuery(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return englishText.toLowerCase().contains(q) 
                || hindiText.contains(q) 
                || marathiText.contains(q);
    }

    public boolean matchesCategory(@Nullable String categoryFilter) {
        if (categoryFilter == null
                || categoryFilter.isEmpty()
                || PhrasebookFirestoreContract.CATEGORY_ALL.equals(categoryFilter)) {
            return true;
        }
        return category.equalsIgnoreCase(categoryFilter);
    }
}
