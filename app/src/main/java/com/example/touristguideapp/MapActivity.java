package com.example.touristguideapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat;
    private double userLng;
    private EditText searchInput;
    private ImageButton searchBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize Views
        searchInput = findViewById(R.id.searchInput);
        searchBtn = findViewById(R.id.searchBtn);

        // Back button functionality
        ImageView btnBack = findViewById(R.id.btnMapBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup Category Buttons
        setupCategoryButtons();

        // Setup Search Functionality
        setupSearch();

        // Setup Map Type Toggle
        setupMapTypeToggle();

        // Request runtime permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void setupMapTypeToggle() {
        MaterialButton btnMap = findViewById(R.id.btnMap);
        MaterialButton btnSatellite = findViewById(R.id.btnSatellite);

        if (btnMap != null) {
            btnMap.setOnClickListener(v -> {
                if (mMap != null) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
            });
        }

        if (btnSatellite != null) {
            btnSatellite.setOnClickListener(v -> {
                if (mMap != null) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
            });
        }
    }

    private void setupSearch() {
        searchBtn.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                searchPlaces(query);
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchPlaces(query);
                }
                return true;
            }
            return false;
        });
    }

    private void setupCategoryButtons() {
        MaterialButton btnRestaurant = findViewById(R.id.btnRestaurant);
        MaterialButton btnCafe = findViewById(R.id.btnCafe);
        MaterialButton btnTourist = findViewById(R.id.btnTourist);
        MaterialButton btnATM = findViewById(R.id.btnATM);
        MaterialButton btnGas = findViewById(R.id.btnGas);

        if (btnRestaurant != null) btnRestaurant.setOnClickListener(v -> fetchPlacesByCategory("restaurant", userLat, userLng));
        if (btnCafe != null) btnCafe.setOnClickListener(v -> fetchPlacesByCategory("cafe", userLat, userLng));
        if (btnTourist != null) btnTourist.setOnClickListener(v -> fetchPlacesByCategory("tourist_attraction", userLat, userLng));
        if (btnATM != null) btnATM.setOnClickListener(v -> fetchPlacesByCategory("atm", userLat, userLng));
        if (btnGas != null) btnGas.setOnClickListener(v -> fetchPlacesByCategory("gas_station", userLat, userLng));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        double lat = getIntent().getDoubleExtra("lat", 0);
        double lng = getIntent().getDoubleExtra("lng", 0);
        String name = getIntent().getStringExtra("name");

        if (lat != 0 && lng != 0) {
            LatLng placeLocation = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions().position(placeLocation).title(name));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(placeLocation, 15));
        } else {
            // Default: center on Pune
            LatLng pune = new LatLng(18.5204, 73.8567);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pune, 12));
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (mMap != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    getUserLocation();
                }
            }
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        
                        // Only fetch default if we didn't come from a specific place
                        if (getIntent().getDoubleExtra("lat", 0) == 0) {
                            fetchPlacesByCategory("tourist_attraction", userLat, userLng);
                        }
                    }
                });
    }

    private void searchPlaces(String query) {
        if (mMap == null) return;

        mMap.clear();

        // Re-add user marker
        LatLng userLocation = new LatLng(userLat, userLng);
        mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));

        String apiKey = "YOUR_API_KEY"; // Replace with actual key
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json"
                + "?query=" + query
                + "&location=" + userLat + "," + userLng
                + "&radius=5000"
                + "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < results.length() && i < 10; i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String name = obj.getString("name");
                            JSONObject locObj = obj.getJSONObject("geometry")
                                    .getJSONObject("location");

                            double latPlace = locObj.getDouble("lat");
                            double lngPlace = locObj.getDouble("lng");

                            LatLng placeLatLng = new LatLng(latPlace, lngPlace);
                            mMap.addMarker(new MarkerOptions()
                                    .position(placeLatLng)
                                    .title(name));

                            // Move camera to first result
                            if (i == 0) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, 14));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, error -> error.printStackTrace());

        queue.add(request);
    }

    private void fetchPlacesByCategory(String type, double lat, double lng) {
        if (mMap == null) return;
        
        mMap.clear();
        
        // Re-add user marker
        LatLng userLocation = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));

        String apiKey = "YOUR_API_KEY"; // Replace with actual key
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=3000"
                + "&type=" + type
                + "&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < results.length() && i < 10; i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String name = obj.getString("name");
                            JSONObject locObj = obj.getJSONObject("geometry")
                                    .getJSONObject("location");

                            double latPlace = locObj.getDouble("lat");
                            double lngPlace = locObj.getDouble("lng");

                            LatLng placeLatLng = new LatLng(latPlace, lngPlace);
                            mMap.addMarker(new MarkerOptions()
                                    .position(placeLatLng)
                                    .title(name));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, error -> error.printStackTrace());

        queue.add(request);
    }
}
