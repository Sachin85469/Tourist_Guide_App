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
import java.util.ArrayList;
import java.util.List;

public class ItineraryActivity extends AppCompatActivity {

    private RecyclerView rvItinerary;
    private ItineraryAdapter adapter;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary);

        ImageView btnBack = findViewById(R.id.btnItineraryBack);
        btnBack.setOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvItineraryTitle);
        rvItinerary = findViewById(R.id.rvItinerary);
        rvItinerary.setLayoutManager(new LinearLayoutManager(this));

        boolean isQuickPlan = getIntent().getBooleanExtra("isQuickPlan", false);
        List<DayPlan> plan;

        if (isQuickPlan) {
            String customTitle = getIntent().getStringExtra("title");
            String quickPlanText = getIntent().getStringExtra("quickPlanText");
            if (tvTitle != null && customTitle != null) tvTitle.setText(customTitle);
            
            plan = new ArrayList<>();
            plan.add(new DayPlan("Your Selection", quickPlanText));
        } else {
            int days = getIntent().getIntExtra("days", 1);
            String type = getIntent().getStringExtra("type");
            plan = generatePlan(days, type);
        }

        adapter = new ItineraryAdapter(plan);
        rvItinerary.setAdapter(adapter);
    }

    private List<DayPlan> generatePlan(int days, String type) {
        List<DayPlan> plan = new ArrayList<>();

        for (int i = 1; i <= days; i++) {
            String places = "";
            if (type.equalsIgnoreCase("Nature")) {
                if (i == 1) places = "- Sinhagad Fort\n- Khadakwasla Dam\n- Pu La Deshpande Garden";
                else if (i == 2) places = "- Mulshi Lake\n- Tamhini Ghat\n- Vetal Tekdi";
                else places = "- Lohagad Fort\n- Pawna Lake\n- Lonavala Points";
            } else if (type.equalsIgnoreCase("Food")) {
                if (i == 1) places = "- FC Road (Street Food)\n- JM Road (Breakfast)\n- Shaniwar Peth (Traditional)";
                else if (i == 2) places = "- Camp Area (Bakeries)\n- Koregaon Park (Cafes)\n- MG Road (Irani Chai)";
                else places = "- Kothrud (Local Snacks)\n- Deccan (Misal Pav)\n- Viman Nagar (Dining)";
            } else { // Mixed
                if (i == 1) places = "- Shaniwar Wada\n- Dagdusheth Halwai Ganpati\n- Laxmi Road Shopping";
                else if (i == 2) places = "- Aga Khan Palace\n- Osho Garden\n- Koregaon Park Dinner";
                else places = "- Pataleshwar Caves\n- Parvati Hill\n- Saras Baug";
            }
            plan.add(new DayPlan("Day " + i, places));
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
