package com.arriva.touristguideapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.data.phrasebook.Phrase;
import com.arriva.touristguideapp.data.phrasebook.PhrasebookFirestoreContract;
import com.arriva.touristguideapp.data.phrasebook.PhrasebookPreferences;
import com.arriva.touristguideapp.data.phrasebook.PhrasebookRepository;
import com.arriva.touristguideapp.phrasebook.PhrasebookAdapter;
import com.arriva.touristguideapp.phrasebook.PhrasebookCategoryAdapter;
import com.arriva.touristguideapp.phrasebook.PhrasebookTtsHelper;
import com.google.android.material.button.MaterialButton;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Travel Phrasebook: Firestore-backed phrases with Marathi/Hindi TTS, search, and filters.
 */
public class PhrasebookActivity extends AppCompatActivity {

    private static final long SEARCH_DEBOUNCE_MS = 300L;

    private PhrasebookRepository repository;
    private PhrasebookPreferences preferences;
    private PhrasebookTtsHelper ttsHelper;

    private final List<Phrase> allPhrases = new ArrayList<>();
    private String selectedCategory = PhrasebookFirestoreContract.CATEGORY_ALL;
    private String searchQuery = "";
    private boolean favoritesOnly = false;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;

    private ProgressBar progressBar;
    private View emptyState;
    private View errorState;
    private TextView tvErrorMessage;
    private TextView tvOriginBadge;
    private RecyclerView rvPhrases;
    private RecyclerView rvCategories;
    private EditText etSearch;
    private SwitchCompat switchFavorites;
    private MaterialButton btnRetry;

    private PhrasebookAdapter phraseAdapter;
    private PhrasebookCategoryAdapter categoryAdapter;

    @Nullable
    private String speakingPhraseId;
    @Nullable
    private PhrasebookTtsHelper.Language speakingLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phrasebook);

        repository = new PhrasebookRepository(this);
        preferences = new PhrasebookPreferences(this);
        ttsHelper = new PhrasebookTtsHelper(this);
        ttsHelper.setSpeakCallback(new PhrasebookTtsHelper.SpeakCallback() {
            @Override
            public void onSpeakingStarted() {
                phraseAdapter.setSpeakingState(speakingPhraseId, speakingLanguage);
            }

            @Override
            public void onSpeakingFinished() {
                speakingPhraseId = null;
                speakingLanguage = null;
                phraseAdapter.setSpeakingState(null, null);
            }
        });

        bindViews();
        showLoading();
        setupCategoryChips();
        setupPhraseList();
        setupSearch();
        setupFavoritesToggle();

        repository.startListening(new PhrasebookRepository.PhrasesListener() {
            @Override
            public void onPhrasesLoaded(@NonNull List<Phrase> phrases, @NonNull PhrasebookRepository.DataOrigin origin) {
                runOnUiThread(() -> handlePhrasesLoaded(phrases, origin));
            }

            @Override
            public void onError(@NonNull String message) {
                runOnUiThread(() -> {
                    if (allPhrases.isEmpty()) {
                        showError(message);
                    }
                });
            }
        });
    }

    private void bindViews() {
        ImageButton btnBack = findViewById(R.id.btnPhrasebookBack);
        progressBar = findViewById(R.id.phrasebookProgress);
        emptyState = findViewById(R.id.phrasebookEmptyState);
        errorState = findViewById(R.id.phrasebookErrorState);
        tvErrorMessage = findViewById(R.id.tvPhrasebookError);
        tvOriginBadge = findViewById(R.id.tvPhrasebookOrigin);
        rvPhrases = findViewById(R.id.rvPhrases);
        rvCategories = findViewById(R.id.rvPhraseCategories);
        etSearch = findViewById(R.id.etPhraseSearch);
        switchFavorites = findViewById(R.id.switchFavoritesOnly);
        btnRetry = findViewById(R.id.btnPhrasebookRetry);
        ImageButton btnReplyTranslator = findViewById(R.id.btnReplyTranslator);

        btnBack.setOnClickListener(v -> finish());
        btnReplyTranslator.setOnClickListener(v -> {
            startActivity(new Intent(this, ReplyTranslatorActivity.class));
        });
        btnRetry.setOnClickListener(v -> {
            showLoading();
            repository.stopListening();
            repository.startListening(new PhrasebookRepository.PhrasesListener() {
                @Override
                public void onPhrasesLoaded(@NonNull List<Phrase> phrases, @NonNull PhrasebookRepository.DataOrigin origin) {
                    runOnUiThread(() -> handlePhrasesLoaded(phrases, origin));
                }

                @Override
                public void onError(@NonNull String message) {
                    runOnUiThread(() -> {
                        if (allPhrases.isEmpty()) {
                            showError(message);
                        }
                    });
                }
            });
        });
    }

    private void setupCategoryChips() {
        List<String> chips = new ArrayList<>();
        chips.add(PhrasebookFirestoreContract.CATEGORY_ALL);
        chips.addAll(Arrays.asList(PhrasebookFirestoreContract.CATEGORIES));

        rvCategories.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new PhrasebookCategoryAdapter(category -> {
            selectedCategory = category;
            applyFilters();
        });
        categoryAdapter.setCategories(chips, selectedCategory);
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupPhraseList() {
        rvPhrases.setLayoutManager(new LinearLayoutManager(this));
        rvPhrases.setHasFixedSize(true);
        phraseAdapter = new PhrasebookAdapter(preferences, new PhrasebookAdapter.PhraseActionListener() {
            @Override
            public void onSpeakMarathi(@NonNull Phrase phrase) {
                playPhrase(phrase, PhrasebookTtsHelper.Language.MARATHI, phrase.getMarathiText());
            }

            @Override
            public void onSpeakHindi(@NonNull Phrase phrase) {
                playPhrase(phrase, PhrasebookTtsHelper.Language.HINDI, phrase.getHindiText());
            }

            @Override
            public void onFavoriteToggled(@NonNull Phrase phrase, boolean isFavorite) {
                if (favoritesOnly) {
                    applyFilters();
                } else {
                    phraseAdapter.notifyDataSetChanged();
                }
            }
        });
        rvPhrases.setAdapter(phraseAdapter);
    }

    private void playPhrase(@NonNull Phrase phrase,
                            @NonNull PhrasebookTtsHelper.Language language,
                            @NonNull String text) {
        ttsHelper.stop();
        speakingPhraseId = phrase.getId();
        speakingLanguage = language;
        preferences.recordPlayed(phrase.getId());
        phraseAdapter.setSpeakingState(speakingPhraseId, speakingLanguage);
        ttsHelper.speak(text, language);
    }

    private void setupSearch() {
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
                    applyFilters();
                };
                debounceHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFavoritesToggle() {
        switchFavorites.setOnCheckedChangeListener((buttonView, isChecked) -> {
            favoritesOnly = isChecked;
            applyFilters();
        });
    }

    private void handlePhrasesLoaded(@NonNull List<Phrase> phrases,
                                    @NonNull PhrasebookRepository.DataOrigin origin) {
        allPhrases.clear();
        allPhrases.addAll(phrases);
        progressBar.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);

        String originLabel;
        switch (origin) {
            case FIRESTORE:
                originLabel = getString(R.string.phrasebook_origin_firestore);
                break;
            case OFFLINE_CACHE:
                originLabel = getString(R.string.phrasebook_origin_cache);
                break;
            default:
                originLabel = getString(R.string.phrasebook_origin_local);
                break;
        }
        tvOriginBadge.setText(originLabel);
        tvOriginBadge.setVisibility(View.VISIBLE);

        applyFilters();
    }

    private void applyFilters() {
        Set<String> favoriteIds = preferences.getFavoriteIds();
        List<Phrase> filtered = PhrasebookRepository.filter(
                allPhrases, selectedCategory, searchQuery, favoritesOnly, favoriteIds);

        if (!searchQuery.trim().isEmpty() || favoritesOnly) {
            phraseAdapter.submitList(filtered);
        } else {
            phraseAdapter.submitList(orderWithRecent(filtered));
        }

        boolean empty = filtered.isEmpty();
        rvPhrases.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private List<Phrase> orderWithRecent(@NonNull List<Phrase> phrases) {
        List<String> recentIds = preferences.getRecentIds();
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

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
        rvPhrases.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void showError(@NonNull String message) {
        progressBar.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        rvPhrases.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        tvErrorMessage.setText(getString(R.string.phrasebook_error_generic, message));
    }

    @Override
    protected void onDestroy() {
        if (pendingSearchRunnable != null) {
            debounceHandler.removeCallbacks(pendingSearchRunnable);
        }
        repository.stopListening();
        ttsHelper.shutdown();
        super.onDestroy();
    }
}
