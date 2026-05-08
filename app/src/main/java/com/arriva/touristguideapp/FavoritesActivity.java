package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerView = findViewById(R.id.rvFavorites);
        emptyLayout = findViewById(R.id.emptyStateFavorites);

        loadFavorites();

        adapter = new FavoriteAdapter(favoritePlaces, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
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
        intent.putExtra("id", place.getId());
        intent.putExtra("name", place.getName());
        intent.putExtra("description", place.getDescription());
        intent.putExtra("category", place.getCategory());
        intent.putExtra("imageResId", place.getImageResId());
        intent.putExtra("tips", place.getTips());
        intent.putExtra("funFact", place.getFunFact());
        intent.putExtra("nearestStation", place.getNearestStation());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
