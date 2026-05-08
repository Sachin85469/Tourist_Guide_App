package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

public class PlanTripActivity extends AppCompatActivity {

    private Spinner spinnerDays;
    private Spinner spinnerType;
    private Button btnGeneratePlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_trip);

        ImageView btnBack = findViewById(R.id.btnPlanBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        spinnerDays = findViewById(R.id.spinnerDays);
        spinnerType = findViewById(R.id.spinnerType);
        btnGeneratePlan = findViewById(R.id.btnGeneratePlan);

        if (spinnerDays != null) {
            String[] daysOptions = {"1 Day", "2 Days", "3 Days"};
            ArrayAdapter<String> daysAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, daysOptions);
            daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDays.setAdapter(daysAdapter);
        }

        if (spinnerType != null) {
            String[] typeOptions = {"Nature", "Food", "Mixed"};
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeOptions);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);
        }

        if (btnGeneratePlan != null) {
            btnGeneratePlan.setOnClickListener(v -> {
                int days = 1;
                if (spinnerDays != null && spinnerDays.getSelectedItem() != null) {
                    days = spinnerDays.getSelectedItemPosition() + 1;
                }

                String type = "Mixed";
                if (spinnerType != null && spinnerType.getSelectedItem() != null) {
                    type = spinnerType.getSelectedItem().toString();
                }

                Intent intent = new Intent(PlanTripActivity.this, ItineraryActivity.class);
                intent.putExtra("days", days);
                intent.putExtra("type", type);
                startActivity(intent);
            });
        }
    }
}
