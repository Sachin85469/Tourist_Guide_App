package com.arriva.touristguideapp.data.places;

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

        String name = snap.getString(PlacesFirestoreContract.FIELD_NAME);
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String city = defaultString(snap.getString(PlacesFirestoreContract.FIELD_CITY), "Unknown");
        String category = defaultString(snap.getString(PlacesFirestoreContract.FIELD_CATEGORY), "General");
        String categoryId = snap.getString(PlacesFirestoreContract.FIELD_CATEGORY_ID);

        String description = defaultString(
                snap.getString(PlacesFirestoreContract.FIELD_DESCRIPTION),
                ""
        );
        String budget = defaultString(snap.getString(PlacesFirestoreContract.FIELD_BUDGET), "Medium");
        String crowdLevel = defaultString(snap.getString(PlacesFirestoreContract.FIELD_CROWD_LEVEL), "Moderate");
        String bestTime = defaultString(snap.getString(PlacesFirestoreContract.FIELD_BEST_TIME), "Day");

        double latitude;
        double longitude;
        GeoPoint geo = snap.getGeoPoint(PlacesFirestoreContract.FIELD_LOCATION);
        if (geo != null) {
            latitude = geo.getLatitude();
            longitude = geo.getLongitude();
        } else {
            Double lat = snap.getDouble(PlacesFirestoreContract.FIELD_LATITUDE);
            Double lng = snap.getDouble(PlacesFirestoreContract.FIELD_LONGITUDE);
            latitude = lat != null ? lat : 0d;
            longitude = lng != null ? lng : 0d;
        }

        double rating = 4.0;
        Double r = snap.getDouble(PlacesFirestoreContract.FIELD_RATING_AVG);
        if (r == null) {
            r = snap.getDouble(PlacesFirestoreContract.FIELD_RATING);
        }
        if (r != null) {
            rating = r;
        }

        String imageUrl = snap.getString(PlacesFirestoreContract.FIELD_IMAGE_URL);
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = snap.getString(PlacesFirestoreContract.FIELD_HERO_IMAGE_URL);
        }

        List<String> gallery = new ArrayList<>();
        Object galleryRaw = snap.get(PlacesFirestoreContract.FIELD_GALLERY_IMAGE_URLS);
        if (galleryRaw instanceof List<?>) {
            for (Object o : (List<?>) galleryRaw) {
                if (o instanceof String) {
                    String u = (String) o;
                    if (!u.trim().isEmpty()) {
                        gallery.add(u.trim());
                    }
                }
            }
        }

        String tips = defaultString(snap.getString(PlacesFirestoreContract.FIELD_TIPS), "");
        String funFact = defaultString(snap.getString(PlacesFirestoreContract.FIELD_FUN_FACT), "");
        String nearestStation = defaultString(snap.getString(PlacesFirestoreContract.FIELD_NEAREST_STATION), "");
        String tag = defaultString(snap.getString(PlacesFirestoreContract.FIELD_TAG), "Popular");

        Boolean top = snap.getBoolean(PlacesFirestoreContract.FIELD_IS_TOP_PICK);
        boolean topPick = top != null && top;

        String status = snap.getString(PlacesFirestoreContract.FIELD_STATUS);
        String legacyId = snap.getString(PlacesFirestoreContract.FIELD_LEGACY_ID);

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

    private static String defaultString(@Nullable String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
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
