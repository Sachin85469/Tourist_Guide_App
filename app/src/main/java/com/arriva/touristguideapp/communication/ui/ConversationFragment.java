package com.arriva.touristguideapp.communication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.communication.CommunicationHost;
import com.arriva.touristguideapp.communication.translation.TranslationCallback;
import com.arriva.touristguideapp.communication.translation.TranslationManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Conversation mode: speak in source language → translate → speak target language.
 */
public class ConversationFragment extends Fragment {

    private CommunicationHost host;
    private TranslationManager translationManager;

    private TextView tvHeard;
    private TextView tvTranslated;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private MaterialCardView cardState;
    private MaterialButton btnListen;
    private MaterialButton btnSpeakAgain;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (!(context instanceof CommunicationHost)) {
            throw new IllegalStateException("Host must implement CommunicationHost");
        }
        host = (CommunicationHost) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        translationManager = host.getTranslationManager();

        View langPanel = view.findViewById(R.id.languageSelector);
        Spinner spinnerSource = langPanel.findViewById(R.id.spinnerSourceLanguage);
        Spinner spinnerTarget = langPanel.findViewById(R.id.spinnerTargetLanguage);
        MaterialButton btnSwap = langPanel.findViewById(R.id.btnSwapLanguages);
        tvHeard = view.findViewById(R.id.tvConversationHeard);
        tvTranslated = view.findViewById(R.id.tvConversationTranslated);
        tvStatus = view.findViewById(R.id.tvConversationStatus);
        progressBar = view.findViewById(R.id.conversationProgress);
        cardState = view.findViewById(R.id.cardConversationState);
        btnListen = view.findViewById(R.id.btnConversationListen);
        btnSpeakAgain = view.findViewById(R.id.btnConversationSpeak);

        LanguageSelectorHelper languageSelector = new LanguageSelectorHelper(host, () -> {
        });
        languageSelector.bind(spinnerSource, spinnerTarget, btnSwap);

        btnListen.setOnClickListener(v -> startListening());
        btnSpeakAgain.setOnClickListener(v -> {
            String translated = tvTranslated.getText() != null ? tvTranslated.getText().toString() : "";
            if (!translated.isEmpty()) {
                setStatus(getString(R.string.communication_status_speaking));
                host.getTtsHelper().speak(translated, host.getTargetLanguage());
            }
        });

        host.getTtsHelper().setSpeakCallback(new com.arriva.touristguideapp.communication.tts.UniversalTtsHelper.SpeakCallback() {
            @Override
            public void onSpeakingStarted() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            setStatus(getString(R.string.communication_status_speaking)));
                }
            }

            @Override
            public void onSpeakingFinished(@Nullable String errorMessage) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (errorMessage != null) {
                        setStatus(errorMessage);
                    } else {
                        setStatus(getString(R.string.communication_status_ready));
                    }
                });
            }
        });
    }

    private void startListening() {
        setStatus(getString(R.string.communication_status_listening));
        progressBar.setVisibility(View.VISIBLE);
        host.requestSpeechInput(new CommunicationHost.SpeechResultCallback() {
            @Override
            public void onSpeechResult(@NonNull String text) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    tvHeard.setText(text);
                    translateAndSpeak(text);
                });
            }

            @Override
            public void onSpeechFailed(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setStatus(message);
                });
            }
        });
    }

    private void translateAndSpeak(@NonNull String heardText) {
        setStatus(getString(R.string.communication_status_translating));
        String cacheKey = translationManager.getCache().textKey(
                heardText,
                host.getSourceLanguage().getLanguageCode(),
                host.getTargetLanguage().getLanguageCode());

        translationManager.translate(
                heardText,
                host.getSourceLanguage(),
                host.getTargetLanguage(),
                cacheKey,
                new TranslationCallback() {
                    @Override
                    public void onSuccess(@NonNull String translatedText) {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvTranslated.setText(translatedText);
                            host.getCommunicationPreferences().addTranslationHistory(
                                    heardText,
                                    translatedText,
                                    host.getSourceLanguage().getLanguageCode(),
                                    host.getTargetLanguage().getLanguageCode());
                            host.getTtsHelper().speak(translatedText, host.getTargetLanguage());
                        });
                    }

                    @Override
                    public void onProgress(@NonNull String message) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> setStatus(message));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            setStatus(errorMessage);
                        });
                    }
                });
    }

    private void setStatus(@NonNull String status) {
        tvStatus.setText(status);
    }
}
