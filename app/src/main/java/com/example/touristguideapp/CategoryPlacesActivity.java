package com.example.touristguideapp;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CategoryPlacesActivity extends AppCompatActivity implements PlaceAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private PlaceAdapter adapter;
    private List<Place> filteredList = new ArrayList<>();
    private LinearLayout emptyLayout;
    private ProgressBar progressBar;

    // Mock User Location (Pune Center)
    private final double USER_LAT = 18.5204;
    private final double USER_LNG = 73.8567;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_places);

        recyclerView = findViewById(R.id.recyclerViewPlaces);
        TextView txtTitle = findViewById(R.id.txtTitle);
        emptyLayout = findViewById(R.id.emptyLayout);
        progressBar = findViewById(R.id.progressBar);
        
        // Receive category from intent
        String selectedCategory = getIntent().getStringExtra("category");

        // Set Title to Uppercase
        if (txtTitle != null && selectedCategory != null) {
            txtTitle.setText(selectedCategory.toUpperCase());
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Simulate loading with distance calculation
        new Handler().postDelayed(() -> {
            loadData(selectedCategory);
        }, 500);
    }

    private void loadData(String selectedCategory) {
        // Filter list from DataProvider
        List<Place> allPlaces = DataProvider.getPlaces();
        filteredList.clear();
        
        if (selectedCategory != null) {
            if (selectedCategory.equalsIgnoreCase("All")) {
                filteredList.addAll(allPlaces);
            } else {
                for (Place place : allPlaces) {
                    if (place.getCategory() != null && place.getCategory().equalsIgnoreCase(selectedCategory)) {
                        filteredList.add(place);
                    }
                }
            }
        }

        // Calculate distances
        for (Place place : filteredList) {
            float[] results = new float[1];
            Location.distanceBetween(
                USER_LAT, USER_LNG,
                place.getLatitude(), place.getLongitude(),
                results
            );
            double distanceKm = results[0] / 1000.0;
            place.setDistance(distanceKm);
        }

        // Hide loading
        progressBar.setVisibility(View.GONE);

        if (filteredList.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            
            emptyLayout.setAlpha(0f);
            emptyLayout.animate().alpha(1f).setDuration(300);
        } else {
            emptyLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            
            recyclerView.setAlpha(0f);
            recyclerView.animate().alpha(1f).setDuration(300);
            
            // Set adapter
            adapter = new PlaceAdapter(filteredList, this);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    @Override
    public void onItemClick(Place place) {
        Intent intent = new Intent(this, PlaceDetailsActivity.class);
        intent.putExtra("id", place.getId());
        intent.putExtra("name", place.getName());
        intent.putExtra("description", place.getDescription());
        intent.putExtra("category", place.getCategory());
        intent.putExtra("budget", place.getBudget());
        intent.putExtra("crowdLevel", place.getCrowdLevel());
        intent.putExtra("bestTime", place.getBestTime());
        intent.putExtra("imageResId", place.getImageResId());
        intent.putExtra("tips", place.getTips());
        intent.putExtra("funFact", place.getFunFact());
        intent.putExtra("nearestStation", place.getNearestStation());
        intent.putExtra("lat", place.getLatitude());
        intent.putExtra("lng", place.getLongitude());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
