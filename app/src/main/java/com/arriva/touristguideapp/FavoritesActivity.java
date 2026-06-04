package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.content.ContextCompat;
import com.arriva.touristguideapp.data.places.PlaceRepository;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends BaseActivity implements FavoriteAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private List<Place> allFavoritePlaces = new ArrayList<>();
    private View emptyLayout, skeletonLayout;
    private TextView tvCount;
    private SwipeRefreshLayout swipeRefresh;
    private String searchQuery = "";
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerView = findViewById(R.id.rvFavorites);
        emptyLayout = findViewById(R.id.emptyStateFavorites);
        skeletonLayout = findViewById(R.id.skeletonFavorites);
        tvCount = findViewById(R.id.tvFavoritesCount);
        swipeRefresh = findViewById(R.id.swipeRefreshFavorites);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        View btnExplore = findViewById(R.id.btnExplore);
        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> {
                Intent intent = new Intent(this, MapActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        setupSearchAndFilters();

        adapter = new FavoriteAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.color_primary));
            swipeRefresh.setOnRefreshListener(this::loadFavorites);
        }

        // Initialize Bottom Navigation
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_favorites);
            nav.setOnItemSelectedListener(item -> {
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
                    startActivity(new Intent(this, MapActivity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                }
                return false;
            });
        }
        
        loadFavorites();
    }

    private void setupSearchAndFilters() {
        EditText etSearch = findViewById(R.id.etSearchFavorites);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString();
                    applyFilters();
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        ChipGroup cg = findViewById(R.id.cgCategoryFilters);
        if (cg != null) {
            cg.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    selectedCategory = "All";
                } else {
                    int id = checkedIds.get(0);
                    if (id == R.id.chipNature) selectedCategory = "Nature";
                    else if (id == R.id.chipAdventure) selectedCategory = "Adventure";
                    else if (id == R.id.chipHistorical) selectedCategory = "Historical";
                    else if (id == R.id.chipReligious) selectedCategory = "Religious";
                    else if (id == R.id.chipFood) selectedCategory = "Food";
                    else if (id == R.id.chipWildlife) selectedCategory = "Wildlife";
                    else selectedCategory = "All";
                }
                applyFilters();
            });
        }
    }

    private void loadFavorites() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        if (allFavoritePlaces.isEmpty() && skeletonLayout != null) {
            skeletonLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        Set<String> favIds = FavoritesManager.getFavoriteIds(this);
        
        new PlaceRepository().fetchPublishedPlaces((places, origin, message) -> {
            allFavoritePlaces.clear();
            for (Place p : places) {
                if (favIds.contains(p.getId())) {
                    allFavoritePlaces.add(p);
                }
            }
            runOnUiThread(() -> {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (skeletonLayout != null) skeletonLayout.setVisibility(View.GONE);
                applyFilters();
            });
        });
    }

    private void applyFilters() {
        List<Place> filtered = new ArrayList<>();
        for (Place p : allFavoritePlaces) {
            boolean matchesSearch = p.getName().toLowerCase().contains(searchQuery.toLowerCase());
            boolean matchesCat = selectedCategory.equals("All") || (p.getCategory() != null && p.getCategory().equalsIgnoreCase(selectedCategory));
            
            if (matchesSearch && matchesCat) {
                filtered.add(p);
            }
        }

        adapter.updateList(filtered);
        
        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
            
            // Handle different empty messages for Search vs No Data
            TextView tvTitle = findViewById(R.id.tvEmptyTitle);
            TextView tvMsg = findViewById(R.id.tvEmptyMsg);
            if (!searchQuery.isEmpty() || !selectedCategory.equals("All")) {
                if (tvTitle != null) tvTitle.setText(R.string.explore_empty_title);
                if (tvMsg != null) tvMsg.setText(R.string.explore_empty_msg);
            } else {
                if (tvTitle != null) tvTitle.setText(R.string.favorites_empty_title);
                if (tvMsg != null) tvMsg.setText(R.string.favorites_empty_msg);
            }
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
        updateCount();
    }

    public void updateCount() {
        if (tvCount != null) {
            int count = adapter.getItemCount();
            tvCount.setText(count + " items saved");
        }
    }

    @Override
    public void onItemClick(Place place) {
        Intent intent = new Intent(this, PlaceDetailsActivity.class);
        PlaceIntentExtras.putPlaceDetails(intent, place);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }
}
