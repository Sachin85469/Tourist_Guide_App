package com.example.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class QuickPlanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_plan);

        findViewById(R.id.card2Hours).setOnClickListener(v -> generateQuickPlan(2));
        findViewById(R.id.cardHalfDay).setOnClickListener(v -> generateQuickPlan(4));
        findViewById(R.id.cardFullDay).setOnClickListener(v -> generateQuickPlan(5));
    }

    private void generateQuickPlan(int count) {
        // Use Top Picks for better suggestions
        List<Place> topPicks = DataProvider.getTopPicks();
        if (topPicks.isEmpty()) {
            topPicks = DataProvider.getAllPlaces();
        }
        
        ArrayList<String> selectedPlaceNames = new ArrayList<>();
        
        for (int i = 0; i < Math.min(count, topPicks.size()); i++) {
            selectedPlaceNames.add(topPicks.get(i).getName());
        }

        StringBuilder itineraryText = new StringBuilder();
        for (String name : selectedPlaceNames) {
            itineraryText.append("- ").append(name).append("\n");
        }

        Intent intent = new Intent(this, ItineraryActivity.class);
        intent.putExtra("isQuickPlan", true);
        intent.putExtra("quickPlanText", itineraryText.toString());
        intent.putExtra("title", "Quick " + (count == 2 ? "2 Hours" : count == 4 ? "Half Day" : "Full Day") + " Plan");
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
