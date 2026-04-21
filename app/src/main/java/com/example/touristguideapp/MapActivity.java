package com.example.touristguideapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private EditText searchInput;
    private ImageButton searchBtn;
    private Button btnDirections;
    private MyLocationNewOverlay locationOverlay;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private GeoPoint searchedPoint;

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
        btnDirections = findViewById(R.id.btnDirections);

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

        btnDirections.setOnClickListener(v -> {
            if (searchedPoint == null) {
                Toast.makeText(this, "Search a place first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (locationOverlay == null || locationOverlay.getMyLocation() == null) {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
                return;
            }

            GeoPoint start = locationOverlay.getMyLocation();
            fetchRoute(start, searchedPoint);
        });

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

    private void searchLocation(String locationName) {

        Geocoder geocoder = new Geocoder(this);

        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);

            if (addressList != null && !addressList.isEmpty()) {

                Address address = addressList.get(0);

                double lat = address.getLatitude();
                double lon = address.getLongitude();

                searchedPoint = new GeoPoint(lat, lon);

                // Move map smoothly
                map.getController().setZoom(15.0);
                map.getController().animateTo(searchedPoint);

                // Improve Marker
                map.getOverlays().removeIf(o -> o instanceof org.osmdroid.views.overlay.Marker);

                org.osmdroid.views.overlay.Marker marker =
                        new org.osmdroid.views.overlay.Marker(map);

                marker.setPosition(searchedPoint);
                marker.setTitle(locationName);

                marker.setAnchor(
                        org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                        org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM
                );

                marker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_mylocation));

                marker.setOnMarkerClickListener((m, mapView) -> {
                    Toast.makeText(this, locationName, Toast.LENGTH_SHORT).show();
                    return true;
                });

                map.getOverlays().add(marker);
                map.invalidate();

            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Search error", Toast.LENGTH_SHORT).show();
        }

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
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );

                StringBuilder json = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                reader.close();

                JSONObject obj = new JSONObject(json.toString());
                JSONArray routes = obj.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONObject geometry = routes.getJSONObject(0)
                            .getJSONObject("geometry");

                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    ArrayList<GeoPoint> points = new ArrayList<>();

                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray point = coordinates.getJSONArray(i);

                        double lon = point.getDouble(0);
                        double lat = point.getDouble(1);

                        points.add(new GeoPoint(lat, lon));
                    }

                    runOnUiThread(() -> drawRoute(points));
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Routing error", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void drawRoute(ArrayList<GeoPoint> points) {
        map.getOverlays().removeIf(overlay -> overlay instanceof Polyline);

        Polyline line = new Polyline();
        line.setPoints(points);
        line.getOutlinePaint().setColor(0xFF0000FF); // Blue color
        line.getOutlinePaint().setStrokeWidth(10f);

        map.getOverlays().add(line);
        map.invalidate();
    }

    private void initLocationOverlay() {
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        locationOverlay.runOnFirstFix(() -> {
            final GeoPoint myLocation = locationOverlay.getMyLocation();
            if (myLocation != null) {
                runOnUiThread(() -> {
                    if (map != null) {
                        map.getController().animateTo(myLocation);
                        map.getController().setZoom(15.0);
                    }
                });
            }
        });
        map.getOverlays().add(locationOverlay);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
        if (locationOverlay != null) {
            locationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocationOverlay();
            }
        }
    }
}
