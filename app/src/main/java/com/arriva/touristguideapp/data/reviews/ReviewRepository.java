package com.arriva.touristguideapp.data.reviews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.arriva.touristguideapp.Review;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing reviews.
 */
public class ReviewRepository {

    public interface ReviewsCallback {
        void onReviewsLoaded(@NonNull List<Review> reviews, @Nullable String error);
    }

    public interface SingleReviewCallback {
        void onReviewLoaded(@Nullable Review review, @Nullable String error);
    }

    public interface PlaceUpdateCallback {
        void onPlaceUpdated(double avgRating, long totalRatings, long totalComments);
    }

    private final FirestoreReviewDataSource dataSource;

    public ReviewRepository() {
        this.dataSource = new FirestoreReviewDataSource();
    }

    /**
     * Realtime listener for reviews.
     */
    public ListenerRegistration listenToReviews(String placeId, ReviewsCallback callback) {
        return dataSource.listenToReviews(placeId, (value, error) -> {
            if (error != null) {
                callback.onReviewsLoaded(new ArrayList<>(), error.getMessage());
                return;
            }
            if (value != null) {
                List<ReviewDto> dtos = value.toObjects(ReviewDto.class);
                callback.onReviewsLoaded(ReviewMapper.toReviews(dtos), null);
            }
        });
    }

    /**
     * Realtime listener for place aggregates.
     */
    public ListenerRegistration listenToPlace(String placeId, PlaceUpdateCallback callback) {
        return dataSource.listenToPlace(placeId, (snap, error) -> {
            if (error != null || snap == null || !snap.exists()) return;
            
            double avg = 0.0;
            if (snap.contains(ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING)) {
                Double d = snap.getDouble(ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING);
                if (d != null) avg = d;
            }

            long total = 0;
            if (snap.contains(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS)) {
                Long l = snap.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS);
                if (l != null) total = l;
            }

            long comments = 0;
            if (snap.contains(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS)) {
                Long l = snap.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS);
                if (l != null) comments = l;
            }

            callback.onPlaceUpdated(avg, total, comments);
        });
    }

    public Task<Void> submitReview(String placeId, Review review) {
        // Validation
        if (review.getRating() < 1 || review.getRating() > 5) {
            return com.google.android.gms.tasks.Tasks.forException(new IllegalArgumentException("Rating must be between 1 and 5"));
        }
        if (review.getComment() != null) {
            review.setComment(review.getComment().trim());
        }
        return dataSource.submitReview(placeId, review);
    }

    public void fetchReviews(String placeId, ReviewsCallback callback) {
        dataSource.fetchReviews(placeId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<ReviewDto> dtos = task.getResult().toObjects(ReviewDto.class);
                callback.onReviewsLoaded(ReviewMapper.toReviews(dtos), null);
            } else {
                callback.onReviewsLoaded(new ArrayList<>(), task.getException() != null ? task.getException().getMessage() : "Fetch failed");
            }
        });
    }

    public void getUserReview(String placeId, String userId, SingleReviewCallback callback) {
        dataSource.getUserReview(placeId, userId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ReviewDto dto = task.getResult().toObject(ReviewDto.class);
                callback.onReviewLoaded(ReviewMapper.toReview(dto), null);
            } else {
                callback.onReviewLoaded(null, task.getException() != null ? task.getException().getMessage() : "Fetch failed");
            }
        });
    }

    public Task<Void> deleteReview(String placeId, String userId) {
        return dataSource.deleteReview(placeId, userId);
    }
    
    // updateReview is functionally same as submitReview due to the document ID being userId
    public Task<Void> updateReview(String placeId, Review review) {
        return submitReview(placeId, review);
    }
}
