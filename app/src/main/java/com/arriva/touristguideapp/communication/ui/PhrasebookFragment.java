package com.arriva.touristguideapp.communication.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.communication.CommunicationHost;
import com.arriva.touristguideapp.communication.CommunicationPreferences;
import com.arriva.touristguideapp.communication.languages.LanguageConfig;
import com.arriva.touristguideapp.data.phrasebook.Phrase;
import com.arriva.touristguideapp.data.phrasebook.PhrasebookFirestoreContract;
import com.arriva.touristguideapp.data.phrasebook.PhrasebookRepository;
import com.arriva.touristguideapp.phrasebook.PhrasebookCategoryAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PhrasebookFragment extends Fragment {

    private static final long SEARCH_DEBOUNCE_MS = 100L;

    private CommunicationHost host;
    private PhrasebookRepository repository;
    private CommunicationPreferences preferences;

    private final List<Phrase> allPhrases = new ArrayList<>();
    private String selectedCategory = PhrasebookFirestoreContract.CATEGORY_ALL;
    private String searchQuery = "";
    private boolean favoritesOnly;
    
    // Unified selectedLanguage variable as requested
    private String selectedLanguage = "Marathi"; 

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;

    private PhrasebookListAdapter listAdapter;
    private PhrasebookCategoryAdapter categoryAdapter;

    @Nullable
    private String speakingPhraseId;

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
        return inflater.inflate(R.layout.fragment_phrasebook, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new PhrasebookRepository(requireContext());
        preferences = host.getCommunicationPreferences();

        initUi(view);
        loadPhrases();
    }

    private void initUi(View view) {
        ProgressBar progressBar = view.findViewById(R.id.phrasebookProgress);
        View errorState = view.findViewById(R.id.phrasebookErrorState);
        MaterialButton btnRetry = view.findViewById(R.id.btnPhrasebookRetry);
        RecyclerView rvPhrases = view.findViewById(R.id.rvPhrases);
        RecyclerView rvCategories = view.findViewById(R.id.rvPhraseCategories);
        EditText etSearch = view.findViewById(R.id.etPhraseSearch);
        SwitchCompat switchFavorites = view.findViewById(R.id.switchFavoritesOnly);
        
        // Setup dedicated Language Selector for Phrasebook
        Spinner spinnerLanguage = view.findViewById(R.id.spinnerPhrasebookLanguage);
        setupLanguageSpinner(spinnerLanguage);

        // Unified list adapter with instant switching logic
        listAdapter = new PhrasebookListAdapter(preferences, new PhrasebookListAdapter.PhraseActionListener() {
            @Override
            public void onSpeak(@NonNull Phrase phrase, @NonNull String textToSpeak, @NonNull String language) {
                speakingPhraseId = phrase.getId();
                listAdapter.setSpeakingPhraseId(speakingPhraseId);
                preferences.recordPhrasePlayed(phrase.getId());
                
                // Unified TTS logic as requested
                Locale locale;
                if (language.equals("Hindi")) {
                    locale = new Locale("hi");
                } else {
                    locale = new Locale("mr");
                }
                
                // Dummy config for TTS helper (it only uses the locale part here)
                LanguageConfig ttsLang = new LanguageConfig(language, "", "", "", locale, "");
                host.getTtsHelper().speak(textToSpeak, ttsLang);
            }

            @Override
            public void onFavoriteToggled() {
                applyFilters();
            }
        });
        rvPhrases.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPhrases.setAdapter(listAdapter);

        setupCategories(rvCategories);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearchRunnable != null) {
                    debounceHandler.removeCallbacks(pendingSearchRunnable);
                }
                pendingSearchRunnable = () -> {
                    searchQuery = s != null ? s.toString() : "";
                    applyFilters();
                };
                debounceHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        switchFavorites.setOnCheckedChangeListener((button, checked) -> {
            favoritesOnly = checked;
            applyFilters();
        });

        btnRetry.setOnClickListener(v -> loadPhrases());

        host.getTtsHelper().setSpeakCallback(new com.arriva.touristguideapp.communication.tts.UniversalTtsHelper.SpeakCallback() {
            @Override
            public void onSpeakingStarted() {}
            @Override
            public void onSpeakingFinished(@Nullable String errorMessage) {
                speakingPhraseId = null;
                if (listAdapter != null) {
                    listAdapter.setSpeakingPhraseId(null);
                }
            }
        });
    }

    private void setupLanguageSpinner(Spinner spinner) {
        if (spinner == null) return;
        String[] languages = {"Marathi", "Hindi"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        // Default: Marathi
        spinner.setSelection(0); 

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Instantly update selectedLanguage and refresh UI
                selectedLanguage = languages[position];
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCategories(@NonNull RecyclerView rvCategories) {
        List<String> chips = new ArrayList<>();
        chips.add(PhrasebookFirestoreContract.CATEGORY_ALL);
        chips.addAll(Arrays.asList(PhrasebookFirestoreContract.CATEGORIES));
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new PhrasebookCategoryAdapter(category -> {
            selectedCategory = category;
            applyFilters();
        });
        categoryAdapter.setCategories(chips, selectedCategory);
        rvCategories.setAdapter(categoryAdapter);
    }

    private void loadPhrases() {
        View progressBar = requireView().findViewById(R.id.phrasebookProgress);
        View errorState = requireView().findViewById(R.id.phrasebookErrorState);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (errorState != null) errorState.setVisibility(View.GONE);

        repository.startListening(new PhrasebookRepository.PhrasesListener() {
            @Override
            public void onPhrasesLoaded(@NonNull List<Phrase> phrases, @NonNull PhrasebookRepository.DataOrigin origin) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    allPhrases.clear();
                    allPhrases.addAll(phrases);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    applyFilters();
                });
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (allPhrases.isEmpty()) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (errorState != null) errorState.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void applyFilters() {
        if (!isAdded()) return;
        Set<String> favoriteIds = preferences.getFavoriteIds();
        List<Phrase> filtered = PhrasebookRepository.filter(
                allPhrases,
                selectedCategory,
                searchQuery,
                favoritesOnly,
                favoriteIds);

        // Instant refresh using selectedLanguage
        listAdapter.submitItems(filtered, selectedLanguage);
        
        RecyclerView rvPhrases = requireView().findViewById(R.id.rvPhrases);
        if (rvPhrases != null) {
            rvPhrases.scheduleLayoutAnimation();
            rvPhrases.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        }

        View emptyState = requireView().findViewById(R.id.phrasebookEmptyState);
        if (emptyState != null) {
            emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        if (pendingSearchRunnable != null) {
            debounceHandler.removeCallbacks(pendingSearchRunnable);
        }
        repository.stopListening();
        super.onDestroyView();
    }
}
