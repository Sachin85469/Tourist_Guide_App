package com.arriva.touristguideapp;

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

public class HomeActivity extends BaseActivity {

    private static final int VOICE_SEARCH_REQUEST_CODE = 101;
    private RecyclerView recyclerViewPlaces;
    private PlaceAdapter placeAdapter;
    private EditText editTextSearch;
    private ImageView btnVoiceSearch, ivProfileIcon;
    private TextView tvHomeUserName, tvHomeUserEmail;
    private View fabAiChat;
    private long lastSearchLogTime = 0;
    private static final long SEARCH_LOG_DEBOUNCE = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Initialize views
        recyclerViewPlaces = findViewById(R.id.recyclerViewPlaces);
        editTextSearch = findViewById(R.id.editTextSearch);
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);
        tvHomeUserName = findViewById(R.id.tvHomeUserName);
        tvHomeUserEmail = findViewById(R.id.tvHomeUserEmail);
        fabAiChat = findViewById(R.id.fabAiChat);

        // AI Chatbot setup
        if (fabAiChat != null) {
            fabAiChat.setOnClickListener(v -> {
                try {
                    android.util.Log.d("HomeActivity", "Navigating to AiChatActivity");
                    startActivity(new Intent(this, AiChatActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    android.util.Log.e("HomeActivity", "Error opening AiChatActivity", e);
                    Toast.makeText(this, "Unable to open assistant", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Profile Avatar and User Info Setup
        loadUserInfo();
        ivProfileIcon.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 2. Set LayoutManager (Vertical)
        recyclerViewPlaces.setLayoutManager(new LinearLayoutManager(this));

        // 3. Get data from DataProvider
        List<Place> allPlaces = DataProvider.getAllPlaces();

        // 4. Create Adapter and set it to RecyclerView
        placeAdapter = new PlaceAdapter(new ArrayList<>(allPlaces), place -> {
            // Track search click (Requirement 2)
            String query = editTextSearch != null ? editTextSearch.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                new com.arriva.touristguideapp.data.analytics.AnalyticsRepository().logSearchClick(query, place.getId());
            }

            Intent intent = new Intent(this, PlaceDetailsActivity.class);
            PlaceIntentExtras.putPlaceDetails(intent, place);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        recyclerViewPlaces.setAdapter(placeAdapter);

        // 5. Setup Category Buttons
        setupCategoryButtons();

        // 6. Update Category Counts
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
        Map<String, Integer> categoryCount = DataProvider.getCategoryCount(DataProvider.getAllPlaces());

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
        // Track Category Explored (Requirement 3)
        new com.arriva.touristguideapp.data.analytics.AnalyticsRepository().trackCategoryExplored(category);

        Intent intent = new Intent(this, CategoryPlacesActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // Method to filter the list based on search text using DataProvider utility
    private void filter(String query) {
        List<Place> filteredList = DataProvider.searchPlaces(query);

        // Update the adapter with the filtered list
        if (placeAdapter != null) {
            placeAdapter.updateList(filteredList);
        }

        // Requirement 2 & 7: Log search analytics with debounce
        if (query != null && query.trim().length() >= 3) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSearchLogTime > SEARCH_LOG_DEBOUNCE) {
                new com.arriva.touristguideapp.data.analytics.AnalyticsRepository().logSearch(query, filteredList.size());
                lastSearchLogTime = currentTime;
            }
        }
    }

    private void loadUserInfo() {
        // Load local avatar immediately from SharedPreferences for better UX (Requirement 4)
        ProfileUtils.loadAvatar(this, ivProfileIcon);

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            ProfileUtils.fetchUserData(user.getUid(), new ProfileUtils.UserCallback() {
                @Override
                public void onUserLoaded(User userModel) {
                    if (tvHomeUserName != null) tvHomeUserName.setText("Hello, " + userModel.getName());
                    if (tvHomeUserEmail != null) tvHomeUserEmail.setText(userModel.getEmail());
                    ProfileUtils.loadAvatar(HomeActivity.this, ivProfileIcon, userModel);
                }

                @Override
                public void onError(Exception e) {
                    if (tvHomeUserName != null) tvHomeUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "ExploreEase");
                    if (tvHomeUserEmail != null) tvHomeUserEmail.setText(user.getEmail());
                    ProfileUtils.loadAvatar(HomeActivity.this, ivProfileIcon);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }
}
