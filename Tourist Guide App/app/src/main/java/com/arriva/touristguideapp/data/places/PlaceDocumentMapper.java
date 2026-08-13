package com.arriva.touristguideapp.data.places;

import androidx.annotation.NonNull;

import com.arriva.touristguideapp.Place;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts the app model into the documented Firestore catalog schema. */
final class PlaceDocumentMapper {

    private PlaceDocumentMapper() {
    }

    @NonNull
    static Map<String, Object> toFirestoreDocument(@NonNull Place place) {
        Map<String, Object> map = new HashMap<>();
        map.put(PlacesFirestoreContract.FIELD_NAME, valueOrEmpty(place.getName()));
        map.put(PlacesFirestoreContract.FIELD_STATUS, statusOrDraft(place.getCatalogStatus()));
        map.put(PlacesFirestoreContract.FIELD_CATEGORY, valueOrEmpty(place.getCategory()));
        map.put(PlacesFirestoreContract.FIELD_CATEGORY_ID, valueOrEmpty(place.getCategoryId()));
        map.put(PlacesFirestoreContract.FIELD_CITY, valueOrEmpty(place.getCity()));
        map.put(PlacesFirestoreContract.FIELD_DESCRIPTION, valueOrEmpty(place.getDescription()));
        map.put(PlacesFirestoreContract.FIELD_LATITUDE, place.getLatitude());
        map.put(PlacesFirestoreContract.FIELD_LONGITUDE, place.getLongitude());
        map.put(PlacesFirestoreContract.FIELD_LOCATION,
                new GeoPoint(place.getLatitude(), place.getLongitude()));
        map.put(PlacesFirestoreContract.FIELD_IS_TOP_PICK, place.isTopPick());
        map.put(PlacesFirestoreContract.FIELD_RATING_AVG, place.getRating());
        map.put(PlacesFirestoreContract.FIELD_BUDGET, valueOrEmpty(place.getBudget()));
        map.put(PlacesFirestoreContract.FIELD_CROWD_LEVEL, valueOrEmpty(place.getCrowdLevel()));
        map.put(PlacesFirestoreContract.FIELD_BEST_TIME, valueOrEmpty(place.getBestTime()));
        map.put(PlacesFirestoreContract.FIELD_TIPS, valueOrEmpty(place.getTips()));
        map.put(PlacesFirestoreContract.FIELD_FUN_FACT, valueOrEmpty(place.getFunFact()));
        map.put(PlacesFirestoreContract.FIELD_NEAREST_STATION, valueOrEmpty(place.getNearestStation()));
        map.put(PlacesFirestoreContract.FIELD_TAG, valueOrEmpty(place.getTag()));
        map.put(PlacesFirestoreContract.FIELD_LEGACY_ID, valueOrEmpty(place.getLegacyCatalogId()));

        String imageRef = trimToNull(place.getImageRef());
        if (imageRef != null) {
            map.put(PlacesFirestoreContract.FIELD_IMAGE_REF, imageRef);
        }
        List<String> galleryRefs = place.getGalleryImageRefs();
        if (!galleryRefs.isEmpty()) {
            map.put(PlacesFirestoreContract.FIELD_GALLERY_IMAGE_REFS, galleryRefs);
        }
        return map;
    }

    @NonNull
    private static String statusOrDraft(String status) {
        String value = trimToNull(status);
        return value == null ? PlacesFirestoreContract.STATUS_DRAFT : value;
    }

    @NonNull
    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
