package com.arriva.touristguideapp;

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
import com.arriva.touristguideapp.data.places.PlaceRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryPlacesActivity extends BaseActivity implements PlaceAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private PlaceAdapter adapter;
    private List<Place> filteredList = new ArrayList<>();
    private LinearLayout emptyLayout;
    private ProgressBar progressBar;
    private PlaceRepository placeRepository;
    private View offlineCacheBanner;
    private boolean offlineCacheBannerDismissed = false;

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
        offlineCacheBanner = findViewById(R.id.offlineCacheBanner);
        View btnDismissOfflineBanner = findViewById(R.id.btnDismissOfflineBanner);
        if (btnDismissOfflineBanner != null) {
            btnDismissOfflineBanner.setOnClickListener(v -> {
                offlineCacheBannerDismissed = true;
                showOfflineCacheBanner(false);
            });
        }
        placeRepository = new PlaceRepository(this);
        
        // ── Receive categories from intent ─────────────────────────────────
        // MoodActivity sends a multi-category ArrayList; other screens send a
        // single "category" string. Support both for full backward-compat.
        ArrayList<String> multiCategories =
                getIntent().getStringArrayListExtra("categories");
        String singleCategory = getIntent().getStringExtra("category");

        // Build the display title
        String displayTitle;
        if (multiCategories != null && !multiCategories.isEmpty()) {
            displayTitle = "YOUR VIBE"; // multi-category mood result
        } else {
            displayTitle = singleCategory != null ? singleCategory.toUpperCase() : "PLACES";
        }

        // Set Title
        if (txtTitle != null) {
            txtTitle.setText(displayTitle);
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Determine the final category list for the repository call
        final List<String> categoryList;
        if (multiCategories != null && !multiCategories.isEmpty()) {
            categoryList = multiCategories;
        } else if (singleCategory != null && !singleCategory.equalsIgnoreCase("All")) {
            categoryList = Collections.singletonList(singleCategory);
        } else {
            categoryList = null;
        }

        // Simulate loading with distance calculation
        new android.os.Handler().postDelayed(() -> loadData(categoryList), 500);
    }

    private void loadData(List<String> categories) {
        placeRepository.getPlacesOfflineFirst(null, categories, (places, origin, cacheEmpty, message) -> {
            showOfflineCacheBanner(origin == PlaceRepository.DataOrigin.ROOM_CACHE && !cacheEmpty);
            filteredList = new ArrayList<>(places);

            // Calculate distances from mock user location
            for (Place place : filteredList) {
                float[] results = new float[1];
                android.location.Location.distanceBetween(
                    USER_LAT, USER_LNG,
                    place.getLatitude(), place.getLongitude(),
                    results
                );
                double distanceKm = results[0] / 1000.0;
                place.setDistance(distanceKm);
            }

            // Sort by distance
            DataProvider.sortByDistance(filteredList);

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
                adapter = new PlaceAdapter(filteredList, this);
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
        });
    }

    private void showOfflineCacheBanner(boolean show) {
        if (offlineCacheBanner != null) {
            offlineCacheBanner.setVisibility(show && !offlineCacheBannerDismissed ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onItemClick(Place place) {
        Intent intent = new Intent(this, PlaceDetailsActivity.class);
        PlaceIntentExtras.putPlaceDetails(intent, place);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
