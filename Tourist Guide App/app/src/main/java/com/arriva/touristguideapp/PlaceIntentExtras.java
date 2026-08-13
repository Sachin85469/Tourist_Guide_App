package com.arriva.touristguideapp;

import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * Writes {@link PlaceDetailsActivity} intent extras consistently.
 * Place images are resolved from Firebase Storage references.
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
        intent.putExtra("imageRef", place.getImageRef());
        intent.putStringArrayListExtra("galleryImageRefs", new java.util.ArrayList<>(place.getGalleryImageRefs()));
    }
}
