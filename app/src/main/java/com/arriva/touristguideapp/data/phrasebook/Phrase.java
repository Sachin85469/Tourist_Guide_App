package com.arriva.touristguideapp.data.phrasebook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Language-agnostic phrase stored in Firestore (base text only; translations are dynamic).
 */
public class Phrase {

    @NonNull
    private final String id;
    @NonNull
    private final String baseText;
    @NonNull
    private final String baseLanguage;
    @NonNull
    private final String category;

    public Phrase(@NonNull String id,
                  @NonNull String baseText,
                  @NonNull String baseLanguage,
                  @NonNull String category) {
        this.id = id;
        this.baseText = baseText;
        this.baseLanguage = baseLanguage;
        this.category = category;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getBaseText() {
        return baseText;
    }

    @NonNull
    public String getBaseLanguage() {
        return baseLanguage;
    }

    @NonNull
    public String getCategory() {
        return category;
    }

    public boolean matchesBaseQuery(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return baseText.toLowerCase().contains(q);
    }

    public boolean matchesTranslatedQuery(@Nullable String query, @Nullable String translatedText) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        if (translatedText != null && translatedText.toLowerCase().contains(query.trim().toLowerCase())) {
            return true;
        }
        return false;
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
