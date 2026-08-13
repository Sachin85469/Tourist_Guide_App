package com.arriva.touristguideapp.communication.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.communication.CommunicationHost;
import com.arriva.touristguideapp.communication.CommunicationPreferences;
import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.arriva.touristguideapp.communication.languages.LanguageRegistry;
import com.google.android.material.button.MaterialButton;

/**
 * Binds source/target language spinners from {@link LanguageRegistry} (no hardcoded languages in UI).
 */
public final class LanguageSelectorHelper {

    public interface OnLanguageChangeListener {
        void onLanguagePairChanged();
    }

    private final CommunicationHost host;
    private final CommunicationPreferences preferences;
    private final OnLanguageChangeListener listener;
    private boolean suppressEvents;

    public LanguageSelectorHelper(@NonNull CommunicationHost host,
                                  @NonNull OnLanguageChangeListener listener) {
        this.host = host;
        this.preferences = host.getCommunicationPreferences();
        this.listener = listener;
    }

    public void bind(@NonNull Spinner spinnerSource,
                     @NonNull Spinner spinnerTarget,
                     @NonNull MaterialButton btnSwap) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                spinnerSource.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                LanguageRegistry.getDisplayNames());
        spinnerSource.setAdapter(adapter);
        spinnerTarget.setAdapter(new ArrayAdapter<>(
                spinnerTarget.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                LanguageRegistry.getDisplayNames()));

        suppressEvents = true;
        spinnerSource.setSelection(LanguageRegistry.indexOf(host.getSourceLanguage()));
        spinnerTarget.setSelection(LanguageRegistry.indexOf(host.getTargetLanguage()));
        suppressEvents = false;

        AdapterView.OnItemSelectedListener changeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressEvents) {
                    return;
                }
                LanguageConfig selected = LanguageRegistry.getSupportedLanguages().get(position);
                if (parent.getId() == R.id.spinnerSourceLanguage) {
                    host.setSourceLanguage(selected);
                } else {
                    host.setTargetLanguage(selected);
                }
                preferences.saveLanguagePair(host.getSourceLanguage(), host.getTargetLanguage());
                listener.onLanguagePairChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spinnerSource.setOnItemSelectedListener(changeListener);
        spinnerTarget.setOnItemSelectedListener(changeListener);

        btnSwap.setOnClickListener(v -> {
            host.swapLanguages();
            preferences.saveLanguagePair(host.getSourceLanguage(), host.getTargetLanguage());
            suppressEvents = true;
            spinnerSource.setSelection(LanguageRegistry.indexOf(host.getSourceLanguage()));
            spinnerTarget.setSelection(LanguageRegistry.indexOf(host.getTargetLanguage()));
            suppressEvents = false;
            listener.onLanguagePairChanged();
        });
    }
}
