package com.arriva.touristguideapp.communication.tts;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.communication.languages.LanguageConfig;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Language-agnostic TTS — locale is taken from {@link LanguageConfig}.
 */
public class UniversalTtsHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = "UniversalTts";
    private static final String UTTERANCE_ID = "communication_tts";

    public interface SpeakCallback {
        void onSpeakingStarted();

        void onSpeakingFinished(@Nullable String errorMessage);
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private TextToSpeech tts;
    private final AtomicBoolean ready = new AtomicBoolean(false);
    @Nullable
    private SpeakCallback speakCallback;

    public UniversalTtsHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
        tts = new TextToSpeech(appContext, this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS || tts == null) {
            Log.e(TAG, "TTS init failed status=" + status);
            ready.set(false);
            showToast(appContext.getString(R.string.tts_init_failed));
            return;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                notifyStarted();
            }

            @Override
            public void onDone(String utteranceId) {
                notifyFinished(null);
            }

            @Override
            public void onError(String utteranceId) {
                notifyFinished("TTS playback error");
            }
        });
        ready.set(true);
    }

    public void setSpeakCallback(@Nullable SpeakCallback callback) {
        speakCallback = callback;
    }

    @MainThread
    public void speak(@NonNull String text, @NonNull LanguageConfig language) {
        if (tts == null || !ready.get()) {
            notifyFinished("Text-to-speech is not ready yet");
            showToast(appContext.getString(R.string.tts_init_failed));
            return;
        }
        if (text.trim().isEmpty()) {
            return;
        }

        stop();
        int result = tts.setLanguage(language.getTtsLocale());
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "TTS missing for " + language.getDisplayName() + ", trying locale fallback");
            result = tts.setLanguage(Locale.forLanguageTag(language.getLanguageCode()));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                showToast(appContext.getString(R.string.tts_voice_missing, language.getDisplayName()));
                promptInstallTtsData();
                notifyFinished("Voice data not installed for " + language.getDisplayName());
                return;
            }
        }

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
    }

    private void promptInstallTtsData() {
        Intent installIntent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            appContext.startActivity(installIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch TTS installation activity", e);
        }
    }

    private void showToast(@NonNull String message) {
        mainHandler.post(() -> Toast.makeText(appContext, message, Toast.LENGTH_LONG).show());
    }

    @MainThread
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void shutdown() {
        stop();
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        ready.set(false);
    }

    private void notifyStarted() {
        mainHandler.post(() -> {
            if (speakCallback != null) {
                speakCallback.onSpeakingStarted();
            }
        });
    }

    private void notifyFinished(@Nullable String error) {
        mainHandler.post(() -> {
            if (speakCallback != null) {
                speakCallback.onSpeakingFinished(error);
            }
        });
    }
}
