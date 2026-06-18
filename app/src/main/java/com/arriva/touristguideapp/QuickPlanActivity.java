package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.arriva.touristguideapp.data.places.PlaceRepository;

import java.util.ArrayList;
import java.util.List;

public class QuickPlanActivity extends BaseActivity {

    private PlaceRepository placeRepository;
    private View progressQuickPlan;
    private View card2Hours;
    private View cardHalfDay;
    private View cardFullDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_plan);

        placeRepository = new PlaceRepository(this);
        progressQuickPlan = findViewById(R.id.progressQuickPlan);
        card2Hours = findViewById(R.id.card2Hours);
        cardHalfDay = findViewById(R.id.cardHalfDay);
        cardFullDay = findViewById(R.id.cardFullDay);

        card2Hours.setOnClickListener(v -> generateQuickPlan(2));
        cardHalfDay.setOnClickListener(v -> generateQuickPlan(4));
        cardFullDay.setOnClickListener(v -> generateQuickPlan(5));
    }

    private void generateQuickPlan(int count) {
        setLoadingState(true);
        placeRepository.getPlacesOfflineFirst(null, null, (places, origin, cacheEmpty, message) -> {
            List<Place> sorted = new ArrayList<>(places);
            sorted.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
            runOnUiThread(() -> {
                setLoadingState(false);
                if (sorted.isEmpty()) {
                    Toast.makeText(this, "No places available for a quick plan yet.", Toast.LENGTH_SHORT).show();
                    return;
                }
                launchItinerary(sorted, count);
            });
        });
    }

    private void launchItinerary(List<Place> sortedPlaces, int count) {
        ArrayList<String> selectedPlaceNames = new ArrayList<>();
        ArrayList<String> selectedPlaceIds = new ArrayList<>();

        for (int i = 0; i < Math.min(count, sortedPlaces.size()); i++) {
            Place place = sortedPlaces.get(i);
            selectedPlaceNames.add(place.getName());
            if (place.getId() != null && !place.getId().trim().isEmpty()) {
                selectedPlaceIds.add(place.getId());
            }
        }

        StringBuilder itineraryText = new StringBuilder();
        for (String name : selectedPlaceNames) {
            itineraryText.append("- ").append(name).append("\n");
        }

        Intent intent = new Intent(this, ItineraryActivity.class);
        intent.putExtra("isQuickPlan", true);
        intent.putExtra("quickPlanText", itineraryText.toString());
        intent.putStringArrayListExtra("quickPlaceIds", selectedPlaceIds);
        intent.putExtra("title", "Quick " + (count == 2 ? "2 Hours" : count == 4 ? "Half Day" : "Full Day") + " Plan");
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setLoadingState(boolean isLoading) {
        if (progressQuickPlan != null) {
            progressQuickPlan.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (card2Hours != null) card2Hours.setEnabled(!isLoading);
        if (cardHalfDay != null) cardHalfDay.setEnabled(!isLoading);
        if (cardFullDay != null) cardFullDay.setEnabled(!isLoading);
    }
}
