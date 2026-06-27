package com.arriva.touristguideapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.arriva.touristguideapp.data.places.AppDatabase;
import com.arriva.touristguideapp.data.places.PlaceDao;
import com.arriva.touristguideapp.data.places.PlaceEntity;
import com.arriva.touristguideapp.data.places.PlaceRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends BaseActivity {

    private static final int VOICE_SEARCH_REQUEST_CODE = 101;
    private RecyclerView recyclerViewPlaces;
    private PlaceAdapter placeAdapter;
    private EditText editTextSearch;
    private ImageView btnVoiceSearch;
    private TextView ivProfileIcon;
    private TextView tvHomeUserName, tvHomeUserEmail;
    private View fabAiChat;
    private View fabNearbyNow;
    private View offlineCacheBanner;
    private PlaceRepository placeRepository;
    private final List<Place> allPlaces = new ArrayList<>();
    private long lastSearchLogTime = 0;
    private static final long SEARCH_LOG_DEBOUNCE = 2000;
    private boolean offlineCacheBannerDismissed = false;
    private static final int REQUEST_NEARBY_LOCATION = 301;
    private static final ExecutorService BG_EXECUTOR = Executors.newSingleThreadExecutor();
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Initialize views
        recyclerViewPlaces = findViewById(R.id.recyclerViewPlaces);
        editTextSearch = findViewById(R.id.editTextSearch);
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);
        tvHomeUserName = findViewById(R.id.tvHomeUserName);
        tvHomeUserEmail = findViewById(R.id.tvHomeUserEmail);
        fabAiChat = findViewById(R.id.fabAiChat);
        fabNearbyNow = findViewById(R.id.fabNearbyNow);
        offlineCacheBanner = findViewById(R.id.offlineCacheBanner);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        View btnDismissOfflineBanner = findViewById(R.id.btnDismissOfflineBanner);
        if (btnDismissOfflineBanner != null) {
            btnDismissOfflineBanner.setOnClickListener(v -> {
                offlineCacheBannerDismissed = true;
                showOfflineCacheBanner(false);
            });
        }
        placeRepository = new PlaceRepository(this);

        // AI Chatbot setup
        if (fabAiChat != null) {
            fabAiChat.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return false;
            });

            View aiCard = findViewById(R.id.cardAiAssistant);
            View aiIcon = findViewById(R.id.ivAiAssistantIcon);
            View.OnClickListener aiClickListener = v -> {
                Log.d("HomeActivity", "Chatbot icon clicked");
                launchAiChatActivity("home_floating_button");
            };
            fabAiChat.setOnClickListener(aiClickListener);
            if (aiCard != null) aiCard.setOnClickListener(aiClickListener);
            if (aiIcon != null) aiIcon.setOnClickListener(aiClickListener);
        }

        // Profile Avatar and User Info Setup
        loadUserInfo();
        ivProfileIcon.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 2. Set LayoutManager (Vertical)
        recyclerViewPlaces.setLayoutManager(new LinearLayoutManager(this));

        // 4. Create Adapter and set it to RecyclerView
        placeAdapter = new PlaceAdapter(new ArrayList<>(), place -> {
            // Track search click (Requirement 2)
            String query = editTextSearch != null ? editTextSearch.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                new com.arriva.touristguideapp.data.analytics.AnalyticsRepository().logSearchClick(query, place.getId());
            }

            Intent intent = new Intent(this, PlaceDetailsActivity.class);
            PlaceIntentExtras.putPlaceDetails(intent, place);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        recyclerViewPlaces.setAdapter(placeAdapter);

        // 5. Setup Category Buttons
        setupCategoryButtons();

        // 6. Update Category Counts
        updateHomeCategoryCounts();
        loadPlaces();

        // 8. Add Search Functionality
        if (editTextSearch != null) {
            editTextSearch.addTextChangedListener(new TextWatcher() {
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

        // 9. Voice Search
        if (btnVoiceSearch != null) {
            btnVoiceSearch.setOnClickListener(v -> startVoiceSearch());
        }

        // 10. Nearby Now FAB
        setupNearbyNowButton();
    }

    // ─── Nearby Now ──────────────────────────────────────────────────────────

    private void setupNearbyNowButton() {
        if (fabNearbyNow == null) return;
        fabNearbyNow.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fetchNearbyPlaces();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_NEARBY_LOCATION);
            }
        });
    }

    private void launchAiChatActivity(String source) {
        try {
            Log.d("HomeActivity", "Launching AIChatActivity from " + source);
            startActivity(new Intent(this, AiChatActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e("HomeActivity", "Failed to launch AIChatActivity from " + source, e);
            Toast.makeText(this, "Unable to open assistant", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchNearbyPlaces() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_NEARBY_LOCATION);
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location == null) {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double userLat = location.getLatitude();
                    double userLng = location.getLongitude();
                    Log.d("HomeActivity", "NEARBY_NOW: lat=" + userLat + " lng=" + userLng);

                    // Query Room cache on background thread, merge Firestore if online
                    BG_EXECUTOR.execute(() -> {
                        List<Place> cachedPlaces = loadAllFromRoom();
                        runOnUiThread(() -> {
                            if (isOnline() && placeRepository != null) {
                                // Merge Firestore results into cached list
                                placeRepository.fetchPublishedPlaces((remotePlaces, origin, msg) -> {
                                    List<Place> merged = mergePlaces(cachedPlaces, remotePlaces);
                                    showNearbySheet(merged, userLat, userLng);
                                });
                            } else {
                                showNearbySheet(cachedPlaces, userLat, userLng);
                            }
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.w("HomeActivity", "NEARBY_NOW location error: " + e.getMessage());
                    Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show();
                });
        } catch (SecurityException e) {
            Log.w("HomeActivity", "NEARBY_NOW permission revoked before location request", e);
            Toast.makeText(this, "Location permission needed for Nearby Now", Toast.LENGTH_SHORT).show();
        }
    }

    /** Loads all Place rows from Room on the calling (background) thread. */
    private List<Place> loadAllFromRoom() {
        try {
            PlaceDao dao = AppDatabase.getInstance(this).placeDao();
            List<PlaceEntity> entities = dao.getAll();
            List<Place> places = new ArrayList<>();
            for (PlaceEntity e : entities) {
                if (e != null) places.add(e.toPlace());
            }
            return places;
        } catch (Exception ex) {
            Log.w("HomeActivity", "NEARBY_NOW room load failed: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    /** Merges Firestore places into the base list, adding those not already present (by ID). */
    private List<Place> mergePlaces(List<Place> base, List<Place> remote) {
        if (remote == null || remote.isEmpty()) return base;
        java.util.Set<String> existingIds = new java.util.HashSet<>();
        for (Place p : base) if (p.getId() != null) existingIds.add(p.getId());
        List<Place> merged = new ArrayList<>(base);
        for (Place p : remote) {
            if (p.getId() != null && !existingIds.contains(p.getId())) {
                merged.add(p);
            }
        }
        return merged;
    }

    private void showNearbySheet(List<Place> allPlacesForNearby, double userLat, double userLng) {
        // Set distances on all places
        for (Place p : allPlacesForNearby) {
            p.setDistance(LocationUtils.calculateDistance(userLat, userLng, p.getLat(), p.getLng()));
        }

        // Try 5km first
        List<Place> within5km = filterByRadius(allPlacesForNearby, 5.0);
        List<Place> results;
        String title;

        if (!within5km.isEmpty()) {
            results = within5km;
            title = "📍 Within 5km of you";
        } else {
            // Expand to 15km
            results = filterByRadius(allPlacesForNearby, 15.0);
            title = results.isEmpty() ? "📍 Nearby places" : "📍 Within 15km of you";
        }

        // Sort by distance ascending
        java.util.Collections.sort(results,
            (a, b) -> Double.compare(a.getDistance(), b.getDistance()));

        BottomSheetNearbyFragment sheet = BottomSheetNearbyFragment.withPlaces(title, results);
        sheet.show(getSupportFragmentManager(), BottomSheetNearbyFragment.TAG);
    }

    private List<Place> filterByRadius(List<Place> places, double radiusKm) {
        List<Place> nearby = new ArrayList<>();
        for (Place p : places) {
            if (p.getDistance() >= 0 && p.getDistance() <= radiusKm) {
                nearby.add(p);
            }
        }
        return nearby;
    }

    private boolean isOnline() {
        android.net.ConnectivityManager cm =
            (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network net = cm.getActiveNetwork();
        if (net == null) return false;
        android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(net);
        return caps != null && (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
            || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
            || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void updateCategoryCounts() {
        Map<String, Integer> categoryCount = DataProvider.getCategoryCount(allPlaces);
        TextView txtHistory = findViewById(R.id.txtHistoryCount);
        if (txtHistory != null) txtHistory.setText("🏛️ History (" + categoryCount.getOrDefault("History", 0) + ")");

        TextView txtSpiritual = findViewById(R.id.txtSpiritualCount);
        if (txtSpiritual != null) txtSpiritual.setText("🙏 Spiritual (" + categoryCount.getOrDefault("Spiritual", 0) + ")");

        TextView txtFood = findViewById(R.id.txtFoodCount);
        if (txtFood != null) txtFood.setText("🍕 Food (" + categoryCount.getOrDefault("Food", 0) + ")");

        TextView txtShopping = findViewById(R.id.txtShoppingCount);
        if (txtShopping != null) txtShopping.setText("🛍️ Shopping (" + categoryCount.getOrDefault("Shopping", 0) + ")");

        TextView txtNature = findViewById(R.id.txtNatureCount);
        if (txtNature != null) txtNature.setText("🌲 Nature (" + categoryCount.getOrDefault("Nature", 0) + ")");

        TextView txtAdventure = findViewById(R.id.txtAdventureCount);
        if (txtAdventure != null) txtAdventure.setText("🎢 Adventure (" + categoryCount.getOrDefault("Adventure", 0) + ")");

        TextView txtEntertainment = findViewById(R.id.txtEntertainmentCount);
        if (txtEntertainment != null) txtEntertainment.setText("🎮 Entertainment (" + categoryCount.getOrDefault("Entertainment", 0) + ")");
    }

    private void updateHomeCategoryCounts() {
        Map<String, Integer> categoryCount = DataProvider.getCategoryCount(allPlaces);
        setCategoryChipText(R.id.btnAll, "All", allPlaces.size());
        setCategoryChipText(R.id.btnHistorical, "History", categoryCount.getOrDefault("History", 0));
        setCategoryChipText(R.id.spiritualCard, "Spiritual", categoryCount.getOrDefault("Spiritual", 0));
        setCategoryChipText(R.id.btnFood, "Food", categoryCount.getOrDefault("Food", 0));
        setCategoryChipText(R.id.shoppingCard, "Shopping", categoryCount.getOrDefault("Shopping", 0));
        setCategoryChipText(R.id.btnNature, "Nature", categoryCount.getOrDefault("Nature", 0));
        setCategoryChipText(R.id.btnAdventure, "Adventure", categoryCount.getOrDefault("Adventure", 0));
        setCategoryChipText(R.id.entertainmentCard, "Entertainment", categoryCount.getOrDefault("Entertainment", 0));
    }

    private void setCategoryChipText(int viewId, String label, int count) {
        TextView chip = findViewById(viewId);
        if (chip != null) {
            chip.setText(label + " (" + count + ")");
        }
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for a place...");
        try {
            startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Voice search not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String voiceQuery = results.get(0);
                if (editTextSearch != null) {
                    editTextSearch.setText(voiceQuery);
                }
                filter(voiceQuery);
            }
        }
    }

    private void setupCategoryButtons() {
        View btnAll = findViewById(R.id.btnAll);
        if (btnAll != null) {
            btnAll.setOnClickListener(v -> {
                if (editTextSearch != null) {
                    editTextSearch.setText("");
                }
                if (placeAdapter != null) {
                    placeAdapter.updateList(new ArrayList<>(allPlaces));
                }
            });
        }

        View btnHistory = findViewById(R.id.btnHistorical);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> openCategory("History"));
        }

        View btnSpiritual = findViewById(R.id.spiritualCard);
        if (btnSpiritual != null) {
            btnSpiritual.setOnClickListener(v -> openCategory("Spiritual"));
        }

        View btnFood = findViewById(R.id.btnFood);
        if (btnFood != null) {
            btnFood.setOnClickListener(v -> openCategory("Food"));
        }

        View btnShopping = findViewById(R.id.shoppingCard);
        if (btnShopping != null) {
            btnShopping.setOnClickListener(v -> openCategory("Shopping"));
        }

        View btnNature = findViewById(R.id.btnNature);
        if (btnNature != null) {
            btnNature.setOnClickListener(v -> openCategory("Nature"));
        }

        View btnAdventure = findViewById(R.id.btnAdventure);
        if (btnAdventure != null) {
            btnAdventure.setOnClickListener(v -> openCategory("Adventure"));
        }

        View btnEntertainment = findViewById(R.id.entertainmentCard);
        if (btnEntertainment != null) {
            btnEntertainment.setOnClickListener(v -> openCategory("Entertainment"));
        }
    }

    private void openCategory(String category) {
        // Track Category Explored (Requirement 3)
        new com.arriva.touristguideapp.data.analytics.AnalyticsRepository().trackCategoryExplored(category);

        Intent intent = new Intent(this, CategoryPlacesActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // Method to filter the list based on search text using DataProvider utility
    private void filter(String query) {
        List<Place> filteredList = searchLoadedPlaces(query);

        // Update the adapter with the filtered list
        if (placeAdapter != null) {
            placeAdapter.updateList(filteredList);
        }

        // Requirement 2 & 7: Log search analytics with debounce
        if (query != null && query.trim().length() >= 3) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSearchLogTime > SEARCH_LOG_DEBOUNCE) {
                new com.arriva.touristguideapp.data.analytics.AnalyticsRepository().logSearch(query, filteredList.size());
                lastSearchLogTime = currentTime;
            }
        }
    }

    private void loadPlaces() {
        placeRepository.getPlacesOfflineFirst(null, null, (places, origin, cacheEmpty, message) -> {
            showOfflineCacheBanner(origin == PlaceRepository.DataOrigin.ROOM_CACHE && !cacheEmpty);
            allPlaces.clear();
            allPlaces.addAll(places);
            updateHomeCategoryCounts();

            String query = editTextSearch != null && editTextSearch.getText() != null
                    ? editTextSearch.getText().toString()
                    : "";
            if (placeAdapter != null) {
                placeAdapter.updateList(query.trim().isEmpty() ? allPlaces : searchLoadedPlaces(query));
            }
        });
    }

    private List<Place> searchLoadedPlaces(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return new ArrayList<>(allPlaces);
        }

        List<Place> filtered = new ArrayList<>();
        for (Place place : allPlaces) {
            if (place == null) {
                continue;
            }
            if (containsIgnoreCase(place.getName(), normalized)
                    || containsIgnoreCase(place.getCategory(), normalized)
                    || containsIgnoreCase(place.getCity(), normalized)
                    || containsIgnoreCase(place.getDescription(), normalized)) {
                filtered.add(place);
            }
        }
        return filtered;
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.US).contains(normalizedQuery);
    }

    private void showOfflineCacheBanner(boolean show) {
        if (offlineCacheBanner != null) {
            offlineCacheBanner.setVisibility(show && !offlineCacheBannerDismissed ? View.VISIBLE : View.GONE);
        }
    }

    private void loadUserInfo() {
        if (tvHomeUserEmail != null) {
            tvHomeUserEmail.setText(getGreeting());
        }
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String fallbackName = user.getDisplayName() != null ? user.getDisplayName() : "ExploreEase";
            applyHomeIdentity(fallbackName);
            ProfileUtils.fetchUserData(user.getUid(), new ProfileUtils.UserCallback() {
                @Override
                public void onUserLoaded(User userModel) {
                    applyHomeIdentity(userModel.getName());
                }

                @Override
                public void onError(Exception e) {
                    applyHomeIdentity(fallbackName);
                }
            });
        } else {
            applyHomeIdentity("ExploreEase");
        }
    }

    private void applyHomeIdentity(String name) {
        String displayName = name == null || name.trim().isEmpty() ? "ExploreEase" : name.trim();
        if (tvHomeUserName != null) {
            tvHomeUserName.setText(displayName);
        }
        if (tvHomeUserEmail != null) {
            tvHomeUserEmail.setText(getGreeting());
        }
        if (ivProfileIcon != null) {
            ivProfileIcon.setText(getInitials(displayName));
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "EE";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
                if (initials.length() == 2) {
                    break;
                }
            }
        }
        return initials.length() > 0 ? initials.toString() : "EE";
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Good morning";
        }
        if (hour < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NEARBY_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchNearbyPlaces();
        } else if (requestCode == REQUEST_NEARBY_LOCATION) {
            Toast.makeText(this, "Location permission needed for Nearby Now", Toast.LENGTH_SHORT).show();
        }
    }
}
