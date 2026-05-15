package com.arriva.touristguideapp.phrasebook;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        HINDI(new Locale("hi", "IN"));

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
        tts = new TextToSpeech(appContext, this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS || tts == null) {
            Log.e(TAG, "TTS init failed status=" + status);
            ready.set(false);
            return;
        }
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
                notifySpeakingFinished();
            }
        });
        ready.set(true);
    }

    public void setSpeakCallback(@Nullable SpeakCallback callback) {
        speakCallback = callback;
    }

    @MainThread
    public void speak(@NonNull String text, @NonNull Language language) {
        if (tts == null || !ready.get()) {
            Log.w(TAG, "speak ignored: TTS not ready");
            return;
        }
        if (text.trim().isEmpty()) {
            return;
        }
        stop();
        int result = tts.setLanguage(language.getLocale());
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: " + language.name() + ", using default");
            tts.setLanguage(Locale.getDefault());
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
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
