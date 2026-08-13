package com.arriva.touristguideapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.arriva.touristguideapp.data.places.PlaceRepository;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.BoundingBox;

public class MapActivity extends BaseActivity {

    private MapView map;
    private EditText searchInput;
    private ImageView searchBtn, clearSearchBtn;
    private ImageView micBtn, ivProfileIcon;
    private Button btnDirections;
    private FloatingActionButton fabMyLocation;
    private MyLocationNewOverlay locationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static final int SPEECH_REQUEST_CODE = 100;
    private GeoPoint searchedPoint;
    private org.osmdroid.views.overlay.Marker selectedMarker;
    private org.osmdroid.views.overlay.Marker searchMarker;
    private Polyline currentRoutePolyline;
    private boolean centerOnLocationAfterPermission = false;

    // Bottom Sheet
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View bottomSheetView;
    private TextView placeNameTv, placeDetailsTv, tvPlaceCategory, tvPlaceRating, tvPlaceDistance;
    private ImageView ivPlaceImage;
    private Button btnSheetGo, btnViewDetails;
    private View offlineCacheBanner;
    
    // Route UI
    private View routeSummaryCard;
    private TextView tvRouteTime, tvRouteDistance, tvRouteTitle;
    private View btnCancelRoute, btnStartNavigation;
    
    private PlaceRepository placeRepository;
    private List<Place> allTouristPlaces = new ArrayList<>();
    private final Map<org.osmdroid.views.overlay.Marker, String> touristMarkers = new LinkedHashMap<>();
    private final List<MapCategoryChip> categoryChipViews = new ArrayList<>();
    private String selectedCategory = "All";
    private boolean offlineCacheBannerDismissed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("OSM_DEBUG", "Activity Created");

        // Load OSM config
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osm", MODE_PRIVATE)
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        // Ensure tiles are stored in internal storage to avoid permission issues with external storage
        java.io.File osmConfig = new java.io.File(getFilesDir(), "osmdroid");
        if (!osmConfig.exists()) osmConfig.mkdirs();
        Configuration.getInstance().setOsmdroidBasePath(osmConfig);
        java.io.File tileCache = new java.io.File(osmConfig, "tiles");
        if (!tileCache.exists()) tileCache.mkdirs();
        Configuration.getInstance().setOsmdroidTileCache(tileCache);

        android.util.Log.d("OSM_DEBUG", "OSM Initialized with UserAgent=" + getPackageName());

        setContentView(R.layout.activity_map);
        android.util.Log.d("OSM_DEBUG", "Layout Loaded");

        // Initialize UI
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        searchInput = findViewById(R.id.searchInput);
        searchBtn = findViewById(R.id.searchBtn);
        clearSearchBtn = findViewById(R.id.clearSearchBtn);
        micBtn = findViewById(R.id.micBtn);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        btnDirections = findViewById(R.id.btnDirections);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);
        offlineCacheBanner = findViewById(R.id.offlineCacheBanner);
        View btnDismissOfflineBanner = findViewById(R.id.btnDismissOfflineBanner);
        if (btnDismissOfflineBanner != null) {
            btnDismissOfflineBanner.setOnClickListener(v -> {
                offlineCacheBannerDismissed = true;
                showOfflineCacheBanner(false);
            });
        }

        setupBottomNavigation();
        setupBottomSheet();
        setupCategoryButtons();
        loadProfileImage();

        // Initialize Map
        map = findViewById(R.id.map);
        if (map != null) {
            android.util.Log.d("OSM_DEBUG", "MapView Found");
            map.setTileSource(TileSourceFactory.MAPNIK);
            android.util.Log.d("OSM_DEBUG", "Tile source applied: MAPNIK");
            map.setMultiTouchControls(true);

            IMapController controller = map.getController();
            controller.setZoom(15.0);

            GeoPoint startPoint = new GeoPoint(18.5204, 73.8567);
            controller.setCenter(startPoint);
            android.util.Log.d("OSM_DEBUG", "Map Center set to Pune");
        } else {
            android.util.Log.e("OSM_DEBUG", "MapView is NULL! Check layout R.layout.activity_map");
        }

        // Search bar animation
        View searchCard = findViewById(R.id.searchCard);
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.03f : 1f;
            View target = searchCard != null ? searchCard : v;
            target.animate().scaleX(scale).scaleY(scale).setDuration(180).start();
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (clearSearchBtn != null) {
                    clearSearchBtn.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        if (clearSearchBtn != null) {
            clearSearchBtn.setOnClickListener(v -> {
                searchInput.setText("");
                clearSearchResult();
            });
        }

        searchBtn.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                searchLocation(query);
            } else {
                Toast.makeText(this, "Enter a place name", Toast.LENGTH_SHORT).show();
            }
        });

        micBtn.setOnClickListener(v -> startVoiceSearch());

        if (fabMyLocation != null) {
            fabMyLocation.setOnClickListener(v -> handleMyLocationClick());
        }

        ivProfileIcon.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                startActivity(new Intent(MapActivity.this, ProfileActivity.class));
            }).start();
        });

        // Button click animation
        btnDirections.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    break;
            }
            return false;
        });

        btnDirections.setOnClickListener(v -> getDirectionsToSearched());

        btnSheetGo.setOnClickListener(v -> getDirectionsToSearched());

        placeRepository = new PlaceRepository(this);
        loadAllTouristPlaces();

        if (hasLocationPermission()) {
            initLocationOverlay();
        }
    }

    private void setupBottomNavigation() {
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_map);
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_favorites) {
                    startActivity(new Intent(this, FavoritesActivity.class));
                    finish();
                    return true;
                }
                return id == R.id.nav_map;
            });
        }
    }

    private void loadProfileImage() {
        ProfileUtils.loadAvatar(this, ivProfileIcon);
    }

    private void setupBottomSheet() {
        View bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetView = bottomSheet;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        
        placeNameTv = findViewById(R.id.placeName);
        placeDetailsTv = findViewById(R.id.placeDetails);
        btnSheetGo = findViewById(R.id.btnSheetGo);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        tvPlaceCategory = findViewById(R.id.tvPlaceCategory);
        tvPlaceRating = findViewById(R.id.tvPlaceRating);
        tvPlaceDistance = findViewById(R.id.tvPlaceDistance);
        ivPlaceImage = findViewById(R.id.ivPlaceImage);
        
        // Route UI
        routeSummaryCard = findViewById(R.id.routeSummaryCard);
        tvRouteTime = findViewById(R.id.tvRouteTime);
        tvRouteDistance = findViewById(R.id.tvRouteDistance);
        tvRouteTitle = findViewById(R.id.tvRouteTitle);
        btnCancelRoute = findViewById(R.id.btnCancelRoute);
        btnStartNavigation = findViewById(R.id.btnStartNavigation);

        if (btnCancelRoute != null) {
            btnCancelRoute.setOnClickListener(v -> clearRoute());
        }
        if (btnStartNavigation != null) {
            btnStartNavigation.setOnClickListener(v -> startNavigationForCurrentRoute());
        }
    }

    /** Starts the in-app navigation session for the route currently shown on this map. */
    private void startNavigationForCurrentRoute() {
        if (searchedPoint == null) {
            Toast.makeText(this, "Choose a destination before starting navigation", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, RouteActivity.class);
        intent.putExtra("destLat", searchedPoint.getLatitude());
        intent.putExtra("destLng", searchedPoint.getLongitude());
        intent.putExtra("destName", selectedMarker != null ? selectedMarker.getTitle() : "Destination");
        intent.putExtra("startNavigation", true);
        startActivity(intent);
    }

    private void clearRoute() {
        if (currentRoutePolyline != null) {
            map.getOverlays().remove(currentRoutePolyline);
            currentRoutePolyline = null;
        }
        if (routeSummaryCard != null) routeSummaryCard.setVisibility(View.GONE);
        map.invalidate();
    }

    private void setupCategoryButtons() {
        categoryChipViews.clear();
        registerCategoryChip("All", R.id.chipAll, R.id.chipAllContent, R.id.chipAllIcon, R.id.chipAllLabel, R.color.color_primary);
        registerCategoryChip("Historical", R.id.chipHistorical, R.id.chipHistoricalContent, R.id.chipHistoricalIcon, R.id.chipHistoricalLabel, R.color.marker_historical);
        registerCategoryChip("Nature", R.id.chipNature, R.id.chipNatureContent, R.id.chipNatureIcon, R.id.chipNatureLabel, R.color.marker_nature);
        registerCategoryChip("Religious", R.id.chipReligious, R.id.chipReligiousContent, R.id.chipReligiousIcon, R.id.chipReligiousLabel, R.color.marker_religious);
        registerCategoryChip("Food", R.id.chipFood, R.id.chipFoodContent, R.id.chipFoodIcon, R.id.chipFoodLabel, R.color.marker_food);
        registerCategoryChip("Culture", R.id.chipCulture, R.id.chipCultureContent, R.id.chipCultureIcon, R.id.chipCultureLabel, R.color.marker_culture);
        registerCategoryChip("Adventure", R.id.chipAdventure, R.id.chipAdventureContent, R.id.chipAdventureIcon, R.id.chipAdventureLabel, R.color.marker_adventure);
        registerCategoryChip("Scenic", R.id.chipScenic, R.id.chipScenicContent, R.id.chipScenicIcon, R.id.chipScenicLabel, R.color.marker_scenic);
        registerCategoryChip("Shopping", R.id.chipShopping, R.id.chipShoppingContent, R.id.chipShoppingIcon, R.id.chipShoppingLabel, R.color.marker_shopping);
        registerCategoryChip("Educational", R.id.chipEducational, R.id.chipEducationalContent, R.id.chipEducationalIcon, R.id.chipEducationalLabel, R.color.marker_educational);
        registerCategoryChip("Park", R.id.chipPark, R.id.chipParkContent, R.id.chipParkIcon, R.id.chipParkLabel, R.color.marker_park);
        updateCategoryChipStates(selectedCategory);
    }

    private void registerCategoryChip(String category, int cardId, int contentId, int iconId, int labelId, int colorRes) {
        View root = findViewById(cardId);
        View content = findViewById(contentId);
        ImageView icon = findViewById(iconId);
        TextView label = findViewById(labelId);
        if (!(root instanceof MaterialCardView) || content == null || icon == null || label == null) {
            return;
        }

        MapCategoryChip chip = new MapCategoryChip(category, (MaterialCardView) root, content, icon, label, colorRes);
        categoryChipViews.add(chip);
        root.setOnClickListener(v -> {
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
            filterMarkers(category);
        });
    }

    private void updateCategoryChipStates(String selected) {
        int white = ContextCompat.getColor(this, R.color.white);
        int textColor = ContextCompat.getColor(this, R.color.m3_on_surface);
        int strokeColor = ContextCompat.getColor(this, R.color.m3_outline_variant);

        for (MapCategoryChip chip : categoryChipViews) {
            boolean isSelected = chip.category.equalsIgnoreCase(selected);
            chip.content.setBackgroundResource(isSelected ? R.drawable.bg_map_chip_selected : R.drawable.bg_map_chip_unselected);
            chip.card.setCardBackgroundColor(Color.TRANSPARENT);
            chip.card.setStrokeWidth(isSelected ? 0 : dp(1));
            chip.card.setStrokeColor(strokeColor);
            chip.card.setCardElevation(dp(isSelected ? 10 : 4));
            chip.icon.setColorFilter(isSelected ? white : ContextCompat.getColor(this, chip.colorRes));
            chip.label.setTextColor(isSelected ? white : textColor);
            chip.card.animate().scaleX(isSelected ? 1.04f : 1f).scaleY(isSelected ? 1.04f : 1f).setDuration(180).start();
        }
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the place name...");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String voiceQuery = result.get(0);
                searchInput.setText(voiceQuery);
                searchLocation(voiceQuery);
            }
        }
    }

    private void getDirectionsToSearched() {
        if (searchedPoint == null) {
            Toast.makeText(this, "Search a place first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, RouteActivity.class);
        intent.putExtra("destLat", searchedPoint.getLatitude());
        intent.putExtra("destLng", searchedPoint.getLongitude());
        intent.putExtra("destName", selectedMarker != null ? selectedMarker.getTitle() : "Searched Location");
        startActivity(intent);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void searchLocation(String locationName) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                searchedPoint = new GeoPoint(address.getLatitude(), address.getLongitude());
                
                map.getController().setZoom(17.0);
                map.getController().animateTo(searchedPoint);

                addMarker(searchedPoint, locationName, "Searched Result", R.drawable.ic_marker_default_purple);
                showPlaceInfo(locationName, "Latitude: " + address.getLatitude() + "\nLongitude: " + address.getLongitude());

            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadCategoryMarkers(String type) {
        Toast.makeText(this, "Searching for " + type + "...", Toast.LENGTH_SHORT).show();
        GeoPoint center = (GeoPoint) map.getMapCenter();
        
        new Thread(() -> {
            try {
                String osmType;
                switch (type) {
                    case "historical": osmType = "node[\"historic\"]"; break;
                    case "nature": osmType = "node[\"leisure\"=\"park\"]"; break;
                    case "religious": osmType = "node[\"amenity\"=\"place_of_worship\"]"; break;
                    case "food": osmType = "node[\"amenity\"~\"restaurant|cafe\"]"; break;
                    case "culture": osmType = "node[\"tourism\"=\"museum\"]"; break;
                    case "adventure": osmType = "node[\"tourism\"=\"viewpoint\"]"; break;
                    case "scenic": osmType = "node[\"tourism\"=\"viewpoint\"]"; break;
                    case "shopping": osmType = "node[\"shop\"]"; break;
                    case "educational": osmType = "node[\"amenity\"~\"school|college|university\"]"; break;
                    case "park": osmType = "node[\"leisure\"=\"park\"]"; break;
                    default: osmType = "node[\"amenity\"=\"" + type + "\"]"; break;
                }
                String query = "[out:json];" + osmType + "(around:5000," + center.getLatitude() + "," + center.getLongitude() + ");out;";
                String urlString = "https://overpass-api.de/api/interpreter?data=" + java.net.URLEncoder.encode(query, "UTF-8");
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray elements = json.getJSONArray("elements");

                List<GeoPoint> points = new ArrayList<>();
                List<String> names = new ArrayList<>();

                for (int i = 0; i < elements.length(); i++) {
                    JSONObject node = elements.getJSONObject(i);
                    points.add(new GeoPoint(node.getDouble("lat"), node.getDouble("lon")));
                    String name = node.optJSONObject("tags") != null ? node.getJSONObject("tags").optString("name", type) : type;
                    names.add(name);
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    map.getOverlays().removeIf(o -> o instanceof org.osmdroid.views.overlay.Marker && !((org.osmdroid.views.overlay.Marker)o).getTitle().equals(searchInput.getText().toString()));
                    
                    if (points.isEmpty()) {
                        View emptyView = findViewById(R.id.emptyStateExplore);
                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> emptyView.setVisibility(View.GONE), 3000);
                        }
                    }

                    for (int i = 0; i < points.size(); i++) {
                        addMarker(points.get(i), names.get(i), "Category: " + type, getMarkerIconForCategory(type));
                    }
                    map.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Failed to load " + type, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void addMarker(GeoPoint point, String title, String snippet, int iconRes) {
        if ("Searched Result".equals(snippet) && searchMarker != null) {
            map.getOverlays().remove(searchMarker);
            searchMarker = null;
        }

        org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(map);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setSnippet(snippet);
        marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        marker.setIcon(androidx.core.content.ContextCompat.getDrawable(this, iconRes));

        if ("Searched Result".equals(snippet)) {
            searchMarker = marker;
        }
        
        marker.setOnMarkerClickListener((m, mapView) -> {
            selectedMarker = m;
            searchedPoint = (GeoPoint) m.getPosition();
            showPlaceInfo(m.getTitle(), m.getSnippet());
            map.getController().animateTo(m.getPosition());
            return true;
        });
        
        map.getOverlays().add(marker);
        map.invalidate();
    }

    private void clearSearchResult() {
        if (searchMarker != null && map != null) {
            map.getOverlays().remove(searchMarker);
            if (selectedMarker == searchMarker) {
                selectedMarker = null;
            }
            searchMarker = null;
            searchedPoint = null;
            map.invalidate();
        }

        if (selectedMarker == null && bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void loadAllTouristPlaces() {
        placeRepository.getPlacesOfflineFirst(null, null, (places, origin, cacheEmpty, message) -> {
            showOfflineCacheBanner(origin == PlaceRepository.DataOrigin.ROOM_CACHE && !cacheEmpty);
            if (!places.isEmpty()) {
                allTouristPlaces = places;
                renderPlaceMarkers();
            }
        });
    }

    private void showOfflineCacheBanner(boolean show) {
        if (offlineCacheBanner != null) {
            offlineCacheBanner.setVisibility(show && !offlineCacheBannerDismissed ? View.VISIBLE : View.GONE);
        }
    }

    private void renderPlaceMarkers() {
        boolean selectedTouristMarkerWasRemoved = selectedMarker != null && touristMarkers.containsKey(selectedMarker);
        for (org.osmdroid.views.overlay.Marker marker : new ArrayList<>(touristMarkers.keySet())) {
            map.getOverlays().remove(marker);
        }
        touristMarkers.clear();
        if (selectedTouristMarkerWasRemoved) {
            selectedMarker = null;
            searchedPoint = null;
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        }

        Handler markerAnimator = new Handler(Looper.getMainLooper());
        int index = 0;
        for (Place p : allTouristPlaces) {
            GeoPoint point = new GeoPoint(p.getLat(), p.getLng());
            org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(map);
            marker.setPosition(point);
            marker.setTitle(p.getName());
            marker.setSnippet(p.getCategory() + " - " + p.getCity());
            marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
            
            int iconRes = getMarkerIconForCategory(p.getCategory());
            marker.setIcon(androidx.core.content.ContextCompat.getDrawable(this, iconRes));
            marker.setRelatedObject(p);
            marker.setAlpha(0f);
            
            marker.setOnMarkerClickListener((m, mapView) -> {
                selectMarker(m, p);
                return true;
            });
            
            map.getOverlays().add(marker);
            touristMarkers.put(marker, normalizeFilterCategory(p.getCategory()));

            long delay = Math.min(index * 32L, 480L);
            markerAnimator.postDelayed(() -> {
                marker.setAlpha(1f);
                map.invalidate();
            }, delay);
            index++;
        }
        filterMarkers(selectedCategory);
        map.invalidate();
        Log.d("MapActivity", "MAP_MARKERS_RENDERED count=" + allTouristPlaces.size());
    }

    private int getMarkerIconForCategory(String category) {
        if (category == null) return R.drawable.ic_marker_default_purple;
        String cat = category.toLowerCase();
        if (cat.contains("park") || cat.contains("garden") || cat.contains("zoo") || cat.contains("baug")) {
            return R.drawable.ic_marker_park_green;
        }
        if (cat.contains("education") || cat.contains("college") || cat.contains("university") || cat.contains("school")) {
            return R.drawable.ic_marker_educational_slate;
        }
        if (cat.contains("shopping") || cat.contains("mall") || cat.contains("market")) {
            return R.drawable.ic_marker_shopping_pink;
        }
        if (cat.contains("scenic") || cat.contains("view") || cat.contains("lake") || cat.contains("dam")) {
            return R.drawable.ic_marker_scenic_blue;
        }
        if (cat.contains("culture") || cat.contains("art") || cat.contains("museum")) {
            return R.drawable.ic_marker_culture_purple;
        }
        if (cat.contains("historical") || cat.contains("history") || cat.contains("fort") || cat.contains("palace") || cat.contains("wada")) {
            return R.drawable.ic_marker_historical_amber;
        }
        if (cat.contains("nature") || cat.contains("hill") || cat.contains("forest")) {
            return R.drawable.ic_marker_nature_green;
        }
        if (cat.contains("religious") || cat.contains("temple") || cat.contains("spiritual")) {
            return R.drawable.ic_marker_religious_orange;
        }
        if (cat.contains("food") || cat.contains("restaurant") || cat.contains("cafe")) {
            return R.drawable.ic_marker_food_red;
        }
        if (cat.contains("adventure") || cat.contains("trek") || cat.contains("sport")) {
            return R.drawable.ic_marker_adventure_blue;
        }
        return R.drawable.ic_marker_default_purple;
    }

    private String normalizeFilterCategory(String category) {
        if (category == null) return "";
        String cat = category.trim().toLowerCase(Locale.ROOT);
        if (cat.contains("park") || cat.contains("garden") || cat.contains("zoo") || cat.contains("baug")) {
            return "Park";
        }
        if (cat.contains("education") || cat.contains("college") || cat.contains("university") || cat.contains("school")) {
            return "Educational";
        }
        if (cat.contains("shopping") || cat.contains("mall") || cat.contains("market")) {
            return "Shopping";
        }
        if (cat.contains("scenic") || cat.contains("view") || cat.contains("lake") || cat.contains("dam")) {
            return "Scenic";
        }
        if (cat.contains("culture") || cat.contains("art") || cat.contains("museum")) {
            return "Culture";
        }
        if (cat.contains("historical") || cat.contains("history") || cat.contains("fort") || cat.contains("palace") || cat.contains("wada")) {
            return "Historical";
        }
        if (cat.contains("nature") || cat.contains("hill") || cat.contains("forest")) {
            return "Nature";
        }
        if (cat.contains("religious") || cat.contains("temple") || cat.contains("spiritual")) {
            return "Religious";
        }
        if (cat.contains("food") || cat.contains("restaurant") || cat.contains("cafe")) {
            return "Food";
        }
        if (cat.contains("adventure") || cat.contains("trek") || cat.contains("sport")) {
            return "Adventure";
        }
        return category.trim();
    }

    private void filterMarkers(String category) {
        selectedCategory = category;
        updateCategoryChipStates(category);
        boolean showAll = "All".equalsIgnoreCase(category);
        int visibleCount = 0;

        for (Map.Entry<org.osmdroid.views.overlay.Marker, String> entry : touristMarkers.entrySet()) {
            String markerCategory = entry.getValue();
            boolean visible = showAll
                    || (markerCategory != null && markerCategory.equalsIgnoreCase(category));
            // osmdroid's equivalent of Google Maps Marker#setVisible.
            entry.getKey().setEnabled(visible);
            if (visible) {
                visibleCount++;
            }
        }

        if (selectedMarker != null && !selectedMarker.isEnabled()) {
            selectedMarker = null;
            searchedPoint = null;
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        View emptyView = findViewById(R.id.emptyStateExplore);
        if (emptyView != null) {
            boolean showEmpty = !allTouristPlaces.isEmpty() && visibleCount == 0;
            emptyView.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
            if (showEmpty) {
                emptyView.setAlpha(0f);
                emptyView.animate().alpha(1f).setDuration(180).start();
            }
        }
        map.invalidate();
    }

    private void selectMarker(org.osmdroid.views.overlay.Marker marker, Place p) {
        // Reset previous selection
        if (selectedMarker != null) {
            Place prevPlace = (Place) selectedMarker.getRelatedObject();
            if (prevPlace != null) {
                selectedMarker.setIcon(ContextCompat.getDrawable(this, getMarkerIconForCategory(prevPlace.getCategory())));
            } else {
                selectedMarker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation));
            }
            selectedMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        }

        selectedMarker = marker;
        selectedMarker.setRelatedObject(p);
        
        // Highlight selection
        selectedMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_marker_selected));
        selectedMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        
        animateSelectedMarker(marker);

        searchedPoint = marker.getPosition();
        showPlaceInfo(marker.getTitle(), p);
        map.getController().animateTo(marker.getPosition());
    }

    private void animateSelectedMarker(org.osmdroid.views.overlay.Marker marker) {
        marker.setAlpha(0.58f);
        map.invalidate();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            marker.setAlpha(1.0f);
            map.invalidate();
        }, 130);
    }

    private void showPlaceInfo(String name, Place place) {
        placeNameTv.setText(name);
        placeDetailsTv.setText(place.getDescription());
        if (tvPlaceCategory != null) tvPlaceCategory.setText(place.getCategory());
        if (tvPlaceRating != null) tvPlaceRating.setText(String.format(java.util.Locale.getDefault(), "%.1f ⭐", place.getRating()));
        
        if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    locationOverlay.getMyLocation().getLatitude(), locationOverlay.getMyLocation().getLongitude(),
                    place.getLat(), place.getLng(), results);
            if (tvPlaceDistance != null) tvPlaceDistance.setText(String.format(java.util.Locale.getDefault(), "%.1f km away", results[0] / 1000f));
        }

        if (ivPlaceImage != null) {
            PlaceImageHelper.loadThumbnail(ivPlaceImage, place);
        }
        
        btnSheetGo.setText("Get Directions");
        btnSheetGo.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
                fetchRoute(locationOverlay.getMyLocation(), selectedMarker.getPosition());
            } else {
                getDirectionsToSearched();
            }
        });

        if (btnViewDetails != null) {
            btnViewDetails.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlaceDetailsActivity.class);
                PlaceIntentExtras.putPlaceDetails(intent, place);
                startActivity(intent);
            });
        }

        showBottomSheetWithAnimation();
    }

    private void showPlaceInfo(String name, String details) {
        placeNameTv.setText(name);
        placeDetailsTv.setText(details);
        btnSheetGo.setText("Get Directions");
        btnSheetGo.setOnClickListener(v -> getDirectionsToSearched());
        showBottomSheetWithAnimation();
    }

    private void showBottomSheetWithAnimation() {
        if (bottomSheetView != null) {
            bottomSheetView.setAlpha(0f);
            bottomSheetView.setTranslationY(dp(36));
        }
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        if (bottomSheetView != null) {
            bottomSheetView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .start();
        }
    }

    private void fetchRoute(GeoPoint start, GeoPoint end) {
        if (currentRoutePolyline != null) {
            map.getOverlays().remove(currentRoutePolyline);
        }

        new Thread(() -> {
            try {
                RoadManager roadManager = new OSRMRoadManager(this, getPackageName());
                ((OSRMRoadManager) roadManager).setMean(OSRMRoadManager.MEAN_BY_CAR);

                ArrayList<GeoPoint> waypoints = new ArrayList<>();
                waypoints.add(start);
                waypoints.add(end);

                Road road = roadManager.getRoad(waypoints);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (road.mStatus != Road.STATUS_OK) {
                        Toast.makeText(this, "Error calculating route", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentRoutePolyline = RoadManager.buildRoadOverlay(road);
                    currentRoutePolyline.getOutlinePaint().setColor(ContextCompat.getColor(this, R.color.m3_primary));
                    currentRoutePolyline.getOutlinePaint().setStrokeWidth(14f);
                    currentRoutePolyline.getOutlinePaint().setStrokeCap(android.graphics.Paint.Cap.ROUND);
                    
                    map.getOverlays().add(currentRoutePolyline);
                    
                    updateRouteInfo(road.mLength, road.mDuration);
                    
                    BoundingBox bb = BoundingBox.fromGeoPoints(road.mRouteHigh);
                    map.zoomToBoundingBox(bb, true, 150);
                    
                    map.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Route failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateRouteInfo(double km, double seconds) {
        if (routeSummaryCard == null) return;
        routeSummaryCard.setVisibility(View.VISIBLE);
        
        int mins = (int) (seconds / 60);
        String timeStr = mins < 60 ? mins + " min" : (mins / 60) + "h " + (mins % 60) + "m";
        tvRouteTime.setText(timeStr);
        
        String distStr = String.format(Locale.getDefault(), "Distance: %.1f km", km);
        tvRouteDistance.setText(distStr);
        
        if (selectedMarker != null) {
            tvRouteTitle.setText("To " + selectedMarker.getTitle());
        }
    }

    private void handleMyLocationClick() {
        if (!hasLocationPermission()) {
            centerOnLocationAfterPermission = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_PERMISSIONS_REQUEST_CODE);
            return;
        }

        if (!isGpsEnabled()) {
            promptEnableLocationServices();
            return;
        }

        recenterOnCurrentLocation();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) return false;

        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private void promptEnableLocationServices() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.map_enable_location_title)
                .setMessage(R.string.map_enable_location_message)
                .setPositiveButton(R.string.map_enable_location_settings, (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @SuppressWarnings("MissingPermission")
    private void recenterOnCurrentLocation() {
        if (map == null || fusedLocationClient == null) return;

        initLocationOverlay(false);

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        boolean[] completed = {false};

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!completed[0]) {
                completed[0] = true;
                cancellationTokenSource.cancel();
                showUnableToGetCurrentLocation();
            }
        }, 10000);

        fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (completed[0]) return;
                    completed[0] = true;

                    if (location != null) {
                        animateCameraToLocation(location);
                        return;
                    }

                    GeoPoint overlayLocation = locationOverlay != null ? locationOverlay.getMyLocation() : null;
                    if (overlayLocation != null) {
                        animateCameraToPoint(overlayLocation);
                    } else {
                        showUnableToGetCurrentLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    if (completed[0]) return;
                    completed[0] = true;

                    GeoPoint overlayLocation = locationOverlay != null ? locationOverlay.getMyLocation() : null;
                    if (overlayLocation != null) {
                        animateCameraToPoint(overlayLocation);
                    } else {
                        showUnableToGetCurrentLocation();
                    }
                });
    }

    private void animateCameraToLocation(Location location) {
        animateCameraToPoint(new GeoPoint(location.getLatitude(), location.getLongitude()));
    }

    private void animateCameraToPoint(GeoPoint point) {
        map.getController().animateTo(point, 16.0, 750L);
        map.invalidate();
    }

    private void showUnableToGetCurrentLocation() {
        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
    }

    private void initLocationOverlay() {
        initLocationOverlay(true);
    }

    private void initLocationOverlay(boolean centerOnFirstFix) {
        if (map == null) return;

        if (locationOverlay == null) {
            locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            map.getOverlays().add(locationOverlay);
        } else if (!map.getOverlays().contains(locationOverlay)) {
            map.getOverlays().add(locationOverlay);
        }

        locationOverlay.enableMyLocation();

        if (centerOnFirstFix) {
            locationOverlay.runOnFirstFix(() -> {
                final GeoPoint myLocation = locationOverlay.getMyLocation();
                if (myLocation != null) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        map.getController().animateTo(myLocation);
                        map.getController().setZoom(15.0);
                    });
                }
            });
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class MapCategoryChip {
        final String category;
        final MaterialCardView card;
        final View content;
        final ImageView icon;
        final TextView label;
        final int colorRes;

        MapCategoryChip(String category, MaterialCardView card, View content, ImageView icon, TextView label, int colorRes) {
            this.category = category;
            this.card = card;
            this.content = content;
            this.icon = icon;
            this.label = label;
            this.colorRes = colorRes;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("OSM_DEBUG", "Activity Resumed");
        if (map != null) {
            map.onResume();
            android.util.Log.d("OSM_DEBUG", "MapView Resumed");
        }
        if (locationOverlay != null) locationOverlay.enableMyLocation();
        loadProfileImage(); // Refresh in case name/photo changed
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        if (locationOverlay != null) locationOverlay.disableMyLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (hasLocationPermission()) {
                initLocationOverlay(!centerOnLocationAfterPermission);
                if (centerOnLocationAfterPermission) {
                    centerOnLocationAfterPermission = false;
                    handleMyLocationClick();
                }
            } else {
                centerOnLocationAfterPermission = false;
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
