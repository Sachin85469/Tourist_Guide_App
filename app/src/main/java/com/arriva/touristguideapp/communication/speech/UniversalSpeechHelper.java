package com.arriva.touristguideapp.communication.speech;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.communication.languages.LanguageConfig;

import java.util.ArrayList;

/**
 * Starts system speech recognition using the selected source language locale.
 */
public final class UniversalSpeechHelper {

    public static final int REQUEST_CODE = 9101;

    private UniversalSpeechHelper() {
    }

    public static void startListening(@NonNull Activity activity,
                                      @NonNull LanguageConfig sourceLanguage) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLanguage.getSpeechRecognizerCode());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                activity.getString(R.string.communication_speech_prompt, sourceLanguage.getDisplayName()));
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try {
            activity.startActivityForResult(intent, REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(activity, R.string.communication_speech_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    public static String parseResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_CODE || resultCode != Activity.RESULT_OK || data == null) {
            return null;
        }
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }
}
