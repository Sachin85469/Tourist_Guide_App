package com.arriva.touristguideapp;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.data.places.PlaceRepository;

import java.util.ArrayList;
import java.util.List;

/** Displays every available place ordered by distance from the mock Pune-center location. */
public class NearbyPlacesActivity extends BaseActivity implements PlaceAdapter.OnItemClickListener {

    private static final double USER_LAT = 18.5204;
    private static final double USER_LNG = 73.8567;

    private RecyclerView recyclerView;
    private PlaceAdapter adapter;
    private List<Place> nearbyPlaces = new ArrayList<>();
    private LinearLayout emptyLayout;
    private ProgressBar progressBar;
    private PlaceRepository placeRepository;
    private View offlineCacheBanner;
    private boolean offlineCacheBannerDismissed = false;

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

        if (txtTitle != null) {
            txtTitle.setText("NEARBY PLACES");
        }

        placeRepository = new PlaceRepository(this);
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        new Handler().postDelayed(this::loadPlaces, 500);
    }

    private void loadPlaces() {
        // Null city and category filters return all published places.
        placeRepository.getPlacesOfflineFirst(null, null, (places, origin, cacheEmpty, message) -> {
            showOfflineCacheBanner(origin == PlaceRepository.DataOrigin.ROOM_CACHE && !cacheEmpty);
            nearbyPlaces = new ArrayList<>(places);

            for (Place place : nearbyPlaces) {
                float[] results = new float[1];
                Location.distanceBetween(
                        USER_LAT,
                        USER_LNG,
                        place.getLatitude(),
                        place.getLongitude(),
                        results
                );
                place.setDistance(results[0] / 1000.0);
            }

            DataProvider.sortByDistance(nearbyPlaces);
            showPlaces();
        });
    }

    private void showPlaces() {
        progressBar.setVisibility(View.GONE);

        if (nearbyPlaces.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setAlpha(0f);
            emptyLayout.animate().alpha(1f).setDuration(300);
            return;
        }

        emptyLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAlpha(0f);
        recyclerView.animate().alpha(1f).setDuration(300);
        adapter = new PlaceAdapter(nearbyPlaces, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showOfflineCacheBanner(boolean show) {
        if (offlineCacheBanner != null) {
            offlineCacheBanner.setVisibility(
                    show && !offlineCacheBannerDismissed ? View.VISIBLE : View.GONE
            );
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
