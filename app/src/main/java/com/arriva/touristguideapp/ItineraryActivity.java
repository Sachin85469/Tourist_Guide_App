package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.arriva.touristguideapp.data.places.PlaceRepository;
import com.arriva.touristguideapp.data.trips.Trip;
import com.arriva.touristguideapp.data.trips.TripRepository;
import com.arriva.touristguideapp.data.notifications.NotificationRepository;
import com.arriva.touristguideapp.data.notifications.NotificationModel;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ItineraryActivity extends BaseActivity {

    private RecyclerView rvItinerary;
    private ItineraryAdapter adapter;
    private TextView tvTitle;
    private PlaceRepository placeRepository;
    private TripRepository tripRepository;
    private NotificationRepository notificationRepository;
    private ExtendedFloatingActionButton btnSaveTrip;
    private List<Place> selectedPlacesList = new ArrayList<>();

    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary);

        placeRepository = new PlaceRepository();
        tripRepository = new TripRepository();
        notificationRepository = new NotificationRepository(this);
        
        emptyState = findViewById(R.id.emptyStatePlanner);
        
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvTitle = findViewById(R.id.tvItineraryTitle);
        rvItinerary = findViewById(R.id.rvItinerary);
        rvItinerary.setLayoutManager(new LinearLayoutManager(this));
        
        btnSaveTrip = findViewById(R.id.btnSaveTrip);
        if (btnSaveTrip != null) {
            btnSaveTrip.setOnClickListener(v -> saveThisItinerary());
        }

        boolean isQuickPlan = getIntent().getBooleanExtra("isQuickPlan", false);

        if (isQuickPlan) {
            String customTitle = getIntent().getStringExtra("title");
            String quickPlanText = getIntent().getStringExtra("quickPlanText");
            if (tvTitle != null && customTitle != null) tvTitle.setText(customTitle);
            
            List<DayPlan> plan = new ArrayList<>();
            if (quickPlanText != null && !quickPlanText.isEmpty()) {
                plan.add(new DayPlan("Your Selection", quickPlanText));
                emptyState.setVisibility(View.GONE);
                rvItinerary.setVisibility(View.VISIBLE);
            } else {
                emptyState.setVisibility(View.VISIBLE);
                rvItinerary.setVisibility(View.GONE);
            }
            
            adapter = new ItineraryAdapter(plan);
            rvItinerary.setAdapter(adapter);

            // Populate selectedPlacesList for Quick Plan
            int count = 2;
            if (customTitle != null) {
                if (customTitle.contains("Half Day")) count = 4;
                else if (customTitle.contains("Full Day")) count = 5;
            }
            List<Place> topPicks = DataProvider.getTopPicks();
            if (topPicks.isEmpty()) {
                topPicks = DataProvider.getAllPlaces();
            }
            selectedPlacesList.clear();
            for (int i = 0; i < Math.min(count, topPicks.size()); i++) {
                selectedPlacesList.add(topPicks.get(i));
            }
        } else {
            int days = getIntent().getIntExtra("days", 1);
            String type = getIntent().getStringExtra("type");
            loadAndGeneratePlan(days, type);
        }
    }

    private void loadAndGeneratePlan(int days, String type) {
        placeRepository.fetchPublishedPlaces((places, origin, message) -> {
            if (places.isEmpty()) {
                runOnUiThread(() -> {
                    emptyState.setVisibility(View.VISIBLE);
                    rvItinerary.setVisibility(View.GONE);
                });
                return;
            }

            // Populate selectedPlacesList for Custom Plan
            List<Place> filtered = new ArrayList<>();
            if (type != null) {
                if (type.equalsIgnoreCase("Nature")) {
                    for (Place p : places) if (p.getCategory() != null && p.getCategory().equalsIgnoreCase("Nature")) filtered.add(p);
                } else if (type.equalsIgnoreCase("Food")) {
                    for (Place p : places) if (p.getCategory() != null && p.getCategory().equalsIgnoreCase("Food")) filtered.add(p);
                } else {
                    filtered.addAll(places);
                }
            } else {
                filtered.addAll(places);
            }
            filtered.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));

            int placesPerDay = 3;
            int currentIdx = 0;
            selectedPlacesList.clear();
            for (int i = 1; i <= days; i++) {
                for (int j = 0; j < placesPerDay && currentIdx < filtered.size(); j++) {
                    selectedPlacesList.add(filtered.get(currentIdx++));
                }
            }

            List<DayPlan> plan = generateSmartPlan(places, days, type != null ? type : "All");
            runOnUiThread(() -> {
                if (plan.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvItinerary.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvItinerary.setVisibility(View.VISIBLE);
                    adapter = new ItineraryAdapter(plan);
                    rvItinerary.setAdapter(adapter);
                }
            });
        });
    }

    private List<DayPlan> generateSmartPlan(List<Place> allPlaces, int days, String type) {
        List<DayPlan> plan = new ArrayList<>();
        List<Place> filtered = new ArrayList<>();
        
        if (type.equalsIgnoreCase("Nature")) {
            for (Place p : allPlaces) {
                if (p.getCategory() != null && p.getCategory().equalsIgnoreCase("Nature")) filtered.add(p);
            }
        } else if (type.equalsIgnoreCase("Food")) {
            for (Place p : allPlaces) {
                if (p.getCategory() != null && p.getCategory().equalsIgnoreCase("Food")) filtered.add(p);
            }
        } else {
            filtered.addAll(allPlaces);
        }
        
        // Sort by rating to pick the best ones
        filtered.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));

        int placesPerDay = 3;
        int currentIdx = 0;

        for (int i = 1; i <= days; i++) {
            StringBuilder dayPlaces = new StringBuilder();
            for (int j = 0; j < placesPerDay && currentIdx < filtered.size(); j++) {
                Place p = filtered.get(currentIdx++);
                dayPlaces.append("• ").append(p.getName()).append(" (").append(p.getCategory()).append(")\n");
                dayPlaces.append("  ").append(p.getRating()).append(" ⭐ | ").append(p.getCity()).append("\n\n");
            }
            
            if (dayPlaces.length() == 0) {
                dayPlaces.append("No more spots found for this category. Explore the main map for more!");
            }
            
            plan.add(new DayPlan("Day " + i, dayPlaces.toString().trim()));
        }
        return plan;
    }

    private static class DayPlan {
        String day;
        String places;

        DayPlan(String day, String places) {
            this.day = day;
            this.places = places;
        }
    }

    private static class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {
        private final List<DayPlan> plans;

        ItineraryAdapter(List<DayPlan> plans) {
            this.plans = plans;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DayPlan plan = plans.get(position);
            holder.tvDayTitle.setText(plan.day);
            holder.tvDayPlaces.setText(plan.places);
        }

        @Override
        public int getItemCount() {
            return plans.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDayTitle, tvDayPlaces;

            ViewHolder(View itemView) {
                super(itemView);
                tvDayTitle = itemView.findViewById(R.id.tvDayTitle);
                tvDayPlaces = itemView.findViewById(R.id.tvDayPlaces);
            }
        }
    }

    private void saveThisItinerary() {
        String title = tvTitle != null ? tvTitle.getText().toString() : "My Trip";
        if (title.isEmpty()) title = "My Trip";

        Trip trip = new Trip();
        trip.setTitle(title);
        String location = selectedPlacesList.isEmpty() ? "" : selectedPlacesList.get(0).getCity();
        trip.setDestinationName(location == null || location.trim().isEmpty() ? title : location);
        trip.setLocation(location == null ? "" : location);
        trip.setStatus("planned");
        trip.setPlaces(selectedPlacesList);

        List<String> activities = new ArrayList<>();
        for (Place place : selectedPlacesList) {
            if (place.getName() != null && !place.getName().trim().isEmpty()) {
                activities.add("Visit " + place.getName());
            }
            if ((trip.getImageUrl() == null || trip.getImageUrl().trim().isEmpty())
                    && place.getImageUrl() != null
                    && !place.getImageUrl().trim().isEmpty()) {
                trip.setImageUrl(place.getImageUrl());
            }
        }
        trip.setActivities(activities);
        trip.setNotes("Generated from the trip planner.");
        trip.setBudget("");
        
        Calendar cal = Calendar.getInstance();
        trip.setStartDate(cal.getTime());
        int days = getIntent().getIntExtra("days", 1);
        cal.add(Calendar.DATE, days - 1);
        trip.setEndDate(cal.getTime());

        if (btnSaveTrip != null) {
            btnSaveTrip.setEnabled(false);
        }
        
        tripRepository.saveTripAsync(trip).addOnCompleteListener(task -> {
            if (btnSaveTrip != null) {
                btnSaveTrip.setEnabled(true);
            }
            if (task.isSuccessful()) {
                Toast.makeText(ItineraryActivity.this, "Trip saved to history!", Toast.LENGTH_SHORT).show();

                String tripLabel = trip.getTitle();
                if (tripLabel == null || tripLabel.trim().isEmpty()) {
                    tripLabel = trip.getDestinationName();
                }
                if (tripLabel == null || tripLabel.trim().isEmpty()) {
                    tripLabel = "Trip";
                }
                com.arriva.touristguideapp.profile.ProfileActivityTracker.log(
                        ItineraryActivity.this,
                        com.arriva.touristguideapp.profile.ProfileActivityTracker.Action.TRIP_CREATED,
                        tripLabel
                );
                
                // Generate trip notification
                notificationRepository.addNotification(
                    "Trip Created",
                    "Your trip '" + trip.getTitle() + "' was planned successfully.",
                    NotificationModel.TYPE_TRIP
                );
                
                finish();
            } else {
                Toast.makeText(ItineraryActivity.this, "Failed to save trip: " + 
                    (task.getException() != null ? task.getException().getMessage() : "unknown"), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
