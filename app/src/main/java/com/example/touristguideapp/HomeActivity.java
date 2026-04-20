package com.example.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPlaces;
    private PlaceAdapter placeAdapter;
    private List<Place> allPlaces; // Full list to filter from
    private EditText editTextSearch;
    private String selectedCategory = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Initialize views
        recyclerViewPlaces = findViewById(R.id.recyclerViewPlaces);
        editTextSearch = findViewById(R.id.editTextSearch);

        // 2. Set LayoutManager (Vertical)
        recyclerViewPlaces.setLayoutManager(new LinearLayoutManager(this));

        // 3. Get data from DataProvider
        allPlaces = DataProvider.getPlaces();

        // 4. Create Adapter and set it to RecyclerView
        placeAdapter = new PlaceAdapter(new ArrayList<>(allPlaces));
        recyclerViewPlaces.setAdapter(placeAdapter);

        // 5. Setup Category Buttons
        setupCategoryButtons();

        // 6. Add Search Functionality
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryButtons() {
        findViewById(R.id.btnNature).setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoryPlacesActivity.class);
            intent.putExtra("category", "nature");
            startActivity(intent);
        });

        findViewById(R.id.btnFood).setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoryPlacesActivity.class);
            intent.putExtra("category", "food");
            startActivity(intent);
        });

        findViewById(R.id.btnHistorical).setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoryPlacesActivity.class);
            intent.putExtra("category", "history");
            startActivity(intent);
        });

        findViewById(R.id.btnAdventure).setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoryPlacesActivity.class);
            intent.putExtra("category", "adventure");
            startActivity(intent);
        });

        findViewById(R.id.spiritualCard).setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoryPlacesActivity.class);
            intent.putExtra("category", "spiritual");
            startActivity(intent);
        });

        findViewById(R.id.entertainmentCard).setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoryPlacesActivity.class);
            intent.putExtra("category", "entertainment");
            startActivity(intent);
        });

        findViewById(R.id.btnAll).setOnClickListener(v -> {
            selectedCategory = "all";
            filter(editTextSearch.getText().toString());
        });
    }

    // Method to filter the list based on search text AND selected category
    private void filter(String query) {
        List<Place> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();

        for (Place place : allPlaces) {
            boolean matchesSearch = lowerCaseQuery.isEmpty() ||
                    place.getName().toLowerCase().contains(lowerCaseQuery) ||
                    place.getCategory().toLowerCase().contains(lowerCaseQuery) ||
                    place.getCity().toLowerCase().contains(lowerCaseQuery);

            boolean matchesCategory = selectedCategory.equals("all") ||
                    place.getCategory().equalsIgnoreCase(selectedCategory);

            if (matchesSearch && matchesCategory) {
                filteredList.add(place);
            }
        }

        // Update the adapter with the filtered list
        placeAdapter.updateList(filteredList);
    }
}
