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
import com.arriva.touristguideapp.data.places.PlaceRepository;
import com.arriva.touristguideapp.data.places.PlaceDto;
import com.arriva.touristguideapp.data.places.PlaceMapper;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private EditText searchInput;
    private ImageButton searchBtn;
    private ImageView micBtn, ivProfileIcon;
    private View btnProfile;
    private Button btnDirections;
    private MyLocationNewOverlay locationOverlay;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static final int SPEECH_REQUEST_CODE = 100;
    private GeoPoint searchedPoint;

    // Bottom Sheet
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView placeNameTv, placeDetailsTv;
    private Button btnSheetGo;
    private PlaceRepository placeRepository;
    private List<Place> allTouristPlaces = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load OSM config
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osm", MODE_PRIVATE)
        );

        setContentView(R.layout.activity_map);

        // Initialize UI
        searchInput = findViewById(R.id.searchInput);
        searchBtn = findViewById(R.id.searchBtn);
        micBtn = findViewById(R.id.micBtn);
        btnDirections = findViewById(R.id.btnDirections);
        btnProfile = findViewById(R.id.btnProfile);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);

        setupBottomSheet();
        setupCategoryButtons();
        loadProfileImage();

        // Initialize Map
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        IMapController controller = map.getController();
        controller.setZoom(15.0);

        GeoPoint startPoint = new GeoPoint(18.5204, 73.8567);
        controller.setCenter(startPoint);

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

        btnProfile.setOnClickListener(v -> {
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
    }

    private void setupCategoryButtons() {
        findViewById(R.id.btnAtm).setOnClickListener(v -> loadCategoryMarkers("atm"));
        findViewById(R.id.btnCafe).setOnClickListener(v -> loadCategoryMarkers("cafe"));
        findViewById(R.id.btnRestaurant).setOnClickListener(v -> loadCategoryMarkers("restaurant"));
        findViewById(R.id.btnBank).setOnClickListener(v -> loadCategoryMarkers("bank"));
        findViewById(R.id.btnFuel).setOnClickListener(v -> loadCategoryMarkers("fuel"));
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
                String query = "[out:json];node[\"amenity\"=\"" + type + "\"](around:3000," + center.getLatitude() + "," + center.getLongitude() + ");out;";
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
                    map.getOverlays().removeIf(o -> o instanceof org.osmdroid.views.overlay.Marker && !((org.osmdroid.views.overlay.Marker)o).getTitle().equals(searchInput.getText().toString()));
                    for (int i = 0; i < points.size(); i++) {
                        addMarker(points.get(i), names.get(i), "Category: " + type, android.R.drawable.btn_star);
                    }
                    map.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to load " + type, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void addMarker(GeoPoint point, String title, String snippet, int iconRes) {
        org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(map);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setSnippet(snippet);
        marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(iconRes));
        
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
            marker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_mapmode));
            
            marker.setOnMarkerClickListener((m, mapView) -> {
                searchedPoint = (GeoPoint) m.getPosition();
                showPlaceInfo(m.getTitle(), m.getSnippet(), p);
                map.getController().animateTo(m.getPosition());
                return true;
            });
            
            map.getOverlays().add(marker);
        }
        map.invalidate();
        Log.d("MapActivity", "MAP_MARKERS_RENDERED count=" + allTouristPlaces.size());
    }

    private void showPlaceInfo(String name, String details, Place place) {
        placeNameTv.setText(name);
        placeDetailsTv.setText(details);
        
        btnSheetGo.setText("View Details");
        btnSheetGo.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlaceDetailsActivity.class);
            PlaceIntentExtras.putPlaceDetails(intent, place);
            startActivity(intent);
        });

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
        new Thread(() -> {
            try {
                String urlString = "https://router.project-osrm.org/route/v1/driving/"
                        + start.getLongitude() + "," + start.getLatitude() + ";"
                        + end.getLongitude() + "," + end.getLatitude()
                        + "?overview=full&geometries=geojson";

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder jsonResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) jsonResponse.append(line);
                reader.close();

                JSONObject obj = new JSONObject(jsonResponse.toString());
                JSONArray routes = obj.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONArray coordinates = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                    ArrayList<GeoPoint> points = new ArrayList<>();
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray p = coordinates.getJSONArray(i);
                        points.add(new GeoPoint(p.getDouble(1), p.getDouble(0)));
                    }
                    runOnUiThread(() -> animateRoute(points));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void animateRoute(ArrayList<GeoPoint> points) {
        map.getOverlays().removeIf(overlay -> overlay instanceof Polyline);
        Polyline line = new Polyline();
        line.getOutlinePaint().setColor(0xFF7B1FA2);
        line.getOutlinePaint().setStrokeWidth(12f);
        map.getOverlays().add(line);

        Handler handler = new Handler(Looper.getMainLooper());
        final int[] index = {0};
        
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < points.size()) {
                    line.addPoint(points.get(index[0]));
                    map.invalidate();
                    index[0]++;
                    handler.postDelayed(this, 30);
                }
            }
        };
        handler.post(runnable);
    }

    private void initLocationOverlay() {
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        locationOverlay.runOnFirstFix(() -> {
            final GeoPoint myLocation = locationOverlay.getMyLocation();
            if (myLocation != null) {
                runOnUiThread(() -> {
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
        if (map != null) map.onResume();
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
