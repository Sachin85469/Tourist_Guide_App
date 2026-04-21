package com.example.touristguideapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private EditText searchInput;
    private ImageButton searchBtn;
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

        // Initialize Map
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        IMapController controller = map.getController();
        controller.setZoom(15.0);

        GeoPoint startPoint = new GeoPoint(18.5204, 73.8567);
        controller.setCenter(startPoint);

        searchBtn.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();

            if (!query.isEmpty()) {
                searchLocation(query);
            } else {
                Toast.makeText(this, "Enter a place name", Toast.LENGTH_SHORT).show();
            }
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

                // Move map
                map.getController().setZoom(15.0);
                map.getController().setCenter(searchedPoint);

                // Add marker
                org.osmdroid.views.overlay.Marker marker =
                        new org.osmdroid.views.overlay.Marker(map);

                marker.setPosition(searchedPoint);
                marker.setTitle(locationName);

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
