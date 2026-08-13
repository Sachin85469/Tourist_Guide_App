package com.arriva.touristguideapp.communication;

import androidx.annotation.NonNull;

import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.arriva.touristguideapp.communication.speech.UniversalSpeechHelper;
import com.arriva.touristguideapp.communication.translation.TranslationManager;
import com.arriva.touristguideapp.communication.tts.UniversalTtsHelper;

/**
 * Shared communication services and language selection for hub fragments.
 */
public interface CommunicationHost {

    @NonNull
    LanguageConfig getSourceLanguage();

    @NonNull
    LanguageConfig getTargetLanguage();

    void setSourceLanguage(@NonNull LanguageConfig language);

    void setTargetLanguage(@NonNull LanguageConfig language);

    void swapLanguages();

    void notifyLanguagePairChanged();

    @NonNull
    TranslationManager getTranslationManager();

    @NonNull
    UniversalTtsHelper getTtsHelper();

    @NonNull
    CommunicationPreferences getCommunicationPreferences();

    void requestSpeechInput(@NonNull SpeechResultCallback callback);

    interface SpeechResultCallback {
        void onSpeechResult(@NonNull String text);

        void onSpeechFailed(@NonNull String message);
    }
}
