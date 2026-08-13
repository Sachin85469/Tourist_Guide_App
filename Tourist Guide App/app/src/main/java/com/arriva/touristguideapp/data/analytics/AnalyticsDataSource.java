package com.arriva.touristguideapp.data.analytics;

import android.util.Log;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Data source for tracking analytics events in Firestore.
 */
public class AnalyticsDataSource {
    private static final String TAG = "AnalyticsDataSource";
    private final FirebaseFirestore db;

    public AnalyticsDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Increments the view count for a place and updates timestamps.
     */
    public void trackPlaceView(String placeId, String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(AnalyticsFirestoreContract.FIELD_TOTAL_VIEWS, FieldValue.increment(1));
        updates.put(AnalyticsFirestoreContract.FIELD_LAST_VIEWED_AT, FieldValue.serverTimestamp());
        
        // Tracking daily and weekly views using nested fields for simplicity in this version
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        updates.put(AnalyticsFirestoreContract.FIELD_DAILY_VIEWS, FieldValue.increment(1));
        updates.put(AnalyticsFirestoreContract.FIELD_WEEKLY_VIEWS, FieldValue.increment(1));

        if (userId != null) {
            // For production, this should check if the user has already viewed to be truly "unique"
            updates.put(AnalyticsFirestoreContract.FIELD_UNIQUE_USERS, FieldValue.increment(1));
        }

        db.collection(AnalyticsFirestoreContract.COLLECTION_PLACE_VIEWS)
                .document(placeId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "PLACE_VIEW_TRACKED: " + placeId))
                .addOnFailureListener(e -> Log.e(TAG, "FAILED_TO_TRACK_VIEW: " + e.getMessage()));
    }

    /**
     * Logs search analytics.
     */
    public void logSearch(String query, int resultCount) {
        Map<String, Object> data = new HashMap<>();
        data.put(AnalyticsFirestoreContract.FIELD_QUERY, query);
        data.put(AnalyticsFirestoreContract.FIELD_RESULT_COUNT, resultCount);
        data.put(AnalyticsFirestoreContract.FIELD_TIMESTAMP, FieldValue.serverTimestamp());

        db.collection(AnalyticsFirestoreContract.COLLECTION_SEARCHES)
                .add(data)
                .addOnSuccessListener(ref -> Log.d(TAG, "SEARCH_ANALYTICS_TRACKED: " + query))
                .addOnFailureListener(e -> Log.e(TAG, "FAILED_TO_LOG_SEARCH: " + e.getMessage()));
    }

    /**
     * Logs when a user clicks a search result.
     */
    public void logSearchClick(String query, String placeId) {
        Map<String, Object> data = new HashMap<>();
        data.put(AnalyticsFirestoreContract.FIELD_QUERY, query);
        data.put(AnalyticsFirestoreContract.FIELD_CLICKED_PLACE_ID, placeId);
        data.put(AnalyticsFirestoreContract.FIELD_TIMESTAMP, FieldValue.serverTimestamp());

        db.collection(AnalyticsFirestoreContract.COLLECTION_SEARCHES)
                .add(data)
                .addOnSuccessListener(ref -> Log.d(TAG, "SEARCH_CLICK_TRACKED: " + placeId));
    }

    /**
     * Updates the trending score for a place.
     */
    public void updateTrendingScore(String placeId, double views, double avgRating, int totalReviews, int favorites) {
        // Simple trending algorithm: weight views, ratings, and engagement
        double score = (views * 0.4) + (avgRating * 10) + (totalReviews * 5) + (favorites * 8);
        
        db.collection(AnalyticsFirestoreContract.COLLECTION_PLACE_VIEWS)
                .document(placeId)
                .update(AnalyticsFirestoreContract.FIELD_TRENDING_SCORE, score)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "TRENDING_SCORE_UPDATED: " + placeId + " Score: " + score));
    }

    /**
     * Fetches top viewed places.
     */
    public void getMostViewedPlaces(int limit, OnAnalyticsLoadedListener<java.util.List<Map<String, Object>>> listener) {
        db.collection(AnalyticsFirestoreContract.COLLECTION_PLACE_VIEWS)
                .orderBy(AnalyticsFirestoreContract.FIELD_TOTAL_VIEWS, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("placeId", doc.getId());
                            results.add(data);
                        }
                    }
                    listener.onLoaded(results);
                });
    }

    public interface OnAnalyticsLoadedListener<T> {
        void onLoaded(T data);
    }

    /**
     * Tracks general engagement events.
     */
    public void trackEngagement(String userId, String eventType, String placeId) {
        Map<String, Object> data = new HashMap<>();
        data.put(AnalyticsFirestoreContract.FIELD_USER_ID, userId);
        data.put(AnalyticsFirestoreContract.FIELD_EVENT_TYPE, eventType);
        if (placeId != null) {
            data.put(AnalyticsFirestoreContract.FIELD_PLACE_ID, placeId);
        }
        data.put(AnalyticsFirestoreContract.FIELD_TIMESTAMP, FieldValue.serverTimestamp());

        db.collection(AnalyticsFirestoreContract.COLLECTION_USER_ENGAGEMENT)
                .add(data);
    }
}
