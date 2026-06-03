package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class MoodActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood);

        MaterialCardView cardRelax = findViewById(R.id.cardRelax);
        MaterialCardView cardFun = findViewById(R.id.cardFun);
        MaterialCardView cardPeace = findViewById(R.id.cardPeace);
        MaterialCardView cardHungry = findViewById(R.id.cardHungry);

        cardRelax.setOnClickListener(v -> openCategory("nature"));
        cardFun.setOnClickListener(v -> openCategory("entertainment"));
        cardPeace.setOnClickListener(v -> openCategory("spiritual"));
        cardHungry.setOnClickListener(v -> openCategory("food"));
    }

    private void openCategory(String category) {
        Intent intent = new Intent(this, CategoryPlacesActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
