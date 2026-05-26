package com.arriva.touristguideapp.data.places;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Immutable Firestore projection for a place document.
 * Only {@link PlacesFirestoreContract#FIELD_STATUS} and {@link PlacesFirestoreContract#FIELD_NAME}
 * are required; every other field is best-effort with warnings on failure.
 */
public final class PlaceDto {

    private static final String TAG = "PlaceDto";

    private static final String DEFAULT_CITY = "Unknown";
    private static final String DEFAULT_CATEGORY = "General";
    private static final String DEFAULT_DESCRIPTION = "";
    private static final String DEFAULT_BUDGET = "Medium";
    private static final String DEFAULT_CROWD = "Moderate";
    private static final String DEFAULT_BEST_TIME = "Day";
    private static final String DEFAULT_TAG = "Popular";
    private static final double DEFAULT_RATING = 0.0;

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
    private final long totalRatings;
    private final long totalComments;
    @Nullable
    private final String imageUrl;
    private final List<String> galleryImageUrls;
    private final String tips;
    private final String funFact;
    private final String nearestStation;
    private final String tag;
    private final boolean topPick;
    @NonNull
    private final String status;
    @Nullable
    private final String legacyId;
    @Nullable
    private final String drawableAssetKey;
    @NonNull
    private final List<String> galleryDrawableKeys;

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
            long totalRatings,
            long totalComments,
            @Nullable String imageUrl,
            List<String> galleryImageUrls,
            String tips,
            String funFact,
            String nearestStation,
            String tag,
            boolean topPick,
            @NonNull String status,
            @Nullable String legacyId,
            @Nullable String drawableAssetKey,
            @NonNull List<String> galleryDrawableKeys
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
        this.totalRatings = totalRatings;
        this.totalComments = totalComments;
        this.imageUrl = imageUrl;
        this.galleryImageUrls = Collections.unmodifiableList(new ArrayList<>(galleryImageUrls));
        this.tips = tips;
        this.funFact = funFact;
        this.nearestStation = nearestStation;
        this.tag = tag;
        this.topPick = topPick;
        this.status = status;
        this.legacyId = legacyId;
        this.drawableAssetKey = drawableAssetKey;
        this.galleryDrawableKeys = Collections.unmodifiableList(new ArrayList<>(galleryDrawableKeys));
    }

    @Nullable
    public static PlaceDto fromSnapshot(DocumentSnapshot snap) {
        if (snap == null || !snap.exists()) {
            return null;
        }
        String documentId = snap.getId();

        try {
            // Requirement 4: Crash Protection / Safe Parsing
            return parseSnapshotFields(snap, documentId);
        } catch (Throwable e) {
            Log.e(TAG, "docId=" + documentId + " CRASH_RECOVERED: fatal_parse_exception", e);
            return tryRecoverMinimalDto(snap, documentId, new Exception(e));
        }
    }

    /**
     * @return true if the snapshot is unusable for the catalog because required fields are absent.
     */
    public static boolean isMissingRequiredFields(@NonNull DocumentSnapshot snap) {
        if (!snap.exists()) {
            return true;
        }
        String documentId = snap.getId();
        String status = readRequiredCoerced(snap, documentId, PlacesFirestoreContract.FIELD_STATUS, false);
        String name = readRequiredCoerced(snap, documentId, PlacesFirestoreContract.FIELD_NAME, false);
        return status == null || name == null;
    }

    /**
     * Merges {@link PlacesFirestoreContract#FIELD_GALLERY_IMAGE_URLS} and {@link PlacesFirestoreContract#FIELD_GALLERY_URLS}
     * in order, de-duplicated. Never throws: missing or malformed fields yield an empty list.
     */
    @NonNull
    private static List<String> mergeGalleryUrlLists(@NonNull DocumentSnapshot snap,
                                                     @NonNull String documentId) {
        try {
            List<String> fromLegacyField = readOptionalStringList(
                    snap, documentId, PlacesFirestoreContract.FIELD_GALLERY_IMAGE_URLS);
            List<String> fromGalleryUrls = readOptionalStringList(
                    snap, documentId, PlacesFirestoreContract.FIELD_GALLERY_URLS);
            LinkedHashSet<String> merged = new LinkedHashSet<>();
            for (String s : fromLegacyField) {
                if (s != null && !s.trim().isEmpty()) {
                    merged.add(s.trim());
                }
            }
            for (String s : fromGalleryUrls) {
                if (s != null && !s.trim().isEmpty()) {
                    merged.add(s.trim());
                }
            }
            return new ArrayList<>(merged);
        } catch (Throwable t) {
            Log.w(TAG, "docId=" + documentId + " mergeGalleryUrlLists failed safely action=empty_list", t);
            return new ArrayList<>();
        }
    }

    @Nullable
    private static PlaceDto parseSnapshotFields(@NonNull DocumentSnapshot snap,
                                                @NonNull String documentId) {
        String status = readRequiredCoerced(snap, documentId, PlacesFirestoreContract.FIELD_STATUS, true);
        String name = readRequiredCoerced(snap, documentId, PlacesFirestoreContract.FIELD_NAME, true);
        if (status == null || name == null) {
            Log.w(TAG, "docId=" + documentId + " SKIPPED_DOCUMENT missing_required_fields statusPresent="
                    + (status != null) + " namePresent=" + (name != null));
            return null;
        }

        String city = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_CITY, DEFAULT_CITY);
        String category = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_CATEGORY, DEFAULT_CATEGORY);
        String categoryId = readOptionalStringOrNull(
                snap, documentId, PlacesFirestoreContract.FIELD_CATEGORY_ID);

        String description = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_DESCRIPTION, DEFAULT_DESCRIPTION);
        String budget = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_BUDGET, DEFAULT_BUDGET);
        String crowdLevel = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_CROWD_LEVEL, DEFAULT_CROWD);
        String bestTime = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_BEST_TIME, DEFAULT_BEST_TIME);

        double latitude = 0d;
        double longitude = 0d;
        Object locRaw = snap.get(PlacesFirestoreContract.FIELD_LOCATION);
        if (locRaw instanceof GeoPoint) {
            GeoPoint geo = (GeoPoint) locRaw;
            latitude = geo.getLatitude();
            longitude = geo.getLongitude();
        } else {
            if (locRaw != null) {
                warnFieldParse(
                        documentId,
                        PlacesFirestoreContract.FIELD_LOCATION,
                        "GeoPoint",
                        locRaw,
                        "falling_back_to_latitude_longitude_fields");
            }
            latitude = readOptionalDouble(
                    snap, documentId, PlacesFirestoreContract.FIELD_LATITUDE, 0d);
            longitude = readOptionalDouble(
                    snap, documentId, PlacesFirestoreContract.FIELD_LONGITUDE, 0d);
        }

        double rating;
        // Priority: avgRating (Reviews) -> ratingAvg (Legacy/Migration) -> rating (Legacy)
        if (hasField(snap, "avgRating")) {
            rating = readOptionalDouble(snap, documentId, "avgRating", DEFAULT_RATING);
            Log.d(TAG, "docId=" + documentId + " RATING_SOURCE: reviews (avgRating=" + rating + ")");
        } else if (hasField(snap, PlacesFirestoreContract.FIELD_RATING_AVG)) {
            rating = readOptionalDouble(
                    snap, documentId, PlacesFirestoreContract.FIELD_RATING_AVG, DEFAULT_RATING);
            Log.d(TAG, "docId=" + documentId + " RATING_SOURCE: migration (ratingAvg=" + rating + ")");
        } else {
            rating = readOptionalDouble(
                    snap, documentId, PlacesFirestoreContract.FIELD_RATING, DEFAULT_RATING);
            Log.d(TAG, "docId=" + documentId + " RATING_SOURCE: fallback (rating=" + rating + ")");
        }

        long totalRatings = (long) readOptionalDouble(snap, documentId, "totalRatings", 0);
        long totalComments = (long) readOptionalDouble(snap, documentId, "totalComments", 0);

        String imageUrl = readOptionalStringOrNull(snap, documentId, PlacesFirestoreContract.FIELD_IMAGE_URL);
        if (imageUrl == null) {
            imageUrl = readOptionalStringOrNull(snap, documentId, PlacesFirestoreContract.FIELD_HERO_IMAGE_URL);
        }

        List<String> gallery = mergeGalleryUrlLists(snap, documentId);

        String tips = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_TIPS, DEFAULT_DESCRIPTION);
        String funFact = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_FUN_FACT, DEFAULT_DESCRIPTION);
        String nearestStation = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_NEAREST_STATION, DEFAULT_DESCRIPTION);
        String tag = readOptionalStringWithDefault(
                snap, documentId, PlacesFirestoreContract.FIELD_TAG, DEFAULT_TAG);

        boolean topPick = readOptionalBoolean(
                snap, documentId, PlacesFirestoreContract.FIELD_IS_TOP_PICK, false);

        String legacyId = readOptionalStringOrNull(snap, documentId, PlacesFirestoreContract.FIELD_LEGACY_ID);

        String drawableAssetKey = readOptionalStringOrNull(
                snap, documentId, PlacesFirestoreContract.FIELD_DRAWABLE_ASSET_KEY);
        List<String> galleryDrawableKeys;
        try {
            galleryDrawableKeys = readOptionalStringList(
                    snap, documentId, PlacesFirestoreContract.FIELD_GALLERY_DRAWABLE_KEYS);
        } catch (Throwable t) {
            Log.w(TAG, "docId=" + documentId + " galleryDrawableKeys read failed safely action=empty_list", t);
            galleryDrawableKeys = new ArrayList<>();
        }

        final String trimmedHero = imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl.trim() : null;
        final String heroLoadSource = trimmedHero != null ? "REMOTE_URL" : "MISSING";
        final String galleryUrlsLoadSource = !gallery.isEmpty() ? "REMOTE_URL" : "MISSING";

        Log.d(TAG, "docId=" + documentId + " PARSE_OK name=" + name
                + " heroLoadSource=" + heroLoadSource
                + " galleryUrlsLoadSource=" + galleryUrlsLoadSource
                + " mergedGalleryUrlCount=" + gallery.size()
                + " hasDrawableAssetKey=" + (drawableAssetKey != null)
                + " galleryDrawableKeyCount=" + galleryDrawableKeys.size());

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
                totalRatings,
                totalComments,
                trimmedHero,
                gallery,
                tips,
                funFact,
                nearestStation,
                tag,
                topPick,
                status,
                legacyId,
                drawableAssetKey,
                galleryDrawableKeys
        );
    }

    @Nullable
    private static PlaceDto tryRecoverMinimalDto(@NonNull DocumentSnapshot snap,
                                                 @NonNull String documentId,
                                                 @NonNull Exception cause) {
        String status = readRequiredCoerced(snap, documentId, PlacesFirestoreContract.FIELD_STATUS, true);
        String name = readRequiredCoerced(snap, documentId, PlacesFirestoreContract.FIELD_NAME, true);
        if (status == null || name == null) {
            return null;
        }
        Log.w(TAG, "docId=" + documentId + " RECOVERED_MINIMAL after="
                + cause.getClass().getSimpleName());
        return new PlaceDto(
                documentId,
                name.trim(),
                DEFAULT_CITY,
                DEFAULT_CATEGORY,
                null,
                DEFAULT_DESCRIPTION,
                DEFAULT_BUDGET,
                DEFAULT_CROWD,
                DEFAULT_BEST_TIME,
                0d,
                0d,
                DEFAULT_RATING,
                0,
                0,
                null,
                new ArrayList<>(),
                DEFAULT_DESCRIPTION,
                DEFAULT_DESCRIPTION,
                DEFAULT_DESCRIPTION,
                DEFAULT_TAG,
                false,
                status,
                null,
                null,
                Collections.emptyList()
        );
    }

    /**
     * Required field: must become a non-empty string after coercion (Firestore often stores numbers as Long).
     */
    @Nullable
    private static String readRequiredCoerced(@NonNull DocumentSnapshot snap,
                                              @NonNull String docId,
                                              @NonNull String field,
                                              boolean logOnReject) {
        Object raw = snap.get(field);
        String value = coerceToNonEmptyString(raw);
        if (value != null) {
            return value;
        }
        if (!logOnReject) {
            return null;
        }
        if (raw == null) {
            Log.w(TAG, "docId=" + docId + " required field missing field=" + field
                    + " expected=non_empty_text actualType=null");
        } else {
            warnFieldParse(docId, field, "non_empty String|Number|Boolean|Timestamp", raw,
                    "required_field_rejected");
        }
        return null;
    }

    @Nullable
    private static String readOptionalStringOrNull(@NonNull DocumentSnapshot snap,
                                                   @NonNull String docId,
                                                   @NonNull String field) {
        Object raw = snap.get(field);
        String value = coerceToNonEmptyString(raw);
        if (value != null) {
            return value;
        }
        if (raw == null) {
            return null;
        }
        if (raw instanceof String && ((String) raw).trim().isEmpty()) {
            Log.w(TAG, "docId=" + docId + " optional field empty field=" + field
                    + " expected=non_empty_string actualType=String");
            return null;
        }
        warnFieldParse(docId, field, "String|Number|Boolean|Timestamp", raw,
                "optional_string_unusable action=null");
        return null;
    }

    @NonNull
    private static String readOptionalStringWithDefault(@NonNull DocumentSnapshot snap,
                                                        @NonNull String docId,
                                                        @NonNull String field,
                                                        @NonNull String fallback) {
        Object raw = snap.get(field);
        String value = coerceToNonEmptyString(raw);
        if (value != null) {
            return value;
        }
        if (raw instanceof String && ((String) raw).trim().isEmpty()) {
            Log.w(TAG, "docId=" + docId + " optional field empty field=" + field
                    + " expected=non_empty_string actualType=String action=use_fallback");
            return fallback;
        }
        if (raw != null) {
            warnFieldParse(docId, field, "String|Number|Boolean|Timestamp", raw,
                    "optional_string_unusable action=use_fallback");
        }
        return fallback;
    }

    /**
     * Accepts String, whole/fractional numbers, Boolean, Timestamp; returns null if missing or unusable.
     */
    @Nullable
    private static String coerceToNonEmptyString(@Nullable Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            return s.isEmpty() ? null : s;
        }
        if (raw instanceof Number) {
            Number n = (Number) raw;
            if (n instanceof Double || n instanceof Float) {
                double d = n.doubleValue();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return null;
                }
                long asLong = (long) d;
                if (asLong == d) {
                    return String.valueOf(asLong);
                }
                return String.valueOf(d);
            }
            return String.valueOf(n.longValue());
        }
        if (raw instanceof Boolean) {
            return ((Boolean) raw) ? "true" : "false";
        }
        if (raw instanceof Timestamp) {
            return String.valueOf(((Timestamp) raw).getSeconds());
        }
        if (raw instanceof java.util.Date) {
            return String.valueOf(((java.util.Date) raw).getTime());
        }
        return null;
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
            double v = ((Number) raw).doubleValue();
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                warnFieldParse(docId, field, "finite Number", raw, "action=use_fallback");
                return fallback;
            }
            return v;
        }
        if (raw instanceof String) {
            String value = ((String) raw).trim();
            if (value.isEmpty()) {
                Log.w(TAG, "docId=" + docId + " optional field=" + field
                        + " expected=Number|non_empty_String actualType=String(empty) action=use_fallback");
                return fallback;
            }
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException nfe) {
                warnFieldParse(docId, field, "Number|parseable String", raw,
                        "NumberFormatException action=use_fallback");
                return fallback;
            }
        }
        if (raw instanceof Boolean) {
            return ((Boolean) raw) ? 1d : 0d;
        }
        warnFieldParse(docId, field, "Number|String|Boolean", raw, "action=use_fallback");
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
        if (raw instanceof Number) {
            return ((Number) raw).longValue() != 0L;
        }
        if (raw instanceof String) {
            String value = ((String) raw).trim();
            if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
                return false;
            }
            warnFieldParse(docId, field, "Boolean|Number|true/false/0/1 String", raw,
                    "action=use_fallback");
            return fallback;
        }
        warnFieldParse(docId, field, "Boolean|Number|String", raw, "action=use_fallback");
        return fallback;
    }

    @NonNull
    private static List<String> readOptionalStringList(@NonNull DocumentSnapshot snap,
                                                       @NonNull String docId,
                                                       @NonNull String field) {
        Object raw = snap.get(field);
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            if (!s.isEmpty()) {
                out.add(s);
            } else {
                Log.w(TAG, "docId=" + docId + " optional field=" + field
                        + " expected=List|non_empty_String actualType=String(empty) action=empty_list");
            }
            return out;
        }
        if (!(raw instanceof List<?>)) {
            warnFieldParse(docId, field, "List|String", raw, "action=empty_list");
            return out;
        }
        List<?> list = (List<?>) raw;
        int index = 0;
        for (Object item : list) {
            String element = coerceToNonEmptyString(item);
            if (element != null) {
                out.add(element);
            } else if (item != null) {
                warnFieldParse(
                        docId,
                        field + "[" + index + "]",
                        "String|Number|Boolean|Timestamp",
                        item,
                        "skipped_array_item");
            }
            index++;
        }
        return out;
    }

    private static boolean hasField(@NonNull DocumentSnapshot snap, @NonNull String field) {
        return snap.get(field) != null;
    }

    private static void warnFieldParse(@NonNull String docId,
                                       @NonNull String field,
                                       @NonNull String expectedType,
                                       @Nullable Object raw,
                                       @NonNull String detail) {
        String actualType = raw == null ? "null" : raw.getClass().getName();
        Log.w(TAG, "docId=" + docId + " field=" + field + " expected=" + expectedType
                + " actualType=" + actualType + " " + detail);
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

    public long getTotalRatings() {
        return totalRatings;
    }

    public long getTotalComments() {
        return totalComments;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getGalleryImageUrls() {
        return galleryImageUrls;
    }

    /**
     * Same ordered list as {@link #getGalleryImageUrls()} — supports Firestore field name {@code galleryUrls}.
     */
    @NonNull
    public List<String> getGalleryUrls() {
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

    @NonNull
    public String getStatus() {
        return status;
    }

    @Nullable
    public String getLegacyId() {
        return legacyId;
    }

    @Nullable
    public String getDrawableAssetKey() {
        return drawableAssetKey;
    }

    @NonNull
    public List<String> getGalleryDrawableKeys() {
        return galleryDrawableKeys;
    }
}
