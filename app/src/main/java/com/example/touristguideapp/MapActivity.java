package com.example.touristguideapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

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

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private EditText searchInput;
    private ImageButton searchBtn;
    private MyLocationNewOverlay locationOverlay;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

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
