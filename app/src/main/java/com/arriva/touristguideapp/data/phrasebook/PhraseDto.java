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
    private String englishText;
    @Nullable
    private String marathiText;
    @Nullable
    private String hindiText;
    @Nullable
    private String category;

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
    public String getEnglishText() {
        return englishText;
    }

    public void setEnglishText(@Nullable String englishText) {
        this.englishText = englishText;
    }

    @Nullable
    public String getMarathiText() {
        return marathiText;
    }

    public void setMarathiText(@Nullable String marathiText) {
        this.marathiText = marathiText;
    }

    @Nullable
    public String getHindiText() {
        return hindiText;
    }

    public void setHindiText(@Nullable String hindiText) {
        this.hindiText = hindiText;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    public boolean isValid() {
        return englishText != null && !englishText.trim().isEmpty()
                && marathiText != null && !marathiText.trim().isEmpty()
                && hindiText != null && !hindiText.trim().isEmpty()
                && category != null && !category.trim().isEmpty();
    }
}
