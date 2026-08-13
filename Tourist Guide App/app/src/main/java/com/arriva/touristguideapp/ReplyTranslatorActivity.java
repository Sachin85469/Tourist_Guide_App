package com.arriva.touristguideapp;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.arriva.touristguideapp.communication.languages.LanguageRegistry;
import com.arriva.touristguideapp.communication.tts.UniversalTtsHelper;
import com.arriva.touristguideapp.translator.SpeechRecognitionHelper;
import com.arriva.touristguideapp.translator.TranslationManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.mlkit.nl.translate.TranslateLanguage;

public class ReplyTranslatorActivity extends BaseActivity {

    private static final String TAG = "ReplyTranslatorActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private SpeechRecognitionHelper speechHelper;
    private TranslationManager translationManager;
    private UniversalTtsHelper ttsHelper;

    private MaterialButton btnSourceLang, btnTargetLang, btnVoiceInput, btnTranslate, btnTts, btnCopy, btnRetryDownload;
    private ImageButton btnSwapLang;
    private EditText etInput;
    private TextView tvStatus, tvTranslatedResult;
    private LinearLayout layoutStatus;
    private ProgressBar downloadProgress;
    private MaterialCardView cardResult;

    private String sourceLangCode = TranslateLanguage.ENGLISH;
    private String targetLangCode = TranslateLanguage.MARATHI;
    private boolean isListening = false;

    private final String[] languages = {"English", "Marathi", "Hindi"};
    private final String[] mlKitCodes = {TranslateLanguage.ENGLISH, TranslateLanguage.MARATHI, TranslateLanguage.HINDI};
    private final String[] speechCodes = {"en-IN", "mr-IN", "hi-IN"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply_translator);

        speechHelper = new SpeechRecognitionHelper(this);
        translationManager = new TranslationManager();
        ttsHelper = new UniversalTtsHelper(this);

        initViews();
        setupListeners();
        updateLanguageButtons();
        checkModelAvailability();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        btnSourceLang = findViewById(R.id.btnSourceLang);
        btnTargetLang = findViewById(R.id.btnTargetLang);
        btnSwapLang = findViewById(R.id.btnSwapLang);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        btnTranslate = findViewById(R.id.btnTranslate);
        btnTts = findViewById(R.id.btnTts);
        btnCopy = findViewById(R.id.btnCopy);
        btnRetryDownload = findViewById(R.id.btnRetryDownload);
        
        etInput = findViewById(R.id.etInput);
        tvStatus = findViewById(R.id.tvStatus);
        tvTranslatedResult = findViewById(R.id.tvTranslatedResult);
        layoutStatus = findViewById(R.id.layoutStatus);
        downloadProgress = findViewById(R.id.downloadProgress);
        cardResult = findViewById(R.id.cardResult);
    }

    private void setupListeners() {
        btnSourceLang.setOnClickListener(v -> showLanguageDialog(true));
        btnTargetLang.setOnClickListener(v -> showLanguageDialog(false));
        
        btnSwapLang.setOnClickListener(v -> {
            String temp = sourceLangCode;
            sourceLangCode = targetLangCode;
            targetLangCode = temp;
            updateLanguageButtons();
            checkModelAvailability();
        });

        btnTranslate.setOnClickListener(v -> performTranslation());

        btnVoiceInput.setOnClickListener(v -> {
            if (isListening) {
                stopListening();
            } else {
                startListening();
            }
        });

        btnTts.setOnClickListener(v -> {
            String text = tvTranslatedResult.getText().toString();
            if (!text.isEmpty()) {
                ttsHelper.speak(text, languageFromMlKit(targetLangCode));
            }
        });

        btnCopy.setOnClickListener(v -> {
            String text = tvTranslatedResult.getText().toString();
            if (!text.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("translation", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        btnRetryDownload.setOnClickListener(v -> downloadModels());
    }

    private void showLanguageDialog(boolean isSource) {
        new AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(languages, (dialog, which) -> {
                    String selected = mlKitCodes[which];
                    if (isSource) {
                        if (selected.equals(targetLangCode)) {
                            Toast.makeText(this, "Source and Target cannot be same", Toast.LENGTH_SHORT).show();
                        } else {
                            sourceLangCode = selected;
                        }
                    } else {
                        if (selected.equals(sourceLangCode)) {
                            Toast.makeText(this, "Source and Target cannot be same", Toast.LENGTH_SHORT).show();
                        } else {
                            targetLangCode = selected;
                        }
                    }
                    updateLanguageButtons();
                    checkModelAvailability();
                })
                .show();
    }

    private void updateLanguageButtons() {
        btnSourceLang.setText(getLanguageName(sourceLangCode));
        btnTargetLang.setText(getLanguageName(targetLangCode));
    }

    private String getLanguageName(String code) {
        for (int i = 0; i < mlKitCodes.length; i++) {
            if (mlKitCodes[i].equals(code)) return languages[i];
        }
        return "English";
    }

    private void checkModelAvailability() {
        translationManager.checkModelStatus(sourceLangCode, status -> {
            if (status == TranslationManager.ModelStatus.READY) {
                translationManager.checkModelStatus(targetLangCode, status2 -> {
                    updateStatusUI(status2);
                });
            } else {
                updateStatusUI(status);
            }
        });
    }

    private void updateStatusUI(TranslationManager.ModelStatus status) {
        runOnUiThread(() -> {
            switch (status) {
                case READY:
                    layoutStatus.setVisibility(View.GONE);
                    btnTranslate.setEnabled(true);
                    btnVoiceInput.setEnabled(true);
                    break;
                case DOWNLOADING:
                    layoutStatus.setVisibility(View.VISIBLE);
                    tvStatus.setText("Downloading translation model...");
                    downloadProgress.setIndeterminate(true);
                    btnTranslate.setEnabled(false);
                    btnVoiceInput.setEnabled(false);
                    btnRetryDownload.setVisibility(View.GONE);
                    break;
                case NOT_DOWNLOADED:
                    layoutStatus.setVisibility(View.VISIBLE);
                    tvStatus.setText("Model not downloaded. Offline translation unavailable.");
                    downloadProgress.setIndeterminate(false);
                    btnTranslate.setEnabled(true); // Will trigger download on click
                    btnVoiceInput.setEnabled(true);
                    btnRetryDownload.setVisibility(View.VISIBLE);
                    btnRetryDownload.setText("Download Now");
                    break;
                case FAILED:
                    layoutStatus.setVisibility(View.VISIBLE);
                    tvStatus.setText("Model download failed.");
                    btnRetryDownload.setVisibility(View.VISIBLE);
                    btnRetryDownload.setText("Retry");
                    break;
            }
        });
    }

    private void downloadModels() {
        translationManager.downloadModels(sourceLangCode, targetLangCode, new TranslationManager.ModelDownloadCallback() {
            @Override
            public void onStatusChanged(TranslationManager.ModelStatus status) {
                updateStatusUI(status);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Download error", e);
            }
        });
    }

    private void performTranslation() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) return;

        layoutStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Translating...");
        btnTranslate.setEnabled(false);

        translationManager.translate(input, sourceLangCode, targetLangCode, new TranslationManager.TranslationCallback() {
            @Override
            public void onSuccess(String translatedText) {
                runOnUiThread(() -> {
                    layoutStatus.setVisibility(View.GONE);
                    btnTranslate.setEnabled(true);
                    cardResult.setVisibility(View.VISIBLE);
                    tvTranslatedResult.setText(translatedText);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    layoutStatus.setVisibility(View.VISIBLE);
                    tvStatus.setText("Translation failed.");
                    btnTranslate.setEnabled(true);
                    Toast.makeText(ReplyTranslatorActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        isListening = true;
        btnVoiceInput.setIconResource(android.R.drawable.ic_media_pause);
        btnVoiceInput.setText("Listening...");
        
        speechHelper.startListening(getSpeechCode(sourceLangCode), new SpeechRecognitionHelper.SpeechResultsCallback() {
            @Override
            public void onPartialResults(String text) {
                etInput.setText(text);
            }

            @Override
            public void onResults(String text) {
                runOnUiThread(() -> {
                    isListening = false;
                    btnVoiceInput.setIconResource(android.R.drawable.ic_btn_speak_now);
                    btnVoiceInput.setText("Voice");
                    etInput.setText(text);
                    performTranslation();
                });
            }

            @Override
            public void onError(int error) {
                runOnUiThread(() -> {
                    isListening = false;
                    btnVoiceInput.setIconResource(android.R.drawable.ic_btn_speak_now);
                    btnVoiceInput.setText("Voice");
                    Toast.makeText(ReplyTranslatorActivity.this, "Speech error: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onEndOfSpeech() {
                runOnUiThread(() -> {
                    isListening = false;
                    btnVoiceInput.setIconResource(android.R.drawable.ic_btn_speak_now);
                    btnVoiceInput.setText("Voice");
                });
            }

            @Override
            public void onReadyForSpeech() {
                Log.d(TAG, "Mic ready");
            }
        });
    }

    private void stopListening() {
        speechHelper.stopListening();
        isListening = false;
        btnVoiceInput.setIconResource(android.R.drawable.ic_btn_speak_now);
        btnVoiceInput.setText("Voice");
    }

    private String getSpeechCode(String mlKitCode) {
        for (int i = 0; i < mlKitCodes.length; i++) {
            if (mlKitCodes[i].equals(mlKitCode)) return speechCodes[i];
        }
        return "en-IN";
    }

    @NonNull
    private LanguageConfig languageFromMlKit(@NonNull String mlKitCode) {
        for (LanguageConfig config : LanguageRegistry.getSupportedLanguages()) {
            if (config.getMlKitLanguageCode().equals(mlKitCode)) {
                return config;
            }
        }
        return LanguageRegistry.getSupportedLanguages().get(0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        speechHelper.destroy();
        translationManager.close();
        ttsHelper.shutdown();
        super.onDestroy();
    }
}
