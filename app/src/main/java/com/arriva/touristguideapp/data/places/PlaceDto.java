package com.arriva.touristguideapp.data.places;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable Firestore projection for a place document.
 * Parsing stays defensive so partial or legacy-shaped documents do not crash the app.
 */
public final class PlaceDto {

    private static final String TAG = "PlaceDto";

    private final String documentId;
    private final String name;
    private final String city;
    private final String category;
    @Nullable
    private final String categoryId;
    private final String description;
    private final String budget;
    private final String crowdLevel;
    private final String bestTime;
    private final double latitude;
    private final double longitude;
    private final double rating;
    @Nullable
    private final String imageUrl;
    private final List<String> galleryImageUrls;
    private final String tips;
    private final String funFact;
    private final String nearestStation;
    private final String tag;
    private final boolean topPick;
    @Nullable
    private final String status;
    @Nullable
    private final String legacyId;

    public PlaceDto(
            String documentId,
            String name,
            String city,
            String category,
            @Nullable String categoryId,
            String description,
            String budget,
            String crowdLevel,
            String bestTime,
            double latitude,
            double longitude,
            double rating,
            @Nullable String imageUrl,
            List<String> galleryImageUrls,
            String tips,
            String funFact,
            String nearestStation,
            String tag,
            boolean topPick,
            @Nullable String status,
            @Nullable String legacyId
    ) {
        this.documentId = documentId;
        this.name = name;
        this.city = city;
        this.category = category;
        this.categoryId = categoryId;
        this.description = description;
        this.budget = budget;
        this.crowdLevel = crowdLevel;
        this.bestTime = bestTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.galleryImageUrls = Collections.unmodifiableList(new ArrayList<>(galleryImageUrls));
        this.tips = tips;
        this.funFact = funFact;
        this.nearestStation = nearestStation;
        this.tag = tag;
        this.topPick = topPick;
        this.status = status;
        this.legacyId = legacyId;
    }

    @Nullable
    public static PlaceDto fromSnapshot(DocumentSnapshot snap) {
        if (snap == null || !snap.exists()) {
            return null;
        }
        String documentId = snap.getId();

        String status = readRequiredString(snap, documentId, PlacesFirestoreContract.FIELD_STATUS);
        String name = readRequiredString(snap, documentId, PlacesFirestoreContract.FIELD_NAME);
        if (status == null || name == null) {
            Log.w(TAG, "Skipping document " + documentId + " because required field is missing/invalid");
            return null;
        }

        String city = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_CITY, "Unknown");
        String category = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_CATEGORY, "General");
        String categoryId = readOptionalStringOrNull(snap, documentId, PlacesFirestoreContract.FIELD_CATEGORY_ID);

        String description = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_DESCRIPTION, "");
        String budget = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_BUDGET, "Medium");
        String crowdLevel = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_CROWD_LEVEL, "Moderate");
        String bestTime = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_BEST_TIME, "Day");

        double latitude;
        double longitude;
        GeoPoint geo = snap.getGeoPoint(PlacesFirestoreContract.FIELD_LOCATION);
        if (geo != null) {
            latitude = geo.getLatitude();
            longitude = geo.getLongitude();
        } else {
            latitude = readOptionalDouble(snap, documentId, PlacesFirestoreContract.FIELD_LATITUDE, 0d);
            longitude = readOptionalDouble(snap, documentId, PlacesFirestoreContract.FIELD_LONGITUDE, 0d);
        }

        double rating = readOptionalDouble(snap, documentId, PlacesFirestoreContract.FIELD_RATING_AVG, 4.0);
        if (!hasField(snap, PlacesFirestoreContract.FIELD_RATING_AVG)) {
            rating = readOptionalDouble(snap, documentId, PlacesFirestoreContract.FIELD_RATING, 4.0);
        }

        String imageUrl = readOptionalStringOrNull(snap, documentId, PlacesFirestoreContract.FIELD_IMAGE_URL);
        if (imageUrl == null) {
            imageUrl = readOptionalStringOrNull(snap, documentId, PlacesFirestoreContract.FIELD_HERO_IMAGE_URL);
        }

        List<String> gallery = readOptionalStringArray(
                snap,
                documentId,
                PlacesFirestoreContract.FIELD_GALLERY_IMAGE_URLS
        );

        String tips = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_TIPS, "");
        String funFact = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_FUN_FACT, "");
        String nearestStation = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_NEAREST_STATION, "");
        String tag = readOptionalString(snap, documentId, PlacesFirestoreContract.FIELD_TAG, "Popular");

        boolean topPick = readOptionalBoolean(snap, documentId, PlacesFirestoreContract.FIELD_IS_TOP_PICK, false);

        String legacyId = readOptionalStringOrNull(snap, documentId, PlacesFirestoreContract.FIELD_LEGACY_ID);

        return new PlaceDto(
                documentId,
                name.trim(),
                city,
                category,
                categoryId,
                description,
                budget,
                crowdLevel,
                bestTime,
                latitude,
                longitude,
                rating,
                imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl.trim() : null,
                gallery,
                tips,
                funFact,
                nearestStation,
                tag,
                topPick,
                status,
                legacyId
        );
    }

    @Nullable
    private static String readRequiredString(@NonNull DocumentSnapshot snap,
                                             @NonNull String docId,
                                             @NonNull String field) {
        Object raw = snap.get(field);
        if (raw == null) {
            Log.w(TAG, "docId=" + docId + " required field missing: " + field);
            return null;
        }
        if (!(raw instanceof String)) {
            Log.w(TAG, "docId=" + docId + " invalid required field type field=" + field
                    + " actualType=" + raw.getClass().getSimpleName());
            return null;
        }
        String value = ((String) raw).trim();
        if (value.isEmpty()) {
            Log.w(TAG, "docId=" + docId + " required field empty: " + field);
            return null;
        }
        return value;
    }

    @Nullable
    private static String readOptionalStringOrNull(@NonNull DocumentSnapshot snap,
                                                    @NonNull String docId,
                                                    @NonNull String field) {
        Object raw = snap.get(field);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String)) {
            logOptionalTypeMismatch(docId, field, raw);
            return null;
        }
        String value = ((String) raw).trim();
        if (value.isEmpty()) {
            Log.w(TAG, "docId=" + docId + " skipped optional field=" + field + " reason=empty_string");
            return null;
        }
        return value;
    }

    @NonNull
    private static String readOptionalString(@NonNull DocumentSnapshot snap,
                                             @NonNull String docId,
                                             @NonNull String field,
                                             @NonNull String fallback) {
        String value = readOptionalStringOrNull(snap, docId, field);
        return value != null ? value : fallback;
    }

    private static double readOptionalDouble(@NonNull DocumentSnapshot snap,
                                             @NonNull String docId,
                                             @NonNull String field,
                                             double fallback) {
        Object raw = snap.get(field);
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        if (raw instanceof String) {
            String value = ((String) raw).trim();
            if (value.isEmpty()) {
                Log.w(TAG, "docId=" + docId + " skipped optional field=" + field + " reason=empty_string");
                return fallback;
            }
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "docId=" + docId + " malformed optional field=" + field
                        + " value=" + value + " expected=double");
                return fallback;
            }
        }
        logOptionalTypeMismatch(docId, field, raw);
        return fallback;
    }

    private static boolean readOptionalBoolean(@NonNull DocumentSnapshot snap,
                                               @NonNull String docId,
                                               @NonNull String field,
                                               boolean fallback) {
        Object raw = snap.get(field);
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        if (raw instanceof String) {
            String value = ((String) raw).trim();
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }
            Log.w(TAG, "docId=" + docId + " malformed optional field=" + field
                    + " value=" + value + " expected=boolean");
            return fallback;
        }
        logOptionalTypeMismatch(docId, field, raw);
        return fallback;
    }

    @NonNull
    private static List<String> readOptionalStringArray(@NonNull DocumentSnapshot snap,
                                                        @NonNull String docId,
                                                        @NonNull String field) {
        Object raw = snap.get(field);
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        if (!(raw instanceof List<?>)) {
            logOptionalTypeMismatch(docId, field, raw);
            return out;
        }
        List<?> list = (List<?>) raw;
        for (Object item : list) {
            if (!(item instanceof String)) {
                String itemType = item == null ? "null" : item.getClass().getSimpleName();
                Log.w(TAG, "docId=" + docId + " skipped optional field=" + field
                        + " reason=invalid_array_item_type itemType=" + itemType);
                continue;
            }
            String value = ((String) item).trim();
            if (value.isEmpty()) {
                Log.w(TAG, "docId=" + docId + " skipped optional field=" + field
                        + " reason=empty_array_item");
                continue;
            }
            out.add(value);
        }
        return out;
    }

    private static boolean hasField(@NonNull DocumentSnapshot snap, @NonNull String field) {
        return snap.get(field) != null;
    }

    private static void logOptionalTypeMismatch(@NonNull String docId,
                                                @NonNull String field,
                                                @NonNull Object raw) {
        Log.w(TAG, "docId=" + docId + " invalid optional field type field=" + field
                + " actualType=" + raw.getClass().getSimpleName()
                + " action=skipped_optional_field");
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getCategory() {
        return category;
    }

    @Nullable
    public String getCategoryId() {
        return categoryId;
    }

    public String getDescription() {
        return description;
    }

    public String getBudget() {
        return budget;
    }

    public String getCrowdLevel() {
        return crowdLevel;
    }

    public String getBestTime() {
        return bestTime;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getRating() {
        return rating;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getGalleryImageUrls() {
        return galleryImageUrls;
    }

    public String getTips() {
        return tips;
    }

    public String getFunFact() {
        return funFact;
    }

    public String getNearestStation() {
        return nearestStation;
    }

    public String getTag() {
        return tag;
    }

    public boolean isTopPick() {
        return topPick;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    @Nullable
    public String getLegacyId() {
        return legacyId;
    }
}
