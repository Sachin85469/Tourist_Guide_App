package com.arriva.touristguideapp.phrasebook;

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

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Text-to-speech for Marathi and Hindi phrase playback.
 * Stops any in-progress speech before starting a new utterance.
 */
public class PhrasebookTtsHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = "PhrasebookTts";
    private static final String UTTERANCE_ID = "phrasebook_utterance";

    public enum Language {
        MARATHI(new Locale("mr", "IN")),
        HINDI(new Locale("hi", "IN")),
        ENGLISH(Locale.US);

        private final Locale locale;

        Language(Locale locale) {
            this.locale = locale;
        }

        public Locale getLocale() {
            return locale;
        }
    }

    public interface SpeakCallback {
        void onSpeakingStarted();

        void onSpeakingFinished();
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private TextToSpeech tts;
    private final AtomicBoolean ready = new AtomicBoolean(false);
    @Nullable
    private SpeakCallback speakCallback;

    public PhrasebookTtsHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
        Log.d(TAG, "Initializing TTS Engine...");
        tts = new TextToSpeech(appContext, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS Initialization successful");
            if (tts != null) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        notifySpeakingStarted();
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        notifySpeakingFinished();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "Utterance error for ID: " + utteranceId);
                        notifySpeakingFinished();
                    }
                });
                ready.set(true);
            }
        } else {
            Log.e(TAG, "TTS Initialization failed with status: " + status);
            ready.set(false);
            showToast(appContext.getString(R.string.tts_init_failed));
        }
    }

    public void setSpeakCallback(@Nullable SpeakCallback callback) {
        speakCallback = callback;
    }

    @MainThread
    public void speak(@NonNull String text, @NonNull Language language) {
        if (tts == null || !ready.get()) {
            Log.w(TAG, "speak() called but TTS not ready. Attempting re-init.");
            if (tts == null) {
                tts = new TextToSpeech(appContext, this);
            }
            showToast(appContext.getString(R.string.tts_init_failed));
            return;
        }

        if (text.trim().isEmpty()) {
            Log.w(TAG, "speak() ignored: empty text");
            return;
        }

        Log.d(TAG, "speak() requested for " + language.name() + ": " + text);

        stop();

        int result = tts.setLanguage(language.getLocale());
        Log.d(TAG, "setLanguage result for " + language.name() + ": " + result);

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Language " + language.name() + " is NOT supported on this device. Result: " + result);

            if (language == Language.MARATHI) {
                showToast(appContext.getString(R.string.tts_marathi_missing));
                promptInstallTtsData();

                // Attempt fallback to Hindi
                Log.d(TAG, "Falling back to Hindi for Marathi request");
                int fallbackResult = tts.setLanguage(Language.HINDI.getLocale());
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Hindi fallback also failed. Using default locale.");
                    tts.setLanguage(Locale.getDefault());
                }
            } else {
                tts.setLanguage(Locale.getDefault());
            }
        }

        Log.d(TAG, "Executing speak() queue_flush for: " + text);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
    }

    private void promptInstallTtsData() {
        Log.d(TAG, "Launching TTS data installation intent");
        Intent installIntent = new Intent();
        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            appContext.startActivity(installIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch TTS installation activity", e);
        }
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(appContext, message, Toast.LENGTH_LONG).show());
    }

    @MainThread
    public void stop() {
        if (tts != null) {
            Log.d(TAG, "Stopping TTS playback");
            tts.stop();
        }
    }

    public void shutdown() {
        Log.d(TAG, "Shutting down TTS engine");
        stop();
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        ready.set(false);
    }

    private void notifySpeakingStarted() {
        mainHandler.post(() -> {
            if (speakCallback != null) {
                speakCallback.onSpeakingStarted();
            }
        });
    }

    private void notifySpeakingFinished() {
        mainHandler.post(() -> {
            if (speakCallback != null) {
                speakCallback.onSpeakingFinished();
            }
        });
    }
}
