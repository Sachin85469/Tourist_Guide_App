package com.example.touristguideapp;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView rvFavorites;
    private PlaceAdapter adapter;
    private List<Place> favoriteList;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        rvFavorites = findViewById(R.id.rvFavorites);
        emptyState = findViewById(R.id.emptyStateFavorites);

        favoriteList = new ArrayList<>();
        adapter = new PlaceAdapter(favoriteList, place -> {
            android.content.Intent intent = new android.content.Intent(this, PlaceDetailsActivity.class);
            intent.putExtra("id", place.getId());
            intent.putExtra("name", place.getName());
            intent.putExtra("description", place.getDescription());
            intent.putExtra("category", place.getCategory());
            intent.putExtra("budget", place.getBudget());
            intent.putExtra("crowdLevel", place.getCrowdLevel());
            intent.putExtra("bestTime", place.getBestTime());
            intent.putExtra("imageResId", place.getImageResId());
            intent.putExtra("tips", place.getTips());
            intent.putExtra("funFact", place.getFunFact());
            intent.putExtra("nearestStation", place.getNearestStation());
            intent.putExtra("lat", place.getLatitude());
            intent.putExtra("lng", place.getLongitude());
            startActivity(intent);
        });

        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        List<Place> allPlaces = DataProvider.getPlaces();
        List<Place> favList = new ArrayList<>();

        for (Place p : allPlaces) {
            if (FavoritesManager.isFavorite(this, p.getId())) {
                favList.add(p);
            }
        }
        
        favoriteList = favList;
        adapter.updateList(favoriteList);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (favoriteList.isEmpty()) {
            rvFavorites.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvFavorites.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }
}
