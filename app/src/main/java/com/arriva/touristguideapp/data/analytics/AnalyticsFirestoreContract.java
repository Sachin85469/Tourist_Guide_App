package com.arriva.touristguideapp.data.analytics;

/**
 * Firestore collection and field names for the analytics system.
 */
public final class AnalyticsFirestoreContract {

    public static final String COLLECTION_PLACE_VIEWS = "analytics_place_views";
    public static final String COLLECTION_SEARCHES = "analytics_searches";
    public static final String COLLECTION_USER_ENGAGEMENT = "analytics_user_engagement";

    // Place Views fields
    public static final String FIELD_TOTAL_VIEWS = "totalViews";
    public static final String FIELD_UNIQUE_USERS = "uniqueUsers";
    public static final String FIELD_LAST_VIEWED_AT = "lastViewedAt";
    public static final String FIELD_DAILY_VIEWS = "dailyViews";
    public static final String FIELD_WEEKLY_VIEWS = "weeklyViews";

    // Search fields
    public static final String FIELD_QUERY = "query";
    public static final String FIELD_RESULT_COUNT = "resultCount";
    public static final String FIELD_CLICKED_PLACE_ID = "clickedPlaceId";
    public static final String FIELD_TIMESTAMP = "timestamp";

    // Engagement fields
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_EVENT_TYPE = "eventType";
    public static final String FIELD_PLACE_ID = "placeId";
    
    public static final String EVENT_FAVORITE_ADDED = "favorite_added";
    public static final String EVENT_REVIEW_SUBMITTED = "review_submitted";
    public static final String EVENT_PLACE_OPENED = "place_opened";
    public static final String EVENT_CATEGORY_EXPLORED = "category_explored";

    private AnalyticsFirestoreContract() {}
}
