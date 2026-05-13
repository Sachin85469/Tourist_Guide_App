package com.arriva.touristguideapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class PlaceDetailsActivity extends AppCompatActivity {

    private String placeId;
    private ImageView btnFavorite;
    private double lat;
    private double lng;
    private ViewPager2 viewPagerGallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_details);

        viewPagerGallery = findViewById(R.id.viewPagerGallery);
        TextView tvName = findViewById(R.id.detailName);
        TextView tvCategory = findViewById(R.id.detailCategory);
        TextView tvDescription = findViewById(R.id.detailDescription);
        TextView tvBudget = findViewById(R.id.detailBudget);
        TextView tvCrowd = findViewById(R.id.detailCrowd);
        TextView tvBestTime = findViewById(R.id.detailBestTime);
        TextView tvTips = findViewById(R.id.detailTips);
        TextView tvFunFact = findViewById(R.id.detailFunFact);
        TextView tvStation = findViewById(R.id.detailStation);
        Button btnDirections = findViewById(R.id.btnDirections);
        
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
            lat = intent.getDoubleExtra("lat", 0);
            lng = intent.getDoubleExtra("lng", 0);

            // Remote / gallery fields
            String imageUrl = intent.getStringExtra("imageUrl");
            ArrayList<String> galleryImageUrls = intent.getStringArrayListExtra("galleryImageUrls");
            ArrayList<String> galleryUrlsExtra = intent.getStringArrayListExtra("galleryUrls");
            ArrayList<String> mergedGalleryUrls = mergeGalleryUrlExtras(galleryImageUrls, galleryUrlsExtra);
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                String u = imageUrl.trim();
                if (mergedGalleryUrls.isEmpty()) {
                    mergedGalleryUrls.add(u);
                } else if (!mergedGalleryUrls.contains(u)) {
                    mergedGalleryUrls.add(0, u);
                }
            }

            String drawableAssetKey = intent.getStringExtra("drawableAssetKey");
            ArrayList<String> galleryDrawableKeys = intent.getStringArrayListExtra("galleryDrawableKeys");
            ArrayList<String> drawableKeysForAdapter = new ArrayList<>();
            if (galleryDrawableKeys != null) {
                for (String k : galleryDrawableKeys) {
                    if (k != null && !k.trim().isEmpty()) {
                        drawableKeysForAdapter.add(k.trim());
                    }
                }
            }
            if (mergedGalleryUrls.isEmpty() && drawableKeysForAdapter.isEmpty()
                    && drawableAssetKey != null && !drawableAssetKey.trim().isEmpty()) {
                drawableKeysForAdapter.add(drawableAssetKey.trim());
            }

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
            
            // Show fun fact popup
            new AlertDialog.Builder(this)
                .setTitle("Did you know?")
                .setMessage(funFact != null ? funFact : "Did you know? Pune is amazing!")
                .setPositiveButton("Cool!", null)
                .show();

            // Setup Gallery — remote URLs first, then drawable keys, then local ints (all via Glide)
            List<Integer> galleryDrawables = new ArrayList<>();
            boolean hasRemoteGallery = !mergedGalleryUrls.isEmpty();

            if (!hasRemoteGallery) {
                List<Place> allPlaces = DataProvider.getPlaces();
                for (Place p : allPlaces) {
                    if (p.getId() != null && p.getId().equals(placeId)) {
                        galleryDrawables.addAll(p.getGalleryImages());
                        break;
                    }
                }
            }

            if (!hasRemoteGallery && galleryDrawables.isEmpty() && imageResId != 0) {
                galleryDrawables.add(imageResId);
            }

            GalleryAdapter galleryAdapter;
            if (hasRemoteGallery) {
                galleryAdapter = new GalleryAdapter(galleryDrawables, mergedGalleryUrls, null);
            } else if (!drawableKeysForAdapter.isEmpty()) {
                galleryAdapter = new GalleryAdapter(galleryDrawables, null, drawableKeysForAdapter);
            } else {
                galleryAdapter = new GalleryAdapter(galleryDrawables);
            }
            if (viewPagerGallery != null) {
                viewPagerGallery.setAdapter(galleryAdapter);
            }

            updateFavoriteIcon();
            
            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> {
                    v.animate()
                        .scaleX(1.3f)
                        .scaleY(1.3f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(150);
                        });

                    FavoritesManager.toggleFavorite(this, placeId);
                    updateFavoriteIcon();
                });
            }

            if (btnDirections != null) {
                btnDirections.setOnClickListener(v -> {
                    Uri uri = Uri.parse("google.navigation:q=" + lat + "," + lng);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        // Fallback to any browser if Maps app is not available
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
                });
            }
        }
    }

    @NonNull
    private static ArrayList<String> mergeGalleryUrlExtras(@Nullable ArrayList<String> a,
                                                           @Nullable ArrayList<String> b) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (a != null) {
            for (String s : a) {
                if (s != null && !s.trim().isEmpty()) {
                    set.add(s.trim());
                }
            }
        }
        if (b != null) {
            for (String s : b) {
                if (s != null && !s.trim().isEmpty()) {
                    set.add(s.trim());
                }
            }
        }
        return new ArrayList<>(set);
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
