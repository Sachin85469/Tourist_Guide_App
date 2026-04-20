package com.example.touristguideapp;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Back button logic
        ImageView btnBack = findViewById(R.id.btnMapBack);
        btnBack.setOnClickListener(v -> finish());

        // FAB logic
        FloatingActionButton fab = findViewById(R.id.fabMyLocation);
        fab.setOnClickListener(v -> {
            if (mMap != null) {
                centerCameraOnFirstPlace();
                Toast.makeText(MapActivity.this, "Centering map...", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // 1. Get list of real places
        List<Place> places = DataProvider.getPlaces();

        // 2. Add markers for each place
        for (Place place : places) {
            LatLng location = new LatLng(place.getLatitude(), place.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(place.getName())
                    .snippet(place.getCategory())); // Show category in snippet
        }

        // 3. Move camera to center on the first place
        centerCameraOnFirstPlace();
    }

    private void centerCameraOnFirstPlace() {
        List<Place> places = DataProvider.getPlaces();
        if (!places.isEmpty() && mMap != null) {
            Place firstPlace = places.get(0);
            LatLng firstLocation = new LatLng(firstPlace.getLatitude(), firstPlace.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12f));
        }
    }
}
