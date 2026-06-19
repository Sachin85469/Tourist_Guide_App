package com.arriva.touristguideapp.data.places;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.arriva.touristguideapp.Place;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps {@link PlaceDto} to the existing {@link Place} model.
 * Updated to exclusively use remote URLs.
 */
public final class PlaceMapper {

    private static final String TAG = "PlaceMapper";

    private PlaceMapper() {}

    @Nullable
    public static Place toPlace(@Nullable PlaceDto dto) {
        if (dto == null) return null;
        
        try {
            String id = dto.getDocumentId();
            // Defensive Null Handling (Requirement 4)
            if (id == null) id = java.util.UUID.randomUUID().toString();
            String name = dto.getName() != null ? dto.getName() : "Unknown Place";

            Place place = new Place(
                    id,
                    name,
                    dto.getCity(),
                    dto.getCategory(),
                    dto.getDescription(),
                    dto.getBudget(),
                    dto.getCrowdLevel(),
                    dto.getBestTime(),
                    dto.getLatitude(),
                    dto.getLongitude(),
                    dto.getTips(),
                    dto.getFunFact(),
                    dto.getNearestStation(),
                    dto.getTag()
            );
            place.setRating(dto.getRating());
            place.setTotalRatings(dto.getTotalRatings());
            place.setTotalComments(dto.getTotalComments());
            place.setTopPick(dto.isTopPick());
            place.setCategoryId(dto.getCategoryId());
            place.setLegacyCatalogId(dto.getLegacyId());
            return place;
        } catch (Exception e) {
            Log.e(TAG, "toPlace FAILED for docId=" + dto.getDocumentId(), e);
            return null;
        }
    }

    @NonNull
    public static List<Place> toPlaces(@NonNull List<PlaceDto> dtos) {
        List<Place> out = new ArrayList<>();
        for (PlaceDto dto : dtos) {
            Place p = toPlace(dto);
            if (p != null) out.add(p);
        }
        return out;
    }

    @NonNull
    public static List<PlaceDto> fromSnapshots(@NonNull List<com.google.firebase.firestore.DocumentSnapshot> snapshots) {
        List<PlaceDto> dtos = new ArrayList<>();
        for (com.google.firebase.firestore.DocumentSnapshot snap : snapshots) {
            PlaceDto dto = PlaceDto.fromSnapshot(snap);
            if (dto != null) dtos.add(dto);
        }
        return dtos;
    }
}
