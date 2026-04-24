package com.example.touristguideapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.libraries.places.api.Places;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initData();
        setupHomeSections();
        setupSearch();
        setupVoiceSearch();
        
        // Start with ProgressBar visible
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        // Fetch location and data
        checkLocationPermission();

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

    private void initData() {
        allPlaces = DataProvider.getAllPlaces();
        
        // Initial fallback Top Picks to avoid empty screen
        topPicks = new ArrayList<>(DataProvider.getDefaultTopPicks());

        categories = new ArrayList<>();
        categories.add(new Category("Nature", android.R.drawable.ic_menu_gallery));
        categories.add(new Category("History", android.R.drawable.ic_menu_today));
        categories.add(new Category("Food", android.R.drawable.ic_menu_view));
        categories.add(new Category("Adventure", android.R.drawable.ic_menu_compass));
        categories.add(new Category("Spiritual", android.R.drawable.ic_menu_info_details));
        categories.add(new Category("Shopping", android.R.drawable.ic_menu_agenda));
        categories.add(new Category("Entertainment", android.R.drawable.ic_menu_slideshow));
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastLocation();
        }
    }

    private void getLastLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    updateTopPicksWithLocation(location.getLatitude(), location.getLongitude());
                } else {
                    Log.w("NearbyDebug", "Location is null, using fallback");
                    useFallbackTopPicks();
                }
                hideLoading();
            }).addOnFailureListener(e -> {
                Log.e("NearbyDebug", "Failed to get location", e);
                useFallbackTopPicks();
                hideLoading();
            });
        } catch (SecurityException e) {
            Log.e("NearbyDebug", "Security Exception", e);
            useFallbackTopPicks();
            hideLoading();
        }
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (rvHome != null) rvHome.setVisibility(View.VISIBLE);
    }

    private void updateTopPicksWithLocation(double userLat, double userLng) {
        // Calculate distances for all places
        for (Place place : allPlaces) {
            if (place.getLat() != 0 && place.getLng() != 0) {
                float[] results = new float[1];
                Location.distanceBetween(userLat, userLng, place.getLat(), place.getLng(), results);
                place.setDistance(results[0] / 1000.0); // Convert to km
            }
        }

        // Sort all places by distance
        Collections.sort(allPlaces, Comparator.comparingDouble(Place::getDistance));

        // Select top 5 nearest places
        List<Place> nearestPlaces = new ArrayList<>();
        for (int i = 0; i < Math.min(5, allPlaces.size()); i++) {
            nearestPlaces.add(allPlaces.get(i));
        }

        // Debug-safe fallback logic
        if (nearestPlaces.isEmpty()) {
            Log.d("NearbyDebug", "Nearest places empty, using default top picks");
            nearestPlaces = DataProvider.getDefaultTopPicks();
        }

        Log.d("NearbyDebug", "Nearest places size: " + nearestPlaces.size());

        // Update topPicks list
        topPicks.clear();
        topPicks.addAll(nearestPlaces);

        // Refresh sections to update Browse All order and distances
        if (sections != null) {
            sections.clear();
            addDefaultSections();
        }

        // Update UI
        if (homeAdapter != null) {
            homeAdapter.notifyDataSetChanged();
        }
    }

    private void useFallbackTopPicks() {
        topPicks.clear();
        topPicks.addAll(DataProvider.getDefaultTopPicks());
        
        Log.d("NearbyDebug", "Using fallback. Nearest places size: " + topPicks.size());

        if (sections != null) {
            sections.clear();
            addDefaultSections();
        }

        if (homeAdapter != null) {
            homeAdapter.notifyDataSetChanged();
        }
        Toast.makeText(this, "Location unavailable, using default top picks", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                useFallbackTopPicks();
                hideLoading();
            }
        }
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
