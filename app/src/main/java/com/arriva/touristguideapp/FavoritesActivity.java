package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends AppCompatActivity implements FavoriteAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private List<Place> favoritePlaces = new ArrayList<>();
    private LinearLayout emptyLayout;
    private TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerView = findViewById(R.id.rvFavorites);
        emptyLayout = findViewById(R.id.emptyStateFavorites);
        tvCount = findViewById(R.id.tvFavoritesCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        View btnExplore = findViewById(R.id.btnExplore);
        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        loadFavorites();

        adapter = new FavoriteAdapter(new ArrayList<>(favoritePlaces), this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1)); // 1 column for modern list
        
        animateEntrance();
    }

    private void animateEntrance() {
        if (recyclerView != null) {
            recyclerView.setAlpha(0f);
            recyclerView.setTranslationY(50f);
            recyclerView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start();
        }
        
        if (emptyLayout != null && emptyLayout.getVisibility() == View.VISIBLE) {
            emptyLayout.setAlpha(0f);
            emptyLayout.setTranslationY(30f);
            emptyLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start();
                
            View btnExplore = findViewById(R.id.btnExplore);
            if (btnExplore != null) {
                btnExplore.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse));
            }
        }
    }

    public void updateCount() {
        if (tvCount != null) {
            tvCount.setText(getString(R.string.favorites_count_format, favoritePlaces.size()));
        }
    }

    private void loadFavorites() {
        Set<String> favoriteIds = FavoritesManager.getFavoriteIds(this);
        List<Place> allPlaces = DataProvider.getPlaces();
        favoritePlaces.clear();

        for (Place p : allPlaces) {
            if (favoriteIds.contains(p.getId())) {
                favoritePlaces.add(p);
            }
        }

        updateCount();

        if (adapter != null) {
            adapter.updateList(new ArrayList<>(favoritePlaces));
        }

        if (favoritePlaces.isEmpty()) {
            if (emptyLayout != null) emptyLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(Place place) {
        Intent intent = new Intent(this, PlaceDetailsActivity.class);
        PlaceIntentExtras.putPlaceDetails(intent, place);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
