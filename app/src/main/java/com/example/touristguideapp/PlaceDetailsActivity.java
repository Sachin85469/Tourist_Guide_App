package com.example.touristguideapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PlaceDetailsActivity extends AppCompatActivity {

    private String placeId;
    private ImageView btnFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_details);

        ImageView ivImage = findViewById(R.id.detailImage);
        TextView tvName = findViewById(R.id.detailName);
        TextView tvCategory = findViewById(R.id.detailCategory);
        TextView tvDescription = findViewById(R.id.detailDescription);
        TextView tvBudget = findViewById(R.id.detailBudget);
        TextView tvCrowd = findViewById(R.id.detailCrowd);
        TextView tvBestTime = findViewById(R.id.detailBestTime);
        TextView tvTips = findViewById(R.id.detailTips);
        TextView tvFunFact = findViewById(R.id.detailFunFact);
        TextView tvStation = findViewById(R.id.detailStation);
        
        // Back button (if you want to add one to the layout or use a separate view)
        // Here I'll look for a common ID or just rely on system back
        
        // Favorite button in details
        btnFavorite = findViewById(R.id.btnFavoriteDetails);

        // Receive data from Intent
        Intent intent = getIntent();
        if (intent != null) {
            placeId = intent.getStringExtra("id");
            String name = intent.getStringExtra("name");
            String description = intent.getStringExtra("description");
            String category = intent.getStringExtra("category");
            String budget = intent.getStringExtra("budget");
            String crowdLevel = intent.getStringExtra("crowdLevel");
            String bestTime = intent.getStringExtra("bestTime");
            String tips = intent.getStringExtra("tips");
            String funFact = intent.getStringExtra("funFact");
            String station = intent.getStringExtra("nearestStation");
            int imageResId = intent.getIntExtra("imageResId", 0);

            // Display data
            if (tvName != null) tvName.setText(name);
            if (tvDescription != null) tvDescription.setText(description);
            if (tvCategory != null) tvCategory.setText(category);
            if (tvBudget != null) tvBudget.setText(budget);
            if (tvCrowd != null) tvCrowd.setText(crowdLevel);
            if (tvBestTime != null) tvBestTime.setText(bestTime);
            if (tvTips != null) tvTips.setText(tips != null ? tips : "No tips available.");
            if (tvFunFact != null) tvFunFact.setText(funFact != null ? funFact : "Did you know? Pune is amazing!");
            if (tvStation != null) tvStation.setText(station != null ? station : "Not specified.");
            
            if (ivImage != null && imageResId != 0) {
                ivImage.setImageResource(imageResId);
            }

            updateFavoriteIcon();
            
            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> {
                    FavoritesManager.toggleFavorite(this, placeId);
                    updateFavoriteIcon();
                });
            }
        }
    }

    private void updateFavoriteIcon() {
        if (btnFavorite != null && placeId != null) {
            if (FavoritesManager.isFavorite(this, placeId)) {
                btnFavorite.setImageResource(R.drawable.ic_favorite);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
        }
    }
}
