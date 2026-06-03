package com.arriva.touristguideapp;

import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.ArrayList;

/**
 * Writes {@link PlaceDetailsActivity} intent extras consistently (exclusively remote URLs).
 */
public final class PlaceIntentExtras {

    private PlaceIntentExtras() {
    }

    public static void putPlaceDetails(@NonNull Intent intent, @NonNull Place place) {
        intent.putExtra("id", place.getId());
        intent.putExtra("name", place.getName());
        intent.putExtra("description", place.getDescription());
        intent.putExtra("category", place.getCategory());
        intent.putExtra("budget", place.getBudget());
        intent.putExtra("crowdLevel", place.getCrowdLevel());
        intent.putExtra("bestTime", place.getBestTime());
        intent.putExtra("tips", place.getTips());
        intent.putExtra("funFact", place.getFunFact());
        intent.putExtra("nearestStation", place.getNearestStation());
        intent.putExtra("city", place.getCity());
        intent.putExtra("tag", place.getTag());
        intent.putExtra("lat", place.getLatitude());
        intent.putExtra("lng", place.getLongitude());
        intent.putExtra("avgRating", place.getRating());
        intent.putExtra("totalRatings", place.getTotalRatings());
        intent.putExtra("totalComments", place.getTotalComments());

        String hero = place.getImageUrl();
        if (hero != null && !hero.trim().isEmpty()) {
            intent.putExtra("imageUrl", hero.trim());
        }

        ArrayList<String> gallery = new ArrayList<>();
        for (String u : place.getGalleryUrls()) {
            if (u != null && !u.trim().isEmpty()) {
                gallery.add(u.trim());
            }
        }
        if (hero != null && !hero.trim().isEmpty()) {
            String h = hero.trim();
            if (gallery.isEmpty()) {
                gallery.add(h);
            } else if (!gallery.contains(h)) {
                gallery.add(0, h);
            }
        }
        if (!gallery.isEmpty()) {
            intent.putStringArrayListExtra("galleryImageUrls", gallery);
            intent.putStringArrayListExtra("galleryUrls", new ArrayList<>(gallery));
        }
    }
}
