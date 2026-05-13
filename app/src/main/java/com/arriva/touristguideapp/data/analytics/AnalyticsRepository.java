package com.arriva.touristguideapp.data.analytics;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Clean interface for logging analytics from the UI.
 */
public class AnalyticsRepository {
    private final AnalyticsDataSource dataSource;
    private static final java.util.Map<String, Long> lastViewTime = new java.util.HashMap<>();
    private static final long VIEW_COOLDOWN = 60000; // 1 minute

    public AnalyticsRepository() {
        this.dataSource = new AnalyticsDataSource();
    }

    public void trackPlaceView(String placeId) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastViewTime.get(placeId);
        if (lastTime != null && currentTime - lastTime < VIEW_COOLDOWN) {
            return; // Avoid spamming views
        }
        lastViewTime.put(placeId, currentTime);

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

    public void logSearchClick(String query, String placeId) {
        dataSource.logSearchClick(query, placeId);
    }

    public void updateTrendingScore(String placeId, double views, double avgRating, int totalReviews, int favorites) {
        dataSource.updateTrendingScore(placeId, views, avgRating, totalReviews, favorites);
    }

    public void getMostViewedPlaces(int limit, AnalyticsDataSource.OnAnalyticsLoadedListener<java.util.List<java.util.Map<java.lang.String, Object>>> listener) {
        dataSource.getMostViewedPlaces(limit, listener);
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
