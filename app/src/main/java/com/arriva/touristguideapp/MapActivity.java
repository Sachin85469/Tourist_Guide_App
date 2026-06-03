package com.arriva.touristguideapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

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
import java.util.List;
import java.util.Locale;

import com.arriva.touristguideapp.data.places.PlaceRepository;
import com.arriva.touristguideapp.data.places.PlaceDto;
import com.arriva.touristguideapp.data.places.PlaceMapper;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.BoundingBox;

public class MapActivity extends BaseActivity {

    private MapView map;
    private EditText searchInput;
    private ImageView searchBtn;
    private ImageView micBtn, ivProfileIcon;
    private Button btnDirections;
    private MyLocationNewOverlay locationOverlay;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static final int SPEECH_REQUEST_CODE = 100;
    private GeoPoint searchedPoint;
    private org.osmdroid.views.overlay.Marker selectedMarker;
    private Polyline currentRoutePolyline;

    // Bottom Sheet
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView placeNameTv, placeDetailsTv, tvPlaceCategory, tvPlaceRating, tvPlaceDistance;
    private ImageView ivPlaceImage;
    private Button btnSheetGo, btnViewDetails;
    
    // Route UI
    private View routeSummaryCard;
    private TextView tvRouteTime, tvRouteDistance, tvRouteTitle;
    private View btnCancelRoute, btnStartNavigation;
    
    private PlaceRepository placeRepository;
    private List<Place> allTouristPlaces = new ArrayList<>();

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
        searchInput = findViewById(R.id.searchInput);
        searchBtn = findViewById(R.id.searchBtn);
        micBtn = findViewById(R.id.micBtn);
        btnDirections = findViewById(R.id.btnDirections);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);

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
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.03f : 1f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(180).start();
        });

        searchBtn.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                searchLocation(query);
            } else {
                Toast.makeText(this, "Enter a place name", Toast.LENGTH_SHORT).show();
            }
        });

        micBtn.setOnClickListener(v -> startVoiceSearch());

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

        placeRepository = new PlaceRepository();
        loadAllTouristPlaces();

        // Request permissions and init location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
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
        View chipTemple = findViewById(R.id.chipTemple);
        if (chipTemple != null) chipTemple.setOnClickListener(v -> loadCategoryMarkers("temple"));

        View chipFort = findViewById(R.id.chipFort);
        if (chipFort != null) chipFort.setOnClickListener(v -> loadCategoryMarkers("fort"));

        View chipNature = findViewById(R.id.chipNature);
        if (chipNature != null) chipNature.setOnClickListener(v -> loadCategoryMarkers("nature"));

        View chipMuseum = findViewById(R.id.chipMuseum);
        if (chipMuseum != null) chipMuseum.setOnClickListener(v -> loadCategoryMarkers("museum"));
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
        
        Uri uri = Uri.parse("google.navigation:q=" + searchedPoint.getLatitude() + "," + searchedPoint.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
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

                addMarker(searchedPoint, locationName, "Searched Result", android.R.drawable.ic_menu_mylocation);
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
                    case "temple": osmType = "node[\"amenity\"=\"place_of_worship\"]"; break;
                    case "fort": osmType = "node[\"historic\"=\"castle\"]"; break;
                    case "nature": osmType = "node[\"leisure\"=\"park\"]"; break;
                    case "museum": osmType = "node[\"tourism\"=\"museum\"]"; break;
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
                    for (int i = 0; i < points.size(); i++) {
                        addMarker(points.get(i), names.get(i), "Category: " + type, R.drawable.ic_map_marker_historical);
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
        org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(map);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setSnippet(snippet);
        marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        marker.setIcon(androidx.core.content.ContextCompat.getDrawable(this, iconRes));
        
        marker.setOnMarkerClickListener((m, mapView) -> {
            searchedPoint = (GeoPoint) m.getPosition();
            showPlaceInfo(m.getTitle(), m.getSnippet());
            map.getController().animateTo(m.getPosition());
            return true;
        });
        
        map.getOverlays().add(marker);
        map.invalidate();
    }

    private void loadAllTouristPlaces() {
        placeRepository.fetchPublishedPlaces((places, origin, message) -> {
            if (!places.isEmpty()) {
                allTouristPlaces = places;
                renderPlaceMarkers();
            }
        });
    }

    private void renderPlaceMarkers() {
        // Remove existing tourist markers, keep location overlay and search markers
        map.getOverlays().removeIf(o -> o instanceof org.osmdroid.views.overlay.Marker && 
                !((org.osmdroid.views.overlay.Marker)o).getTitle().equals(searchInput.getText().toString()));

        for (Place p : allTouristPlaces) {
            GeoPoint point = new GeoPoint(p.getLat(), p.getLng());
            org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(map);
            marker.setPosition(point);
            marker.setTitle(p.getName());
            marker.setSnippet(p.getCategory() + " - " + p.getCity());
            marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
            
            int iconRes = getMarkerIconForCategory(p.getCategory());
            marker.setIcon(androidx.core.content.ContextCompat.getDrawable(this, iconRes));
            
            marker.setOnMarkerClickListener((m, mapView) -> {
                selectMarker(m, p);
                return true;
            });
            
            map.getOverlays().add(marker);
        }
        map.invalidate();
        Log.d("MapActivity", "MAP_MARKERS_RENDERED count=" + allTouristPlaces.size());
    }

    private int getMarkerIconForCategory(String category) {
        if (category == null) return R.drawable.ic_map_marker_historical;
        String cat = category.toLowerCase();
        if (cat.contains("temple")) return R.drawable.ic_map_marker_temple;
        if (cat.contains("fort")) return R.drawable.ic_map_marker_fort;
        if (cat.contains("museum")) return R.drawable.ic_map_marker_museum;
        if (cat.contains("nature") || cat.contains("park") || cat.contains("hill")) return R.drawable.ic_map_marker_nature;
        if (cat.contains("food") || cat.contains("restaurant") || cat.contains("cafe")) return R.drawable.ic_map_marker_food;
        if (cat.contains("shop") || cat.contains("mall") || cat.contains("market")) return R.drawable.ic_map_marker_shopping;
        if (cat.contains("adventure") || cat.contains("trek") || cat.contains("sport")) return R.drawable.ic_map_marker_adventure;
        return R.drawable.ic_map_marker_historical;
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
        
        // Animate
        marker.setAlpha(0.5f);
        new Handler().postDelayed(() -> marker.setAlpha(1.0f), 150);

        searchedPoint = marker.getPosition();
        showPlaceInfo(marker.getTitle(), p);
        map.getController().animateTo(marker.getPosition());
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

        if (ivPlaceImage != null && place.getImageUrl() != null) {
            Glide.with(this).load(place.getImageUrl()).into(ivPlaceImage);
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

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showPlaceInfo(String name, String details) {
        placeNameTv.setText(name);
        placeDetailsTv.setText(details);
        btnSheetGo.setText("Get Directions");
        btnSheetGo.setOnClickListener(v -> getDirectionsToSearched());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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

    private void initLocationOverlay() {
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
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
        map.getOverlays().add(locationOverlay);
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
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initLocationOverlay();
        }
    }
}
