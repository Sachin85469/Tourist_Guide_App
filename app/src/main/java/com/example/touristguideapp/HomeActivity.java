package com.example.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private static final int VOICE_SEARCH_REQUEST_CODE = 101;
    private RecyclerView recyclerViewPlaces;
    private PlaceAdapter placeAdapter;
    private List<Place> allPlaces; // Full list to filter from
    private EditText editTextSearch;
    private ImageView btnVoiceSearch;
    private String selectedCategory = ""; // Default empty, meaning show all in main list if no category selected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Initialize views
        recyclerViewPlaces = findViewById(R.id.recyclerViewPlaces);
        editTextSearch = findViewById(R.id.editTextSearch);
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch);

        // 2. Set LayoutManager (Vertical)
        recyclerViewPlaces.setLayoutManager(new LinearLayoutManager(this));

        // 3. Get data from DataProvider
        allPlaces = DataProvider.getPlaces();

        // 4. Create Adapter and set it to RecyclerView
        placeAdapter = new PlaceAdapter(new ArrayList<>(allPlaces));
        recyclerViewPlaces.setAdapter(placeAdapter);

        // 5. Setup Category Buttons
        setupCategoryButtons();

        // 6. Setup Feature Buttons
        setupFeatureButtons();

        // 7. Update Category Counts
        updateCategoryCounts();

        // 8. Add Search Functionality
        if (editTextSearch != null) {
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

        // 9. Voice Search
        if (btnVoiceSearch != null) {
            btnVoiceSearch.setOnClickListener(v -> startVoiceSearch());
        }
    }

    private void updateCategoryCounts() {
        Map<String, Integer> categoryCount = DataProvider.getCategoryCount(allPlaces);

        TextView txtHistory = findViewById(R.id.txtHistoryCount);
        if (txtHistory != null) txtHistory.setText("🏛️ History (" + categoryCount.getOrDefault("History", 0) + ")");

        TextView txtSpiritual = findViewById(R.id.txtSpiritualCount);
        if (txtSpiritual != null) txtSpiritual.setText("🙏 Spiritual (" + categoryCount.getOrDefault("Spiritual", 0) + ")");

        TextView txtFood = findViewById(R.id.txtFoodCount);
        if (txtFood != null) txtFood.setText("🍕 Food (" + categoryCount.getOrDefault("Food", 0) + ")");

        TextView txtShopping = findViewById(R.id.txtShoppingCount);
        if (txtShopping != null) txtShopping.setText("🛍️ Shopping (" + categoryCount.getOrDefault("Shopping", 0) + ")");

        TextView txtNature = findViewById(R.id.txtNatureCount);
        if (txtNature != null) txtNature.setText("🌲 Nature (" + categoryCount.getOrDefault("Nature", 0) + ")");

        TextView txtAdventure = findViewById(R.id.txtAdventureCount);
        if (txtAdventure != null) txtAdventure.setText("🎢 Adventure (" + categoryCount.getOrDefault("Adventure", 0) + ")");

        TextView txtEntertainment = findViewById(R.id.txtEntertainmentCount);
        if (txtEntertainment != null) txtEntertainment.setText("🎮 Entertainment (" + categoryCount.getOrDefault("Entertainment", 0) + ")");
    }

    private void setupFeatureButtons() {
        View btnMood = findViewById(R.id.btnMood);
        if (btnMood != null) {
            btnMood.setOnClickListener(v -> {
                startActivity(new Intent(this, MoodActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        View btnQuickPlan = findViewById(R.id.btnQuickPlan);
        if (btnQuickPlan != null) {
            btnQuickPlan.setOnClickListener(v -> {
                startActivity(new Intent(this, QuickPlanActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for a place...");
        try {
            startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Voice search not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String voiceQuery = results.get(0);
                if (editTextSearch != null) {
                    editTextSearch.setText(voiceQuery);
                }
                filter(voiceQuery);
            }
        }
    }

    private void setupCategoryButtons() {
        View btnHistory = findViewById(R.id.btnHistorical);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> openCategory("History"));
        }

        View btnSpiritual = findViewById(R.id.spiritualCard);
        if (btnSpiritual != null) {
            btnSpiritual.setOnClickListener(v -> openCategory("Spiritual"));
        }

        View btnFood = findViewById(R.id.btnFood);
        if (btnFood != null) {
            btnFood.setOnClickListener(v -> openCategory("Food"));
        }

        View btnShopping = findViewById(R.id.shoppingCard);
        if (btnShopping != null) {
            btnShopping.setOnClickListener(v -> openCategory("Shopping"));
        }

        View btnNature = findViewById(R.id.btnNature);
        if (btnNature != null) {
            btnNature.setOnClickListener(v -> openCategory("Nature"));
        }

        View btnAdventure = findViewById(R.id.btnAdventure);
        if (btnAdventure != null) {
            btnAdventure.setOnClickListener(v -> openCategory("Adventure"));
        }

        View btnEntertainment = findViewById(R.id.entertainmentCard);
        if (btnEntertainment != null) {
            btnEntertainment.setOnClickListener(v -> openCategory("Entertainment"));
        }
    }

    private void openCategory(String category) {
        Intent intent = new Intent(this, CategoryPlacesActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

            boolean matchesCategory = selectedCategory.isEmpty() ||
                    place.getCategory().equalsIgnoreCase(selectedCategory);

            if (matchesSearch && matchesCategory) {
                filteredList.add(place);
            }
        }

        // Update the adapter with the filtered list
        if (placeAdapter != null) {
            placeAdapter.updateList(filteredList);
        }
    }
}
