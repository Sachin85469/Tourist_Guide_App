package com.arriva.touristguideapp;

import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.utils.ImageUtils;

import java.util.Locale;

public final class PlaceImageHelper {

    private PlaceImageHelper() {
    }

    public static void clear(@NonNull ImageView imageView) {
        ImageUtils.clear(imageView);
    }

    /**
     * Returns a bundled drawable resource for places that need a local override
     * instead of Firebase Storage (e.g. missing/broken cloud images).
     * Returns 0 when no override applies.
     *
     * Matching priority:
     * 1. Stable Firestore document ID (from the exported catalog).
     * 2. Name-contains fallback for resilience against ID changes.
     */
    @DrawableRes
    public static int getBundledPlaceDrawable(@NonNull Place place) {
        return getBundledPlaceDrawable(place.getId(), place.getName());
    }

    @DrawableRes
    public static int getBundledPlaceDrawable(@Nullable String placeId, @Nullable String name) {
        // 1. Match by Firestore document ID (stable per-catalog key).
        //    Dagdusheth Ganpati: doc ID "2"
        //    ISKCON Temple:      doc ID "-1446420468"
        if ("2".equals(placeId)) {
            return R.drawable.place_dagdusheth_halwai;
        }
        if ("-1446420468".equals(placeId)) {
            return R.drawable.place_iskcon_nvcc;
        }

        // 2. Fallback: case-insensitive name match (resilient against doc ID changes).
        String normalized = name != null ? name.trim().toLowerCase(Locale.ROOT) : "";
        if (normalized.contains("dagdusheth")) {
            return R.drawable.place_dagdusheth_halwai;
        }
        if (normalized.contains("iskcon")) {
            return R.drawable.place_iskcon_nvcc;
        }

        return 0;
    }

    /**
     * Loads the place thumbnail into the given ImageView, using a bundled local
     * drawable override for the two known places and falling through to Firebase
     * Storage for everything else.
     */
    public static void loadThumbnail(@NonNull ImageView imageView, @NonNull Place place) {
        int override = getBundledPlaceDrawable(place);
        if (override != 0) {
            ImageUtils.loadDrawable(imageView, override);
            return;
        }
        ImageUtils.loadPlaceMainImage(imageView, place);
    }

}
