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

    private PlaceMapper() {
    }

    @Nullable
    public static Place toPlace(@Nullable PlaceDto dto) {
        if (dto == null) {
            return null;
        }
        try {
            String id = dto.getDocumentId();

            Place place = new Place(
                    id,
                    dto.getName(),
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
            place.setTopPick(dto.isTopPick());
            place.setImageUrl(dto.getImageUrl());

            List<String> galleryUrls = new ArrayList<>(dto.getGalleryUrls());
            place.setGalleryImageUrls(galleryUrls);
            place.setCategoryId(dto.getCategoryId());
            place.setCatalogStatus(dto.getStatus());
            place.setLegacyCatalogId(dto.getLegacyId());

            boolean hasRemoteHero = dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty();
            boolean hasRemoteGallery = !galleryUrls.isEmpty();

            Log.d(TAG, "toPlace OK docId=" + id
                    + " imageUrlSet=" + hasRemoteHero
                    + " galleryUrlCount=" + galleryUrls.size());
            return place;
        } catch (Exception e) {
            Log.e(TAG, "toPlace FAILED for docId=" + dto.getDocumentId()
                    + " name=" + dto.getName()
                    + " exceptionType=" + e.getClass().getSimpleName()
                    + " message=" + e.getMessage(), e);
            return null;
        }
    }

    @NonNull
    public static List<Place> toPlaces(@NonNull List<PlaceDto> dtos) {
        List<Place> out = new ArrayList<>();
        for (PlaceDto dto : dtos) {
            Place p = toPlace(dto);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }
}
