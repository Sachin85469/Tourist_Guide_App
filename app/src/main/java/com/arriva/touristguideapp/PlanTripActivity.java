package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;

public class PlanTripActivity extends BaseActivity {

    private Spinner spinnerDays;
    private Spinner spinnerType;
    private Spinner spinnerBudget;
    private Button btnGeneratePlan;
    private ProgressBar progressGeneratePlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_trip);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        spinnerDays = findViewById(R.id.spinnerDays);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerBudget = findViewById(R.id.spinnerBudget);
        btnGeneratePlan = findViewById(R.id.btnGeneratePlan);
        progressGeneratePlan = findViewById(R.id.progressGeneratePlan);

        if (spinnerDays != null) {
            String[] daysOptions = {"1 Day", "2 Days", "3 Days", "4 Days", "5 Days", "6 Days", "7 Days"};
            ArrayAdapter<String> daysAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, daysOptions);
            daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDays.setAdapter(daysAdapter);
        }

        if (spinnerType != null) {
            String[] typeOptions = {"Mixed", "Historical", "Nature", "Religious", "Food", "Culture", "Adventure"};
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeOptions);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);
        }

        if (spinnerBudget != null) {
            String[] budgetOptions = {"Budget", "Mid-range", "Luxury"};
            ArrayAdapter<String> budgetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, budgetOptions);
            budgetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerBudget.setAdapter(budgetAdapter);
            spinnerBudget.setSelection(1);
        }

        if (btnGeneratePlan != null) {
            btnGeneratePlan.setOnClickListener(v -> {
                setGeneratingState(true);

                int days = 1;
                if (spinnerDays != null && spinnerDays.getSelectedItem() != null) {
                    days = spinnerDays.getSelectedItemPosition() + 1;
                }

                String type = "Mixed";
                if (spinnerType != null && spinnerType.getSelectedItem() != null) {
                    type = spinnerType.getSelectedItem().toString();
                }

                String budget = "Mid-range";
                if (spinnerBudget != null && spinnerBudget.getSelectedItem() != null) {
                    budget = spinnerBudget.getSelectedItem().toString();
                }

                Intent intent = new Intent(PlanTripActivity.this, ItineraryActivity.class);
                intent.putExtra("days", days);
                intent.putExtra("type", type);
                intent.putExtra("budget", budget);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setGeneratingState(false);
    }

    private void setGeneratingState(boolean isGenerating) {
        if (btnGeneratePlan != null) {
            btnGeneratePlan.setEnabled(!isGenerating);
            btnGeneratePlan.setText(isGenerating ? "Creating your AI trip..." : "Create My Trip");
        }
        if (progressGeneratePlan != null) {
            progressGeneratePlan.setVisibility(isGenerating ? View.VISIBLE : View.GONE);
        }
        if (spinnerDays != null) spinnerDays.setEnabled(!isGenerating);
        if (spinnerType != null) spinnerType.setEnabled(!isGenerating);
        if (spinnerBudget != null) spinnerBudget.setEnabled(!isGenerating);
    }
}
