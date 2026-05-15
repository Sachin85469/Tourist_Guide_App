package com.arriva.touristguideapp.communication.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.arriva.touristguideapp.communication.languages.LanguageRegistry;
import com.arriva.touristguideapp.communication.translation.TranslationCallback;
import com.arriva.touristguideapp.communication.translation.TranslationCache;
import com.arriva.touristguideapp.communication.translation.TranslationManager;
import com.arriva.touristguideapp.data.phrasebook.Phrase;
import com.arriva.touristguideapp.data.phrasebook.PhrasebookFirestoreContract;
import com.arriva.touristguideapp.data.phrasebook.PhrasebookRepository;
import com.arriva.touristguideapp.phrasebook.PhrasebookCategoryAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PhrasebookFragment extends Fragment {

    private static final long SEARCH_DEBOUNCE_MS = 300L;

    private CommunicationHost host;
    private PhrasebookRepository repository;
    private TranslationManager translationManager;
    private TranslationCache translationCache;
    private CommunicationPreferences preferences;

    private final List<Phrase> allPhrases = new ArrayList<>();
    private final List<PhraseDisplayItem> displayItems = new ArrayList<>();
    private String selectedCategory = PhrasebookFirestoreContract.CATEGORY_ALL;
    private String searchQuery = "";
    private boolean favoritesOnly;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;

    private PhrasebookListAdapter listAdapter;
    private PhrasebookCategoryAdapter categoryAdapter;
    private LanguageSelectorHelper languageSelectorHelper;

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
        translationManager = host.getTranslationManager();
        translationCache = translationManager.getCache();
        preferences = host.getCommunicationPreferences();

        ProgressBar progressBar = view.findViewById(R.id.phrasebookProgress);
        View emptyState = view.findViewById(R.id.phrasebookEmptyState);
        View errorState = view.findViewById(R.id.phrasebookErrorState);
        TextView tvError = view.findViewById(R.id.tvPhrasebookError);
        MaterialButton btnRetry = view.findViewById(R.id.btnPhrasebookRetry);
        RecyclerView rvPhrases = view.findViewById(R.id.rvPhrases);
        RecyclerView rvCategories = view.findViewById(R.id.rvPhraseCategories);
        EditText etSearch = view.findViewById(R.id.etPhraseSearch);
        SwitchCompat switchFavorites = view.findViewById(R.id.switchFavoritesOnly);
        View langPanel = view.findViewById(R.id.languageSelector);
        Spinner spinnerSource = langPanel.findViewById(R.id.spinnerSourceLanguage);
        Spinner spinnerTarget = langPanel.findViewById(R.id.spinnerTargetLanguage);
        MaterialButton btnSwap = langPanel.findViewById(R.id.btnSwapLanguages);

        listAdapter = new PhrasebookListAdapter(preferences, new PhrasebookListAdapter.PhraseActionListener() {
            @Override
            public void onSpeak(@NonNull Phrase phrase, @NonNull String textToSpeak) {
                speakingPhraseId = phrase.getId();
                listAdapter.setSpeakingPhraseId(speakingPhraseId);
                preferences.recordPhrasePlayed(phrase.getId());
                host.getTtsHelper().speak(textToSpeak, host.getTargetLanguage());
            }

            @Override
            public void onFavoriteToggled() {
                applyFiltersAndTranslate();
            }
        });
        rvPhrases.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPhrases.setAdapter(listAdapter);

        setupCategories(rvCategories);
        languageSelectorHelper = new LanguageSelectorHelper(host, this::onLanguagePairChanged);
        languageSelectorHelper.bind(spinnerSource, spinnerTarget, btnSwap);

        if (host instanceof com.arriva.touristguideapp.communication.CommunicationHubActivity) {
            ((com.arriva.touristguideapp.communication.CommunicationHubActivity) host)
                    .addLanguageChangeListener(this::onLanguagePairChanged);
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearchRunnable != null) {
                    debounceHandler.removeCallbacks(pendingSearchRunnable);
                }
                pendingSearchRunnable = () -> {
                    searchQuery = s != null ? s.toString() : "";
                    applyFiltersAndTranslate();
                };
                debounceHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        switchFavorites.setOnCheckedChangeListener((button, checked) -> {
            favoritesOnly = checked;
            applyFiltersAndTranslate();
        });

        btnRetry.setOnClickListener(v -> loadPhrases(progressBar, errorState, tvError));

        host.getTtsHelper().setSpeakCallback(new com.arriva.touristguideapp.communication.tts.UniversalTtsHelper.SpeakCallback() {
            @Override
            public void onSpeakingStarted() {
            }

            @Override
            public void onSpeakingFinished(@Nullable String errorMessage) {
                speakingPhraseId = null;
                if (listAdapter != null) {
                    listAdapter.setSpeakingPhraseId(null);
                }
            }
        });

        loadPhrases(progressBar, errorState, tvError);
    }

    private void setupCategories(@NonNull RecyclerView rvCategories) {
        List<String> chips = new ArrayList<>();
        chips.add(PhrasebookFirestoreContract.CATEGORY_ALL);
        chips.addAll(Arrays.asList(PhrasebookFirestoreContract.CATEGORIES));
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new PhrasebookCategoryAdapter(category -> {
            selectedCategory = category;
            applyFiltersAndTranslate();
        });
        categoryAdapter.setCategories(chips, selectedCategory);
        rvCategories.setAdapter(categoryAdapter);
    }

    private void loadPhrases(@NonNull ProgressBar progressBar,
                             @NonNull View errorState,
                             @NonNull TextView tvError) {
        progressBar.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
        repository.startListening(new PhrasebookRepository.PhrasesListener() {
            @Override
            public void onPhrasesLoaded(@NonNull List<Phrase> phrases, @NonNull PhrasebookRepository.DataOrigin origin) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    allPhrases.clear();
                    allPhrases.addAll(phrases);
                    progressBar.setVisibility(View.GONE);
                    applyFiltersAndTranslate();
                });
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (allPhrases.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        errorState.setVisibility(View.VISIBLE);
                        tvError.setText(getString(R.string.phrasebook_error_generic, message));
                    }
                });
            }
        });
    }

    private void onLanguagePairChanged() {
        applyFiltersAndTranslate();
    }

    private void applyFiltersAndTranslate() {
        if (!isAdded()) {
            return;
        }
        Set<String> favoriteIds = preferences.getFavoriteIds();
        List<Phrase> filtered = PhrasebookRepository.filter(
                allPhrases,
                selectedCategory,
                searchQuery,
                favoritesOnly,
                favoriteIds,
                translationCache,
                host.getTargetLanguage().getLanguageCode());

        if (!searchQuery.trim().isEmpty() || favoritesOnly) {
            rebuildDisplayItems(filtered);
        } else {
            rebuildDisplayItems(orderWithRecent(filtered));
        }

        listAdapter.submitItems(displayItems);
        View emptyState = requireView().findViewById(R.id.phrasebookEmptyState);
        RecyclerView rvPhrases = requireView().findViewById(R.id.rvPhrases);
        boolean empty = displayItems.isEmpty();
        rvPhrases.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);

        translateAllVisible();
    }

    private void rebuildDisplayItems(@NonNull List<Phrase> phrases) {
        displayItems.clear();
        for (Phrase phrase : phrases) {
            displayItems.add(new PhraseDisplayItem(phrase));
        }
    }

    private void translateAllVisible() {
        LanguageConfig target = host.getTargetLanguage();
        for (int i = 0; i < displayItems.size(); i++) {
            PhraseDisplayItem item = displayItems.get(i);
            Phrase phrase = item.getPhrase();
            LanguageConfig source = LanguageRegistry.requireFromCode(phrase.getBaseLanguage());

            if (source.getLanguageCode().equals(target.getLanguageCode())) {
                item.setTranslatedText(phrase.getBaseText());
                item.setState(PhraseDisplayItem.TranslationUiState.READY);
                listAdapter.notifyItemChanged(i);
                continue;
            }

            String cacheKey = translationCache.phraseKey(
                    phrase.getId(), source.getLanguageCode(), target.getLanguageCode());
            String cached = translationCache.get(cacheKey);
            if (cached != null) {
                item.setTranslatedText(cached);
                item.setState(PhraseDisplayItem.TranslationUiState.READY);
                listAdapter.notifyItemChanged(i);
                continue;
            }

            item.setState(PhraseDisplayItem.TranslationUiState.LOADING);
            listAdapter.notifyItemChanged(i);
            final int index = i;
            translationManager.translate(
                    phrase.getBaseText(),
                    source,
                    target,
                    cacheKey,
                    new TranslationCallback() {
                        @Override
                        public void onSuccess(@NonNull String translatedText) {
                            if (!isAdded() || index >= displayItems.size()) {
                                return;
                            }
                            PhraseDisplayItem current = displayItems.get(index);
                            if (!current.getPhrase().getId().equals(phrase.getId())) {
                                return;
                            }
                            current.setTranslatedText(translatedText);
                            current.setState(PhraseDisplayItem.TranslationUiState.READY);
                            requireActivity().runOnUiThread(() -> listAdapter.notifyItemChanged(index));
                        }

                        @Override
                        public void onProgress(@NonNull String message) {
                        }

                        @Override
                        public void onFailure(@NonNull String errorMessage) {
                            if (!isAdded() || index >= displayItems.size()) {
                                return;
                            }
                            PhraseDisplayItem current = displayItems.get(index);
                            current.setState(PhraseDisplayItem.TranslationUiState.ERROR);
                            current.setErrorMessage(errorMessage);
                            requireActivity().runOnUiThread(() -> listAdapter.notifyItemChanged(index));
                        }
                    });
        }
    }

    @NonNull
    private List<Phrase> orderWithRecent(@NonNull List<Phrase> phrases) {
        List<String> recentIds = preferences.getRecentPhraseIds();
        if (recentIds.isEmpty()) {
            return phrases;
        }
        Map<String, Phrase> byId = new LinkedHashMap<>();
        for (Phrase phrase : phrases) {
            byId.put(phrase.getId(), phrase);
        }
        List<Phrase> ordered = new ArrayList<>();
        for (String id : recentIds) {
            Phrase recent = byId.remove(id);
            if (recent != null) {
                ordered.add(recent);
            }
        }
        ordered.addAll(byId.values());
        return ordered;
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
