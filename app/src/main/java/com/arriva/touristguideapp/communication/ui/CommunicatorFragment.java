package com.arriva.touristguideapp.communication.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.communication.CommunicationHost;
import com.arriva.touristguideapp.communication.CommunicationPreferences;
import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.arriva.touristguideapp.communication.languages.LanguageRegistry;
import com.arriva.touristguideapp.communication.translation.TranslationCallback;
import com.arriva.touristguideapp.communication.translation.TranslationManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CommunicatorFragment extends Fragment {

    private CommunicationHost host;
    private TranslationManager translationManager;
    private CommunicationPreferences preferences;

    private EditText etInput;
    private TextView tvOriginal;
    private TextView tvTranslated;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private MaterialButton btnTranslate;
    private MaterialButton btnVoice;
    private MaterialButton btnSpeak;
    private MaterialButton btnCopy;
    private MaterialButton btnSave;
    private MaterialButton btnListen;
    private RecyclerView rvHistory;

    @Override
    public void onAttach(@NonNull Context context) {
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
        return inflater.inflate(R.layout.fragment_communicator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        translationManager = host.getTranslationManager();
        preferences = host.getCommunicationPreferences();

        initViews(view);
        setupLanguageSelector(view);
        setupListeners();

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        refreshHistory();
        updateStatusIdle();

        host.getTtsHelper().setSpeakCallback(new com.arriva.touristguideapp.communication.tts.UniversalTtsHelper.SpeakCallback() {
            @Override
            public void onSpeakingStarted() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> setStatus(getString(R.string.communication_status_speaking)));
                }
            }

            @Override
            public void onSpeakingFinished(@Nullable String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (errorMessage != null) setStatus(errorMessage);
                    else setStatus(getString(R.string.communication_status_ready));
                });
            }
        });
    }

    private void initViews(View view) {
        etInput = view.findViewById(R.id.etCommunicatorInput);
        tvOriginal = view.findViewById(R.id.tvCommunicatorOriginal);
        tvTranslated = view.findViewById(R.id.tvCommunicatorTranslated);
        tvStatus = view.findViewById(R.id.tvCommunicatorStatus);
        progressBar = view.findViewById(R.id.communicatorProgress);
        btnTranslate = view.findViewById(R.id.btnCommunicatorTranslate);
        btnVoice = view.findViewById(R.id.btnCommunicatorVoice);
        btnSpeak = view.findViewById(R.id.btnCommunicatorSpeak);
        btnCopy = view.findViewById(R.id.btnCommunicatorCopy);
        btnSave = view.findViewById(R.id.btnCommunicatorSave);
        btnListen = view.findViewById(R.id.btnCommunicatorListen);
        rvHistory = view.findViewById(R.id.rvCommunicatorHistory);
    }

    private void setupLanguageSelector(View view) {
        View langPanel = view.findViewById(R.id.languageSelector);
        Spinner spinnerSource = langPanel.findViewById(R.id.spinnerSourceLanguage);
        Spinner spinnerTarget = langPanel.findViewById(R.id.spinnerTargetLanguage);
        MaterialButton btnSwap = langPanel.findViewById(R.id.btnSwapLanguages);

        LanguageSelectorHelper languageSelector = new LanguageSelectorHelper(host, this::updateStatusIdle);
        languageSelector.bind(spinnerSource, spinnerTarget, btnSwap);
    }

    private void setupListeners() {
        btnTranslate.setOnClickListener(v -> runManualTranslation());
        btnVoice.setOnClickListener(v -> startVoiceInput());
        btnListen.setOnClickListener(v -> startConversationMode());

        btnSpeak.setOnClickListener(v -> {
            String text = tvTranslated.getText().toString();
            if (!text.isEmpty()) {
                host.getTtsHelper().speak(text, host.getTargetLanguage());
            }
        });

        btnCopy.setOnClickListener(v -> copyToClipboard());
        btnSave.setOnClickListener(v -> saveToFavorites());
    }

    private void runManualTranslation() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            setStatus(getString(R.string.communication_enter_text));
            return;
        }
        translateAndHandleResult(input, false);
    }

    private void startVoiceInput() {
        host.requestSpeechInput(new CommunicationHost.SpeechResultCallback() {
            @Override
            public void onSpeechResult(@NonNull String text) {
                etInput.setText(text);
                translateAndHandleResult(text, false);
            }

            @Override
            public void onSpeechFailed(@NonNull String message) {
                setStatus(message);
            }
        });
    }

    private void startConversationMode() {
        setStatus(getString(R.string.communication_status_listening));
        progressBar.setVisibility(View.VISIBLE);
        host.requestSpeechInput(new CommunicationHost.SpeechResultCallback() {
            @Override
            public void onSpeechResult(@NonNull String text) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    tvOriginal.setText(text);
                    translateAndHandleResult(text, true);
                });
            }

            @Override
            public void onSpeechFailed(@NonNull String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setStatus(message);
                });
            }
        });
    }

    private void translateAndHandleResult(@NonNull String text, boolean autoSpeak) {
        setStatus(getString(R.string.communication_status_translating));
        progressBar.setVisibility(View.VISIBLE);
        
        LanguageConfig source = host.getSourceLanguage();
        LanguageConfig target = host.getTargetLanguage();
        
        String cacheKey = translationManager.getCache().textKey(
                text, source.getLanguageCode(), target.getLanguageCode());

        translationManager.translate(text, source, target, cacheKey, new TranslationCallback() {
            @Override
            public void onSuccess(@NonNull String translatedText) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvOriginal.setText(text);
                    tvTranslated.setText(translatedText);
                    setStatus(getString(R.string.communication_status_ready));
                    
                    preferences.addTranslationHistory(
                            text, translatedText, source.getLanguageCode(), target.getLanguageCode());
                    refreshHistory();

                    if (autoSpeak) {
                        host.getTtsHelper().speak(translatedText, target);
                    }
                });
            }

            @Override
            public void onProgress(@NonNull String message) {
                if (isAdded()) requireActivity().runOnUiThread(() -> setStatus(message));
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setStatus(errorMessage);
                });
            }
        });
    }

    private void copyToClipboard() {
        String text = tvTranslated.getText().toString();
        if (text.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("translation", text));
            Toast.makeText(requireContext(), R.string.phrasebook_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFavorites() {
        // Implementation for saving to favorites if needed, or just a toast for now
        Toast.makeText(requireContext(), "Saved to favorites", Toast.LENGTH_SHORT).show();
    }

    private void updateStatusIdle() {
        setStatus(getString(R.string.communication_status_ready));
    }

    private void setStatus(@NonNull String message) {
        tvStatus.setText(message);
    }

    private void refreshHistory() {
        List<CommunicationPreferences.TranslationHistoryEntry> history = preferences.getTranslationHistory();
        rvHistory.setAdapter(new TranslationHistoryAdapter(history, entry -> {
            etInput.setText(entry.sourceText);
            tvOriginal.setText(entry.sourceText);
            tvTranslated.setText(entry.translatedText);
            
            // Update languages if needed
            LanguageConfig src = LanguageRegistry.fromCode(entry.sourceCode);
            LanguageConfig tgt = LanguageRegistry.fromCode(entry.targetCode);
            if (src != null && tgt != null) {
                host.setSourceLanguage(src);
                host.setTargetLanguage(tgt);
                host.notifyLanguagePairChanged();
            }
        }));
    }
}
