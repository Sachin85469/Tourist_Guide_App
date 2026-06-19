package com.arriva.touristguideapp.data.places;

/**
 * Firestore collection and field names for the places catalog.
 * Keep in sync with security rules and console indexes.
 */
public final class PlacesFirestoreContract {

    public static final String COLLECTION_PLACES = "places";

    public static final String FIELD_STATUS = "status";
    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_ARCHIVED = "archived";

    public static final String FIELD_IS_TOP_PICK = "isTopPick";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_CATEGORY_ID = "categoryId";

    public static final String FIELD_NAME = "name";
    public static final String FIELD_CITY = "city";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_BUDGET = "budget";
    public static final String FIELD_CROWD_LEVEL = "crowdLevel";
    public static final String FIELD_BEST_TIME = "bestTime";
    public static final String FIELD_LATITUDE = "latitude";
    public static final String FIELD_LONGITUDE = "longitude";
    public static final String FIELD_LOCATION = "location";

    public static final String FIELD_RATING = "rating";
    public static final String FIELD_RATING_AVG = "ratingAvg";

    public static final String FIELD_TIPS = "tips";
    public static final String FIELD_FUN_FACT = "funFact";
    public static final String FIELD_NEAREST_STATION = "nearestStation";
    public static final String FIELD_TAG = "tag";
    public static final String FIELD_LEGACY_ID = "legacyId";
    public static final String FIELD_GALLERY_URLS = "galleryUrls";

    private PlacesFirestoreContract() {
    }
}
