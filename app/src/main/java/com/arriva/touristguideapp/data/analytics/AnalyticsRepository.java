package com.arriva.touristguideapp.data.analytics;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Clean interface for logging analytics from the UI.
 */
public class AnalyticsRepository {
    private final AnalyticsDataSource dataSource;

    public AnalyticsRepository() {
        this.dataSource = new AnalyticsDataSource();
    }

    public void trackPlaceView(String placeId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (user != null) ? user.getUid() : null;
        dataSource.trackPlaceView(placeId, userId);
        
        if (userId != null) {
            dataSource.trackEngagement(userId, AnalyticsFirestoreContract.EVENT_PLACE_OPENED, placeId);
        }
    }

    public void logSearch(String query, int count) {
        dataSource.logSearch(query, count);
    }

    public void trackReviewSubmitted(String placeId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            dataSource.trackEngagement(user.getUid(), AnalyticsFirestoreContract.EVENT_REVIEW_SUBMITTED, placeId);
        }
    }

    public void trackFavoriteAdded(String placeId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            dataSource.trackEngagement(user.getUid(), AnalyticsFirestoreContract.EVENT_FAVORITE_ADDED, placeId);
        }
    }

    public void trackCategoryExplored(String category) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            dataSource.trackEngagement(user.getUid(), AnalyticsFirestoreContract.EVENT_CATEGORY_EXPLORED, category);
        }
    }
}
