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
import com.arriva.touristguideapp.communication.translation.TranslationCallback;
import com.arriva.touristguideapp.communication.translation.TranslationManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class TranslatorFragment extends Fragment {

    private CommunicationHost host;
    private TranslationManager translationManager;
    private CommunicationPreferences preferences;

    private EditText etInput;
    private TextView tvOutput;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private MaterialButton btnTranslate;
    private MaterialButton btnSpeak;
    private MaterialButton btnCopy;
    private MaterialButton btnVoice;
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
        return inflater.inflate(R.layout.fragment_translator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        translationManager = host.getTranslationManager();
        preferences = host.getCommunicationPreferences();

        View langPanel = view.findViewById(R.id.languageSelector);
        Spinner spinnerSource = langPanel.findViewById(R.id.spinnerSourceLanguage);
        Spinner spinnerTarget = langPanel.findViewById(R.id.spinnerTargetLanguage);
        MaterialButton btnSwap = langPanel.findViewById(R.id.btnSwapLanguages);
        etInput = view.findViewById(R.id.etTranslatorInput);
        tvOutput = view.findViewById(R.id.tvTranslatorOutput);
        tvStatus = view.findViewById(R.id.tvTranslatorStatus);
        progressBar = view.findViewById(R.id.translatorProgress);
        btnTranslate = view.findViewById(R.id.btnTranslate);
        btnSpeak = view.findViewById(R.id.btnSpeakTranslation);
        btnCopy = view.findViewById(R.id.btnCopyTranslation);
        btnVoice = view.findViewById(R.id.btnVoiceInput);
        rvHistory = view.findViewById(R.id.rvTranslationHistory);

        LanguageSelectorHelper languageSelector = new LanguageSelectorHelper(host, this::updateStatusIdle);
        languageSelector.bind(spinnerSource, spinnerTarget, btnSwap);

        btnTranslate.setOnClickListener(v -> runTranslation());
        btnVoice.setOnClickListener(v -> host.requestSpeechInput(new CommunicationHost.SpeechResultCallback() {
            @Override
            public void onSpeechResult(@NonNull String text) {
                etInput.setText(text);
                runTranslation();
            }

            @Override
            public void onSpeechFailed(@NonNull String message) {
                showStatus(message);
            }
        }));
        btnSpeak.setOnClickListener(v -> {
            String text = tvOutput.getText() != null ? tvOutput.getText().toString() : "";
            if (!text.isEmpty()) {
                host.getTtsHelper().speak(text, host.getTargetLanguage());
            }
        });
        btnCopy.setOnClickListener(v -> {
            String text = tvOutput.getText() != null ? tvOutput.getText().toString() : "";
            if (text.isEmpty()) {
                return;
            }
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("translation", text));
                Toast.makeText(requireContext(), R.string.phrasebook_copied, Toast.LENGTH_SHORT).show();
            }
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        refreshHistory();
        updateStatusIdle();
    }

    private void runTranslation() {
        String input = etInput.getText() != null ? etInput.getText().toString().trim() : "";
        if (input.isEmpty()) {
            showStatus(getString(R.string.communication_enter_text));
            return;
        }
        setUiStateTranslating();
        String cacheKey = translationManager.getCache().textKey(
                input,
                host.getSourceLanguage().getLanguageCode(),
                host.getTargetLanguage().getLanguageCode());

        translationManager.translate(
                input,
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
                            tvOutput.setText(translatedText);
                            showStatus(getString(R.string.communication_status_ready));
                            preferences.addTranslationHistory(
                                    input,
                                    translatedText,
                                    host.getSourceLanguage().getLanguageCode(),
                                    host.getTargetLanguage().getLanguageCode());
                            refreshHistory();
                        });
                    }

                    @Override
                    public void onProgress(@NonNull String message) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> showStatus(message));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            showStatus(errorMessage);
                        });
                    }
                });
    }

    private void setUiStateTranslating() {
        progressBar.setVisibility(View.VISIBLE);
        showStatus(getString(R.string.communication_status_translating));
    }

    private void updateStatusIdle() {
        showStatus(getString(R.string.communication_status_ready));
    }

    private void showStatus(@NonNull String message) {
        tvStatus.setText(message);
    }

    private void refreshHistory() {
        List<CommunicationPreferences.TranslationHistoryEntry> history = preferences.getTranslationHistory();
        rvHistory.setAdapter(new TranslationHistoryAdapter(history, entry -> {
            etInput.setText(entry.sourceText);
            tvOutput.setText(entry.translatedText);
        }));
    }
}
