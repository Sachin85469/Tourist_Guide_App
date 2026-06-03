package com.arriva.touristguideapp.communication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.arriva.touristguideapp.communication.speech.UniversalSpeechHelper;
import com.arriva.touristguideapp.communication.translation.LanguageSetupManager;
import com.arriva.touristguideapp.communication.translation.TranslationManager;
import com.arriva.touristguideapp.communication.tts.UniversalTtsHelper;
import com.arriva.touristguideapp.communication.ui.CommunicationPagerAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Multilingual communication hub: Phrasebook, Translator, and Conversation mode.
 */
public class CommunicationHubActivity extends AppCompatActivity implements CommunicationHost {

    public static final String EXTRA_INITIAL_TAB = "initial_tab";

    private CommunicationPreferences preferences;
    private TranslationManager translationManager;
    private UniversalTtsHelper ttsHelper;
    private LanguageSetupManager setupManager;

    private LanguageConfig sourceLanguage;
    private LanguageConfig targetLanguage;

    private final List<Runnable> languageChangeListeners = new ArrayList<>();
    @Nullable
    private SpeechResultCallback pendingSpeechCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication_hub);

        preferences = new CommunicationPreferences(this);
        translationManager = TranslationManager.getInstance(this);
        ttsHelper = new UniversalTtsHelper(this);
        setupManager = new LanguageSetupManager(this);

        sourceLanguage = preferences.getSourceLanguage();
        targetLanguage = preferences.getTargetLanguage();

        ImageButton btnBack = findViewById(R.id.btnCommunicationBack);
        btnBack.setOnClickListener(v -> finish());

        TabLayout tabLayout = findViewById(R.id.tabCommunication);
        ViewPager2 viewPager = findViewById(R.id.pagerCommunication);
        viewPager.setAdapter(new CommunicationPagerAdapter(this));
        viewPager.setOffscreenPageLimit(2);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 1) {
                tab.setText(R.string.communication_tab_communicator);
            } else {
                tab.setText(R.string.communication_tab_phrasebook);
            }
        }).attach();

        int initialTab = getIntent().getIntExtra(EXTRA_INITIAL_TAB, 0);
        if (initialTab == 1) {
            viewPager.setCurrentItem(initialTab, false);
        }

        if (!setupManager.isSetupComplete()) {
            showSetupDialog();
        }
    }

    private void showSetupDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.setup_title);
        builder.setMessage(R.string.setup_message);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.setup_btn_download, (dialog, which) -> {
            startLanguageSetup();
        });
        builder.setNegativeButton(R.string.back, (dialog, which) -> finish());
        builder.show();
    }

    private void startLanguageSetup() {
        View setupView = LayoutInflater.from(this).inflate(R.layout.dialog_language_setup, null);
        ProgressBar progressBar = setupView.findViewById(R.id.setupProgress);
        TextView tvStatus = setupView.findViewById(R.id.tvSetupStatus);
        TextView tvPercentage = setupView.findViewById(R.id.tvSetupPercentage);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(setupView)
                .setCancelable(false)
                .show();

        setupManager.startSetup(new LanguageSetupManager.SetupCallback() {
            @Override
            public void onProgress(int percentage, String message) {
                runOnUiThread(() -> {
                    progressBar.setProgress(percentage);
                    tvPercentage.setText(percentage + "%");
                    tvStatus.setText(message);
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    tvStatus.setText(R.string.setup_complete);
                    dialog.setCancelable(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.setup_btn_finish);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> dialog.dismiss());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    tvStatus.setText(message);
                    dialog.setCancelable(true);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.setup_btn_retry);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        dialog.dismiss();
                        startLanguageSetup();
                    });
                });
            }
        });
        
        // Hide positive button initially
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
    }

    @Override
    @NonNull
    public LanguageConfig getSourceLanguage() {
        return sourceLanguage;
    }

    @Override
    @NonNull
    public LanguageConfig getTargetLanguage() {
        return targetLanguage;
    }

    @Override
    public void setSourceLanguage(@NonNull LanguageConfig language) {
        sourceLanguage = language;
    }

    @Override
    public void setTargetLanguage(@NonNull LanguageConfig language) {
        targetLanguage = language;
    }

    @Override
    public void swapLanguages() {
        LanguageConfig temp = sourceLanguage;
        sourceLanguage = targetLanguage;
        targetLanguage = temp;
        preferences.saveLanguagePair(sourceLanguage, targetLanguage);
    }

    @Override
    public void notifyLanguagePairChanged() {
        translationManager.getCache().clearPair(
                sourceLanguage.getLanguageCode(),
                targetLanguage.getLanguageCode());
        for (Runnable listener : languageChangeListeners) {
            listener.run();
        }
    }

    public void addLanguageChangeListener(@NonNull Runnable listener) {
        languageChangeListeners.add(listener);
    }

    @Override
    @NonNull
    public TranslationManager getTranslationManager() {
        return translationManager;
    }

    @Override
    @NonNull
    public UniversalTtsHelper getTtsHelper() {
        return ttsHelper;
    }

    @Override
    @NonNull
    public CommunicationPreferences getCommunicationPreferences() {
        return preferences;
    }

    @Override
    public void requestSpeechInput(@NonNull SpeechResultCallback callback) {
        pendingSpeechCallback = callback;
        UniversalSpeechHelper.startListening(this, sourceLanguage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String text = UniversalSpeechHelper.parseResult(requestCode, resultCode, data);
        if (pendingSpeechCallback == null) {
            return;
        }
        SpeechResultCallback callback = pendingSpeechCallback;
        pendingSpeechCallback = null;
        if (text != null && !text.trim().isEmpty()) {
            callback.onSpeechResult(text.trim());
        } else {
            callback.onSpeechFailed(getString(R.string.communication_speech_failed));
        }
    }

    @Override
    protected void onDestroy() {
        translationManager.closeAll();
        ttsHelper.shutdown();
        super.onDestroy();
    }
}
