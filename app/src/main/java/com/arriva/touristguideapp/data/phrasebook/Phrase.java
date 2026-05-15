package com.arriva.touristguideapp.data.phrasebook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * UI model for a travel phrase with English, Marathi, and Hindi text.
 */
public class Phrase {

    @NonNull
    private final String id;
    @NonNull
    private final String englishText;
    @NonNull
    private final String marathiText;
    @NonNull
    private final String hindiText;
    @NonNull
    private final String category;

    public Phrase(@NonNull String id,
                  @NonNull String englishText,
                  @NonNull String marathiText,
                  @NonNull String hindiText,
                  @NonNull String category) {
        this.id = id;
        this.englishText = englishText;
        this.marathiText = marathiText;
        this.hindiText = hindiText;
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
    public String getMarathiText() {
        return marathiText;
    }

    @NonNull
    public String getHindiText() {
        return hindiText;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    /** Case-insensitive match across all three languages. */
    public boolean matchesQuery(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return englishText.toLowerCase().contains(q)
                || marathiText.contains(query.trim())
                || hindiText.contains(query.trim());
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
