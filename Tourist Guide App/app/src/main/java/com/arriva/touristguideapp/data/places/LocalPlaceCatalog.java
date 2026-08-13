package com.arriva.touristguideapp.data.places;

import androidx.annotation.NonNull;

import com.arriva.touristguideapp.DataProvider;
import com.arriva.touristguideapp.Place;

import java.util.List;

/**
 * Local static catalog used when Firestore is empty or unavailable.
 * Delegates to existing {@link DataProvider} — do not remove DataProvider during migration.
 */
public class LocalPlaceCatalog {

    @NonNull
    public List<Place> getAllPlaces() {
        // Return empty list to avoid using hardcoded fallback data; rely on Firestore.
        return new java.util.ArrayList<>();
    }

    @NonNull
    public List<Place> getTopPicks() {
        return new java.util.ArrayList<>();
    }

    @NonNull
    public List<Place> getPlacesByCategory(@NonNull String category) {
        return new java.util.ArrayList<>();
    }
}
