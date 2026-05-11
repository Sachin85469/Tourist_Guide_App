package com.arriva.touristguideapp.data.places;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arriva.touristguideapp.Place;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps {@link PlaceDto} to the existing {@link Place} model without breaking drawable-based UIs.
 * Remote rows use a system placeholder drawable for {@link Place#getImageResId()} until adapters load URLs.
 */
public final class PlaceMapper {

    private static final String TAG = "PlaceMapper";

    /** Drawable used when Firestore supplies URLs so {@link Place#getImageResId()} stays valid. */
    private static final int REMOTE_PLACEHOLDER_RES = android.R.drawable.ic_menu_gallery;

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
                    REMOTE_PLACEHOLDER_RES,
                    dto.getTips(),
                    dto.getFunFact(),
                    dto.getNearestStation(),
                    dto.getTag()
            );
            place.setRating(dto.getRating());
            place.setTopPick(dto.isTopPick());
            place.setImageUrl(dto.getImageUrl());
            place.setGalleryImageUrls(new ArrayList<>(dto.getGalleryImageUrls()));
            place.setCategoryId(dto.getCategoryId());
            place.setCatalogStatus(dto.getStatus());
            place.setLegacyCatalogId(dto.getLegacyId());

            boolean hasRemoteImages = dto.getImageUrl() != null || !dto.getGalleryImageUrls().isEmpty();
            if (hasRemoteImages) {
                place.setGalleryImages(new ArrayList<>());
            }

            Log.d(TAG, "toPlace OK docId=" + id + " imageUrlSet=" + (dto.getImageUrl() != null));
            return place;
        } catch (Exception e) {
            Log.e(TAG, "Mapping failure for documentId=" + dto.getDocumentId(), e);
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
