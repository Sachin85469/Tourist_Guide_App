package com.arriva.touristguideapp.communication.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.data.phrasebook.Phrase;

/**
 * Phrase row with dynamic translation state for the RecyclerView.
 */
public class PhraseDisplayItem {

    public enum TranslationUiState {
        IDLE,
        LOADING,
        READY,
        ERROR
    }

    @NonNull
    private final Phrase phrase;
    @Nullable
    private String translatedText;
    @NonNull
    private TranslationUiState state = TranslationUiState.IDLE;
    @Nullable
    private String errorMessage;

    public PhraseDisplayItem(@NonNull Phrase phrase) {
        this.phrase = phrase;
    }

    @NonNull
    public Phrase getPhrase() {
        return phrase;
    }

    @Nullable
    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(@Nullable String translatedText) {
        this.translatedText = translatedText;
    }

    @NonNull
    public TranslationUiState getState() {
        return state;
    }

    public void setState(@NonNull TranslationUiState state) {
        this.state = state;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
