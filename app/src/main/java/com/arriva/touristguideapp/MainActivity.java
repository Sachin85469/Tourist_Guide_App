package com.arriva.touristguideapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.arriva.touristguideapp.data.places.PlaceMigrationHelper;
import com.arriva.touristguideapp.data.places.PlaceRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final int VOICE_SEARCH_REQUEST_CODE = 101;
    private static final int HOME_CAROUSEL_LIMIT = 15;
    private RecyclerView rvHome;
    private HomeAdapter homeAdapter;
    private List<HomeSection> sections;
    private List<Place> allPlaces;
    private List<Place> topPicks;
    private List<String> userTravelInterests;
    private List<Category> categories;
    
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable reminderRunnable;

    private View btnProfile, btnNotifications;
    private ImageView ivProfileIcon;
    private BottomNavigationView bottomNavigationView;
    private View fabAiChat;
    private View offlineCacheBanner;
    private ProgressBar progressBar;
    private View emptyStateContainer;
    private EditText searchBox;
    private ImageView btnVoiceSearch, searchBtn;
    private TextView tvMainUserName, tvMainUserEmail, tvQuickStats, tvProfileBadge;
    private String resolvedCity = "Your Current Location";

    private PlaceRepository placeRepository;
    private com.arriva.touristguideapp.data.analytics.AnalyticsRepository analyticsRepository;
    private com.arriva.touristguideapp.data.places.DiscoveryRepository discoveryRepository;
    private com.arriva.touristguideapp.data.places.SearchHistoryManager searchHistoryManager;
    private com.arriva.touristguideapp.data.places.RecentlyViewedManager recentlyViewedManager;
    private com.arriva.touristguideapp.data.notifications.NotificationRepository notificationRepository;

    /** Last fix used to sort "Top Picks Near You" after the Firestore catalog arrives. */
    @Nullable
    private Location cachedUserLocation;
    private boolean offlineCacheBannerDismissed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("APP_DEBUG", "MainActivity started");
        PerformanceTracker.startTimer("MAIN_ACTIVITY_INIT");
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (!BaseActivity.canEnterMainActivity(currentUser)) {
            auth.signOut();
            Toast.makeText(
                    this,
                    "Please sign in with Google or verify your email before continuing.",
                    Toast.LENGTH_LONG
            ).show();
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        Log.d("APP_DEBUG", "Layout loaded");

        // Initialize Places SDK
        String googlePlacesKey = BuildConfig.GOOGLE_PLACES_API_KEY.trim();
        if (!Places.isInitialized() && isConfiguredApiKey(googlePlacesKey)) {
            Places.initialize(getApplicationContext(), googlePlacesKey);
        } else if (!Places.isInitialized()) {
            Log.w(TAG, "Google Places SDK not initialized: GOOGLE_PLACES_API_KEY is missing.");
        }

        rvHome = findViewById(R.id.rvHome);
        btnProfile = findViewById(R.id.btnProfile);
        ivProfileIcon = findViewById(R.id.btnProfile);
        tvProfileBadge = findViewById(R.id.tvProfileBadge);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        tvMainUserName = findViewById(R.id.tvMainUserName);
        tvMainUserEmail = findViewById(R.id.tvMainUserEmail);
        tvQuickStats = findViewById(R.id.tvQuickStats);
        fabAiChat = findViewById(R.id.fabAiChat);
        btnNotifications = findViewById(R.id.btnNotifications);

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                startActivity(new Intent(this, NotificationHistoryActivity.class));
            });
        }
        View aiTooltip = findViewById(R.id.ai_tooltip);

        if (fabAiChat != null) {
            fabAiChat.setOnClickListener(v -> {
                startActivity(new Intent(this, AiChatActivity.class));
            });

            // Delay tooltip entrance
            if (aiTooltip != null) {
                handler.postDelayed(() -> {
                    aiTooltip.setVisibility(View.VISIBLE);
                    aiTooltip.setAlpha(0f);
                    aiTooltip.setTranslationY(20f);
                    aiTooltip.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .setInterpolator(new android.view.animation.OvershootInterpolator())
                        .start();
                }, 3000);
            }
        }

        progressBar = findViewById(R.id.mainProgressBar);
        emptyStateContainer = findViewById(R.id.tvEmptyState);
        searchBox = findViewById(R.id.searchBox);
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch);
        offlineCacheBanner = findViewById(R.id.offlineCacheBanner);
        View btnDismissOfflineBanner = findViewById(R.id.btnDismissOfflineBanner);
        if (btnDismissOfflineBanner != null) {
            btnDismissOfflineBanner.setOnClickListener(v -> {
                offlineCacheBannerDismissed = true;
                showOfflineCacheBanner(false);
            });
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initData();
        setupHomeSections();
        setupSearch();
        setupVoiceSearch();

        // Handle Notification Intent
        handleNotificationIntent(getIntent());

        // Phase 10: Local Notification Reminder (Requirement 5)
        if (savedInstanceState == null) {
            reminderRunnable = () -> {
                com.arriva.touristguideapp.data.notifications.LocalNotificationHelper.showReminder(
                    MainActivity.this, "Ready for Adventure?", "Explore the best hidden gems today!");
            };
            handler.postDelayed(reminderRunnable, 5000);
        }
        
        // Start with ProgressBar visible
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        // Fetch location and data
        checkLocationPermission();
        loadPublishedPlacesCatalog();

        PerformanceTracker.endTimer("MAIN_ACTIVITY_INIT");

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return true;
                } else if (id == R.id.nav_favorites) {
                    startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                } else if (id == R.id.nav_map) {
                    startActivity(new Intent(MainActivity.this, MapActivity.class));
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    return true;
                }
                return false;
            }
        });

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private boolean isConfiguredApiKey(String apiKey) {
        return apiKey != null
                && !apiKey.isEmpty()
                && !apiKey.equalsIgnoreCase("YOUR_API_KEY")
                && !apiKey.startsWith("YOUR_");
    }

    @Override
    protected void onDestroy() {
        if (reminderRunnable != null) {
            handler.removeCallbacks(reminderRunnable);
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("notification_type")) {
            String type = intent.getStringExtra("notification_type");
            Log.d(TAG, "NOTIFICATION_OPENED: type=" + type);
            if (notificationRepository != null) {
                notificationRepository.logNotificationOpened(type);
            }
            // Navigate based on type if needed
            if ("trending".equals(type)) {
                // Scroll to trending section or open a trending activity
            }
        }
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
        if (text == null) text = "";
        text = text.trim();

        if (text.isEmpty()) {
            sections.clear();
            addDefaultSections();
            if (homeAdapter != null) {
                homeAdapter.updateSections(new ArrayList<>(sections));
            }
            return;
        }

        searchHistoryManager.addSearch(text);
        List<Place> filteredList = discoveryRepository.searchAndRank(allPlaces, text);
        analyticsRepository.logSearch(text, filteredList.size());

        if (filteredList.isEmpty()) {
            rvHome.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            rvHome.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            
            sections.clear();
            sections.add(new HomeSection(HomeSection.TYPE_ALL_PLACES_HEADER, getString(R.string.search_results, filteredList.size())));
            for (Place p : filteredList) {
                sections.add(new HomeSection(HomeSection.TYPE_PLACE, p));
            }
            if (homeAdapter != null) {
                homeAdapter.updateSections(new ArrayList<>(sections));
            }
        }
    }

    private void initData() {
        placeRepository = new PlaceRepository(this);
        analyticsRepository = new com.arriva.touristguideapp.data.analytics.AnalyticsRepository();
        discoveryRepository = new com.arriva.touristguideapp.data.places.DiscoveryRepository();
        searchHistoryManager = new com.arriva.touristguideapp.data.places.SearchHistoryManager(this);
        recentlyViewedManager = new com.arriva.touristguideapp.data.places.RecentlyViewedManager(this);
        notificationRepository = new com.arriva.touristguideapp.data.notifications.NotificationRepository(this);

        // Browse-all list is filled asynchronously via PlaceRepository (Firestore with local fallback).
        allPlaces = new ArrayList<>();

        // Initial Top Picks row until location + catalog are ready (unchanged UX).
        topPicks = new ArrayList<>(DataProvider.getDefaultTopPicks());
        userTravelInterests = new ArrayList<>();

        categories = new ArrayList<>();
        categories.add(new Category(getString(R.string.cat_historical), R.drawable.ic_map_marker_historical));
        categories.add(new Category(getString(R.string.cat_nature), R.drawable.ic_map_marker_nature));
        categories.add(new Category(getString(R.string.cat_religious), R.drawable.ic_map_marker_temple));
        categories.add(new Category(getString(R.string.cat_food), R.drawable.ic_map_marker_food));
        categories.add(new Category(getString(R.string.cat_culture), R.drawable.ic_mood));
        categories.add(new Category(getString(R.string.cat_adventure), R.drawable.ic_map_marker_adventure));
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
                    cachedUserLocation = location;
                    Log.i(TAG, "USER_LOCATION_UPDATED lat=" + location.getLatitude() + " lng=" + location.getLongitude());
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
        // Geocode city name dynamically
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, Locale.getDefault());
            List<android.location.Address> addresses = geocoder.getFromLocation(userLat, userLng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                if (city != null && !city.trim().isEmpty()) {
                    resolvedCity = city;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Geocoder failed", e);
        }
        refreshQuickStatsOnly();

        // Calculate distances for all places using LocationUtils
        for (Place place : allPlaces) {
            if (place.getLat() != 0 && place.getLng() != 0) {
                place.setDistance(LocationUtils.calculateDistance(userLat, userLng, place.getLat(), place.getLng()));
            }
        }

        // Sort all places by distance
        Collections.sort(allPlaces, Comparator.comparingDouble(Place::getDistance));

        List<Place> nearestPlaces = selectTopPicksFromOrderedPlaces(allPlaces);

        // Debug-safe fallback logic
        if (nearestPlaces.isEmpty()) {
            Log.d("NearbyDebug", "Nearest places empty, using default top picks");
            nearestPlaces = DataProvider.getDefaultTopPicks();
        }

        Log.d("NearbyDebug", "Nearest places size: " + nearestPlaces.size());
        applyTopPicks(nearestPlaces);
    }

    private void useFallbackTopPicks() {
        updateTopPicksWithoutLocation(true);
    }

    private void updateTopPicksWithoutLocation(boolean showToast) {
        List<Place> sourcePlaces = allPlaces != null && !allPlaces.isEmpty()
                ? new ArrayList<>(allPlaces)
                : DataProvider.getAllPlaces();

        sourcePlaces.sort(
                Comparator.comparing(Place::isTopPick).reversed()
                        .thenComparing(Comparator.comparingDouble(Place::getRating).reversed())
        );

        List<Place> selectedPlaces = selectTopPicksFromOrderedPlaces(sourcePlaces);
        if (selectedPlaces.isEmpty()) {
            selectedPlaces = DataProvider.getDefaultTopPicks();
        }

        Log.d("NearbyDebug", "Using fallback. Nearest places size: " + selectedPlaces.size());
        applyTopPicks(selectedPlaces);
        if (showToast) {
            Toast.makeText(this, "Location unavailable, using default top picks", Toast.LENGTH_SHORT).show();
        }
    }

    private List<Place> selectTopPicksFromOrderedPlaces(List<Place> orderedPlaces) {
        List<Place> selectedPlaces = new ArrayList<>();
        if (orderedPlaces == null || orderedPlaces.isEmpty()) {
            return selectedPlaces;
        }

        List<Place> candidatePlaces = new ArrayList<>();
        if (userTravelInterests != null && !userTravelInterests.isEmpty()) {
            for (Place place : orderedPlaces) {
                if (placeMatchesTravelInterests(place)) {
                    candidatePlaces.add(place);
                }
            }
        }

        if (candidatePlaces.isEmpty()) {
            candidatePlaces.addAll(orderedPlaces);
        }

        for (int i = 0; i < Math.min(5, candidatePlaces.size()); i++) {
            selectedPlaces.add(candidatePlaces.get(i));
        }
        return selectedPlaces;
    }

    private boolean placeMatchesTravelInterests(Place place) {
        if (place == null || userTravelInterests == null || userTravelInterests.isEmpty()) {
            return false;
        }

        String category = normalizeTravelInterest(place.getCategory());
        String categoryId = normalizeTravelInterest(place.getCategoryId());
        for (String interest : userTravelInterests) {
            if (interest.equals(category) || interest.equals(categoryId)) {
                return true;
            }
        }
        return false;
    }

    private void updateTravelInterests(@Nullable List<String> travelInterests) {
        List<String> normalizedInterests = new ArrayList<>();
        if (travelInterests != null) {
            for (String interest : travelInterests) {
                String normalized = normalizeTravelInterest(interest);
                if (!normalized.isEmpty() && !normalizedInterests.contains(normalized)) {
                    normalizedInterests.add(normalized);
                }
            }
        }

        if (userTravelInterests == null) {
            userTravelInterests = new ArrayList<>();
        }

        Set<String> oldInterests = new HashSet<>(userTravelInterests);
        Set<String> newInterests = new HashSet<>(normalizedInterests);
        if (oldInterests.equals(newInterests)) {
            return;
        }

        userTravelInterests.clear();
        userTravelInterests.addAll(normalizedInterests);
        refreshHomeSections();
    }

    private String normalizeTravelInterest(@Nullable String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        if (normalized.equals("history") || normalized.equals("historic") || normalized.equals("historical")) {
            return "historical";
        }
        if (normalized.equals("spiritual") || normalized.equals("religion")
                || normalized.equals("religious") || normalized.equals("temple") || normalized.equals("temples")) {
            return "religious";
        }
        return normalized;
    }

    private void applyTopPicks(List<Place> selectedPlaces) {
        topPicks.clear();
        topPicks.addAll(selectedPlaces);
        refreshHomeSections();
    }

    private void refreshHomeSections() {
        if (sections != null) {
            sections.clear();
            addDefaultSections();
        }

        if (homeAdapter != null) {
            homeAdapter.updateSections(new ArrayList<>(sections));
        }
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
            },
            v -> {
                startActivity(new Intent(MainActivity.this, com.arriva.touristguideapp.communication.CommunicationHubActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        );

        rvHome.setLayoutManager(new LinearLayoutManager(this));
        rvHome.setAdapter(homeAdapter);
        rvHome.setItemViewCacheSize(20);
        rvHome.setHasFixedSize(true);
        
        // Premium Staggered Animation
        android.view.animation.LayoutAnimationController animation = android.view.animation.AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down);
        rvHome.setLayoutAnimation(animation);
        Log.d("APP_DEBUG", "RecyclerView initialized");
    }

    private void addDefaultSections() {
        sections.add(new HomeSection(HomeSection.TYPE_WELCOME));

        List<Place> forYouPlaces = buildForYouPlaces();
        if (!forYouPlaces.isEmpty()) {
            sections.add(new HomeSection(HomeSection.TYPE_FOR_YOU, getString(R.string.your_picks_for_you_header), forYouPlaces));
        }

        List<Place> topRatedPlaces = buildTopRatedPlaces();
        if (!topRatedPlaces.isEmpty()) {
            sections.add(new HomeSection(HomeSection.TYPE_POPULAR_THIS_WEEK, getString(R.string.top_rated_arriva_header), topRatedPlaces));
        }

        sections.add(new HomeSection(HomeSection.TYPE_CATEGORIES, getString(R.string.explore_categories_header)));
    }

    private List<Place> buildForYouPlaces() {
        List<Place> forYouPlaces = new ArrayList<>();
        if (allPlaces == null || allPlaces.isEmpty()
                || userTravelInterests == null || userTravelInterests.isEmpty()) {
            return forYouPlaces;
        }

        for (Place place : allPlaces) {
            if (placeMatchesTravelInterests(place)) {
                forYouPlaces.add(place);
            }
        }
        sortPlacesByRating(forYouPlaces);
        return limitedPlaces(forYouPlaces);
    }

    private List<Place> buildTopRatedPlaces() {
        List<Place> topRatedPlaces = new ArrayList<>();
        if (allPlaces == null || allPlaces.isEmpty()) {
            return topRatedPlaces;
        }

        for (Place place : allPlaces) {
            if (place != null && place.getTotalRatings() > 0) {
                topRatedPlaces.add(place);
            }
        }
        sortPlacesByRating(topRatedPlaces);
        return limitedPlaces(topRatedPlaces);
    }

    private void sortPlacesByRating(List<Place> places) {
        places.sort(
                Comparator.comparingDouble(Place::getRating).reversed()
                        .thenComparing(Comparator.comparingLong(Place::getTotalRatings).reversed())
                        .thenComparing(place -> place.getName() != null ? place.getName() : "", String.CASE_INSENSITIVE_ORDER)
        );
    }

    private List<Place> limitedPlaces(List<Place> places) {
        return new ArrayList<>(places.subList(0, Math.min(HOME_CAROUSEL_LIMIT, places.size())));
    }

    /**
     * TEMPORARY: uploads all {@link DataProvider#getAllPlaces()} documents into Firestore {@code places}.
     * Remove this method, the {@code tvMainUserEmail} long-press hook, and {@code btnSeedFirestorePlaces} after migration.
     */
    private void migratePlacesToFirestore() {
        PlaceMigrationHelper.migratePlacesToFirestore(getApplicationContext(), (success, failure) ->
                runOnUiThread(() -> Toast.makeText(this,
                        "Migration finished: " + success + " succeeded, " + failure + " failed",
                        Toast.LENGTH_LONG).show()));
    }

    /**
     * Loads published places for the curated home sections (Firestore, else {@link DataProvider} via repository fallback).
     */
    private void loadPublishedPlacesCatalog() {
        if (placeRepository == null) {
            placeRepository = new PlaceRepository(this);
        }
        placeRepository.getPlacesOfflineFirst(null, null, (places, origin, cacheEmpty, message) -> {
            boolean fromFirestore = origin == PlaceRepository.DataOrigin.FIRESTORE;
            boolean fromRoomCache = origin == PlaceRepository.DataOrigin.ROOM_CACHE;
            if (fromFirestore) {
                Log.d(TAG, "fetchPublishedPlaces: Firestore success (main home) count=" + places.size());
                showOfflineCacheBanner(false);
            } else if (fromRoomCache) {
                Log.w(TAG, "getPlacesOfflineFirst: ROOM_CACHE (main home) count=" + places.size()
                        + " cacheEmpty=" + cacheEmpty
                        + (message != null ? (" detail=" + message) : ""));
                showOfflineCacheBanner(!cacheEmpty);
            } else {
                Log.w(TAG, "fetchPublishedPlaces: LOCAL_FALLBACK (main home) count=" + places.size()
                        + (message != null ? (" detail=" + message) : ""));
                showOfflineCacheBanner(false);
            }

            allPlaces.clear();
            allPlaces.addAll(places);
            Log.d("APP_DEBUG", "Data loaded, count=" + places.size());

            if (cachedUserLocation != null) {
                updateTopPicksWithLocation(cachedUserLocation.getLatitude(), cachedUserLocation.getLongitude());
            } else {
                if (sections != null) {
                    sections.clear();
                    addDefaultSections();
                    if (homeAdapter != null) {
                        homeAdapter.updateSections(new ArrayList<>(sections));
                    }
                }
            }

            if (searchBox != null) {
                String q = searchBox.getText() != null ? searchBox.getText().toString() : "";
                if (!q.trim().isEmpty()) {
                    filter(q);
                }
            }

            if (homeAdapter != null) {
                Log.d(TAG, "HomeAdapter item count=" + homeAdapter.getItemCount());
            }
        });
    }

    private void showOfflineCacheBanner(boolean show) {
        if (offlineCacheBanner != null) {
            offlineCacheBanner.setVisibility(show && !offlineCacheBannerDismissed ? View.VISIBLE : View.GONE);
        }
    }

    private void openDetails(Place place) {
        if (place == null) return;
        recentlyViewedManager.addRecentlyViewed(place);
        Intent intent = new Intent(MainActivity.this, PlaceDetailsActivity.class);
        PlaceIntentExtras.putPlaceDetails(intent, place);
        
        androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_in_right, android.R.anim.fade_out);
        startActivity(intent, options.toBundle());
    }

    private void loadUserInfo() {
        // Load local avatar immediately from SharedPreferences for better UX (Requirement 4)
        ProfileUtils.loadAvatar(this, ivProfileIcon);

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            ProfileUtils.fetchUserData(user.getUid(), new ProfileUtils.UserCallback() {
                @Override
                public void onUserLoaded(User userModel) {
                    if (tvMainUserName != null) tvMainUserName.setText(formatName(userModel.getName()));
                    if (tvMainUserEmail != null) tvMainUserEmail.setText(userModel.getEmail());
                    ProfileUtils.loadAvatar(MainActivity.this, ivProfileIcon, userModel);
                    updateTravelInterests(userModel.getTravelInterests());
                }

                @Override
                public void onError(Exception e) {
                    if (tvMainUserName != null) tvMainUserName.setText(user.getDisplayName() != null ? formatName(user.getDisplayName()) : "Traveler");
                    if (tvMainUserEmail != null) tvMainUserEmail.setText(user.getEmail());
                    ProfileUtils.loadAvatar(MainActivity.this, ivProfileIcon);
                }
            });
        }
    }

    private String formatName(String name) {
        if (name == null || name.trim().isEmpty()) return "Traveler";
        String first = name.trim().split("\\s+")[0];
        if (first.isEmpty()) return "Traveler";
        return first.substring(0, 1).toUpperCase() + first.substring(1).toLowerCase();
    }

    public void refreshQuickStatsOnly() {
        // Redesigned header doesn't show city in stats row anymore
        int favoritesCount = FavoritesManager.getFavoritesCount(this);
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("trips")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    int tripsCount = 0;
                    if (task.isSuccessful() && task.getResult() != null) {
                        tripsCount = task.getResult().size();
                    }
                    if (tvQuickStats != null) {
                        tvQuickStats.setText(getString(R.string.quick_stats_format, favoritesCount, tripsCount));
                    }
                });
        } else {
            if (tvQuickStats != null) {
                tvQuickStats.setText(getString(R.string.quick_stats_format, favoritesCount, 0));
            }
        }
    }

    private void refreshRecentlyViewed() {
        // No-op as recently viewed card is removed from Home screen
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
        refreshQuickStatsOnly();
        FavoritesManager.syncFavoritesFromFirestore(this);
        if (tvProfileBadge != null && notificationRepository != null) {
            int unreadCount = notificationRepository.getUnreadCount();
            if (unreadCount > 0) {
                tvProfileBadge.setText(String.valueOf(unreadCount));
                tvProfileBadge.setVisibility(View.VISIBLE);
            } else {
                tvProfileBadge.setVisibility(View.GONE);
            }
        }
    }
}
