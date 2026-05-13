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
        
        // In a real production app, unique users tracking would involve a sub-collection or a distinct set.
        // For simplicity, we just increment a counter if userId is provided (not truly unique per user yet).
        if (userId != null) {
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
