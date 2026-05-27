package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.arriva.touristguideapp.data.places.PlaceRepository;
import java.util.ArrayList;
import java.util.List;

public class ItineraryActivity extends AppCompatActivity {

    private RecyclerView rvItinerary;
    private ItineraryAdapter adapter;
    private TextView tvTitle;
    private PlaceRepository placeRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary);

        placeRepository = new PlaceRepository();
        
        ImageView btnBack = findViewById(R.id.btnItineraryBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvItineraryTitle);
        rvItinerary = findViewById(R.id.rvItinerary);
        rvItinerary.setLayoutManager(new LinearLayoutManager(this));

        boolean isQuickPlan = getIntent().getBooleanExtra("isQuickPlan", false);

        if (isQuickPlan) {
            String customTitle = getIntent().getStringExtra("title");
            String quickPlanText = getIntent().getStringExtra("quickPlanText");
            if (tvTitle != null && customTitle != null) tvTitle.setText(customTitle);
            
            List<DayPlan> plan = new ArrayList<>();
            plan.add(new DayPlan("Your Selection", quickPlanText));
            adapter = new ItineraryAdapter(plan);
            rvItinerary.setAdapter(adapter);
        } else {
            int days = getIntent().getIntExtra("days", 1);
            String type = getIntent().getStringExtra("type");
            loadAndGeneratePlan(days, type);
        }
    }

    private void loadAndGeneratePlan(int days, String type) {
        placeRepository.fetchPublishedPlaces((places, origin, message) -> {
            List<DayPlan> plan = generateSmartPlan(places, days, type);
            adapter = new ItineraryAdapter(plan);
            rvItinerary.setAdapter(adapter);
        });
    }

    private List<DayPlan> generateSmartPlan(List<Place> allPlaces, int days, String type) {
        List<DayPlan> plan = new ArrayList<>();
        List<Place> filtered = new ArrayList<>();
        
        if (type.equalsIgnoreCase("Nature")) {
            for (Place p : allPlaces) if (p.getCategory().equalsIgnoreCase("Nature")) filtered.add(p);
        } else if (type.equalsIgnoreCase("Food")) {
            for (Place p : allPlaces) if (p.getCategory().equalsIgnoreCase("Food")) filtered.add(p);
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
}
