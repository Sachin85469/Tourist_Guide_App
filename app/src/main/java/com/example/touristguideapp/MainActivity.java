package com.example.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.libraries.places.api.Places;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int VOICE_SEARCH_REQUEST_CODE = 101;
    private RecyclerView rvHome;
    private HomeAdapter homeAdapter;
    private List<HomeSection> sections;
    private List<Place> allPlaces;
    private List<Place> topPicks;
    private List<Category> categories;
    
    private ImageView favoritesIcon;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressBar;
    private View emptyStateContainer;
    private EditText searchBox;
    private ImageView btnVoiceSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_API_KEY");
        }

        rvHome = findViewById(R.id.rvHome);
        favoritesIcon = findViewById(R.id.favoritesIcon);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        progressBar = findViewById(R.id.mainProgressBar);
        emptyStateContainer = findViewById(R.id.tvEmptyState);
        searchBox = findViewById(R.id.searchBox);
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch);

        initData();
        setupHomeSections();
        setupSearch();
        setupVoiceSearch();
        
        simulateLoading();

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return true;
                } else if (id == R.id.nav_favorites) {
                    startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return true;
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(MainActivity.this, MapActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    return true;
                }
                return false;
            }
        });

        favoritesIcon.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupVoiceSearch() {
        if (btnVoiceSearch != null) {
            btnVoiceSearch.setOnClickListener(v -> {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the place name...");
                try {
                    startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE);
                } catch (Exception e) {
                    Toast.makeText(this, "Voice search not supported", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String voiceText = result.get(0);
                if (searchBox != null) {
                    searchBox.setText(voiceText);
                }
                filter(voiceText);
            }
        }
    }

    private void setupSearch() {
        if (searchBox != null) {
            searchBox.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filter(String text) {
        List<Place> filteredList = new ArrayList<>();
        for (Place item : allPlaces) {
            if (item.getName().toLowerCase().contains(text.toLowerCase()) || 
                item.getCity().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }

        if (filteredList.isEmpty() && !text.isEmpty()) {
            rvHome.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            rvHome.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            
            sections.clear();
            if (text.isEmpty()) {
                addDefaultSections();
            } else {
                sections.add(new HomeSection(HomeSection.TYPE_ALL_PLACES_HEADER, "Search Results"));
                for (Place p : filteredList) {
                    sections.add(new HomeSection(HomeSection.TYPE_PLACE, p));
                }
            }
            homeAdapter.notifyDataSetChanged();
        }
    }

    private void simulateLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (rvHome != null) rvHome.setVisibility(View.GONE);
        if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.GONE);

        new Handler().postDelayed(() -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (rvHome != null) rvHome.setVisibility(View.VISIBLE);
        }, 1200);
    }

    private void initData() {
        allPlaces = DataProvider.getPlaces();
        
        topPicks = new ArrayList<>();
        if (allPlaces.size() > 2) {
            topPicks.add(allPlaces.get(0));
            topPicks.add(allPlaces.get(1));
        }

        categories = new ArrayList<>();
        categories.add(new Category("Nature", android.R.drawable.ic_menu_gallery));
        categories.add(new Category("History", android.R.drawable.ic_menu_today));
        categories.add(new Category("Food", android.R.drawable.ic_menu_view));
        categories.add(new Category("Adventure", android.R.drawable.ic_menu_compass));
        categories.add(new Category("Spiritual", android.R.drawable.ic_menu_info_details));
        categories.add(new Category("Shopping", android.R.drawable.ic_menu_agenda));
        categories.add(new Category("Entertainment", android.R.drawable.ic_menu_slideshow));
    }

    private void setupHomeSections() {
        sections = new ArrayList<>();
        addDefaultSections();

        homeAdapter = new HomeAdapter(
            sections, 
            categories, 
            topPicks, 
            place -> openDetails(place),
            category -> {
                Intent intent = new Intent(this, CategoryPlacesActivity.class);
                intent.putExtra("category", category);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            },
            v -> {
                startActivity(new Intent(MainActivity.this, PlanTripActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        );

        rvHome.setLayoutManager(new LinearLayoutManager(this));
        rvHome.setAdapter(homeAdapter);
    }

    private void addDefaultSections() {
        sections.add(new HomeSection(HomeSection.TYPE_CATEGORIES));
        sections.add(new HomeSection(HomeSection.TYPE_TOP_PICKS, "Top Picks Near You"));
        sections.add(new HomeSection(HomeSection.TYPE_PLAN_TRIP));
        sections.add(new HomeSection(HomeSection.TYPE_ALL_PLACES_HEADER, "Browse All"));
        for (Place p : allPlaces) {
            sections.add(new HomeSection(HomeSection.TYPE_PLACE, p));
        }
    }

    private void openDetails(Place place) {
        Intent intent = new Intent(MainActivity.this, PlaceDetailsActivity.class);
        intent.putExtra("id", place.getId());
        intent.putExtra("name", place.getName());
        intent.putExtra("description", place.getDescription());
        intent.putExtra("category", place.getCategory());
        intent.putExtra("budget", place.getBudget());
        intent.putExtra("crowdLevel", place.getCrowdLevel());
        intent.putExtra("bestTime", place.getBestTime());
        intent.putExtra("imageResId", place.getImageResId());
        intent.putExtra("tips", place.getTips());
        intent.putExtra("funFact", place.getFunFact());
        intent.putExtra("nearestStation", place.getNearestStation());
        intent.putExtra("lat", place.getLatitude());
        intent.putExtra("lng", place.getLongitude());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
