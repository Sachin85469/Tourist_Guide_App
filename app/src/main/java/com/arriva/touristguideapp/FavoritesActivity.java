package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends BaseActivity implements FavoriteAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private List<Place> favoritePlaces = new ArrayList<>();
    private LinearLayout emptyLayout;
    private TextView tvCount;

    private String searchQuery = "";
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerView = findViewById(R.id.rvFavorites);
        emptyLayout = findViewById(R.id.emptyStateFavorites);
        tvCount = findViewById(R.id.tvFavoritesCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        View btnExplore = findViewById(R.id.btnExplore);
        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        setupSearchAndFilters();

        adapter = new FavoriteAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        
        // Initialize Bottom Navigation
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_favorites);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    Intent intent = new Intent(FavoritesActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                } else if (id == R.id.nav_favorites) {
                    return true;
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(FavoritesActivity.this, MapActivity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                }
                return false;
            });
        }
        
        animateEntrance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void setupSearchAndFilters() {
        EditText etSearch = findViewById(R.id.etSearchFavorites);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString();
                    applyFilters();
                }
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        ChipGroup cgFilters = findViewById(R.id.cgCategoryFilters);
        if (cgFilters != null) {
            cgFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipAll) {
                    selectedCategory = "All";
                } else if (checkedId == R.id.chipNature) {
                    selectedCategory = "Nature";
                } else if (checkedId == R.id.chipAdventure) {
                    selectedCategory = "Adventure";
                } else if (checkedId == R.id.chipHistorical) {
                    selectedCategory = "History";
                } else if (checkedId == R.id.chipReligious) {
                    selectedCategory = "Spiritual";
                } else if (checkedId == R.id.chipFood) {
                    selectedCategory = "Food";
                } else if (checkedId == R.id.chipWildlife) {
                    selectedCategory = "Wildlife";
                }
                applyFilters();
            });
        }
    }

    private void animateEntrance() {
        if (recyclerView != null) {
            recyclerView.setAlpha(0f);
            recyclerView.setTranslationY(50f);
            recyclerView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start();
        }
    }

    public void updateCount() {
        loadFromLocalCache();
    }

    private void loadFavorites() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            loadFromLocalCache();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Fetch the complete catalog of places (from Firestore or local fallback)
        new com.arriva.touristguideapp.data.places.PlaceRepository().fetchPublishedPlaces((places, origin, message) -> {
            // 2. Fetch the user's favorites from Firestore
            db.collection("favorites")
                .whereEqualTo("userId", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();
                        
                        // Sort in memory by savedAt descending to prevent index requirement
                        java.util.Collections.sort(docs, new java.util.Comparator<DocumentSnapshot>() {
                            @Override
                            public int compare(DocumentSnapshot d1, DocumentSnapshot d2) {
                                Long t1 = d1.getLong("savedAt");
                                Long t2 = d2.getLong("savedAt");
                                long val1 = t1 != null ? t1 : 0L;
                                long val2 = t2 != null ? t2 : 0L;
                                return Long.compare(val2, val1);
                            }
                        });

                        List<Place> remoteFavorites = new ArrayList<>();
                        android.content.SharedPreferences.Editor editor = getSharedPreferences("favorites", MODE_PRIVATE).edit();
                        
                        // Clear existing favorite keys in preference
                        android.content.SharedPreferences prefs = getSharedPreferences("favorites", MODE_PRIVATE);
                        for (String key : prefs.getAll().keySet()) {
                            if (prefs.getAll().get(key) instanceof Boolean) {
                                editor.remove(key);
                            }
                        }

                        android.util.Log.d("FavoritesActivity", "Total favorites found: " + docs.size());
                        List<String> loadedNames = new ArrayList<>();

                        for (DocumentSnapshot doc : docs) {
                            String destId = doc.getString("destinationId");
                            if (destId != null) {
                                editor.putBoolean(destId, true);
                                
                                Place placeDetails = null;
                                for (Place p : places) {
                                    if (p.getId().equals(destId)) {
                                        placeDetails = p;
                                        break;
                                    }
                                }
                                if (placeDetails != null) {
                                    remoteFavorites.add(placeDetails);
                                    loadedNames.add(placeDetails.getName());
                                } else {
                                    android.util.Log.w("FavoritesActivity", "Favorite details not found in catalog for destId: " + destId);
                                }
                            }
                        }
                        editor.apply();

                        android.util.Log.d("FavoritesActivity", "Favorite destination names loaded: " + loadedNames);

                        favoritePlaces.clear();
                        favoritePlaces.addAll(remoteFavorites);
                        applyFilters();
                    } else {
                        loadFromLocalCache();
                    }
                });
        });
    }

    private void loadFromLocalCache() {
        Set<String> favoriteIds = FavoritesManager.getFavoriteIds(this);
        List<Place> allPlaces = DataProvider.getPlaces();
        favoritePlaces.clear();

        for (Place p : allPlaces) {
            if (favoriteIds.contains(p.getId())) {
                favoritePlaces.add(p);
            }
        }
        applyFilters();
    }

    private void applyFilters() {
        List<Place> filteredList = new ArrayList<>();
        String lowerQuery = searchQuery.toLowerCase().trim();

        for (Place p : favoritePlaces) {
            boolean matchesCategory = selectedCategory.equals("All") ||
                    p.getCategory().equalsIgnoreCase(selectedCategory) ||
                    (selectedCategory.equals("History") && p.getCategory().equalsIgnoreCase("Historical")) ||
                    (selectedCategory.equals("Spiritual") && p.getCategory().equalsIgnoreCase("Religious"));

            boolean matchesSearch = lowerQuery.isEmpty() ||
                    p.getName().toLowerCase().contains(lowerQuery) ||
                    p.getCity().toLowerCase().contains(lowerQuery) ||
                    p.getCategory().toLowerCase().contains(lowerQuery);

            if (matchesCategory && matchesSearch) {
                filteredList.add(p);
            }
        }

        if (tvCount != null) {
            tvCount.setText(filteredList.size() + " items");
        }

        if (adapter != null) {
            adapter.updateList(filteredList);
        }

        if (filteredList.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            TextView tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
            TextView tvEmptyMsg = findViewById(R.id.tvEmptyMsg);
            if (!searchQuery.isEmpty() || !selectedCategory.equals("All")) {
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No results found");
                if (tvEmptyMsg != null) tvEmptyMsg.setText("Try refining your search keyword or selected category.");
            } else {
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No saved destinations yet");
                if (tvEmptyMsg != null) tvEmptyMsg.setText("Explore tourist destinations and tap the heart icon to save them here.");
            }
        } else {
            emptyLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(Place place) {
        Intent intent = new Intent(this, PlaceDetailsActivity.class);
        PlaceIntentExtras.putPlaceDetails(intent, place);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
