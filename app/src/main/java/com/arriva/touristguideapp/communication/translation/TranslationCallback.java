package com.arriva.touristguideapp.communication.translation;

import androidx.annotation.NonNull;

public interface TranslationCallback {
    void onSuccess(@NonNull String translatedText);

    void onProgress(@NonNull String message);

    void onFailure(@NonNull String errorMessage);
}
