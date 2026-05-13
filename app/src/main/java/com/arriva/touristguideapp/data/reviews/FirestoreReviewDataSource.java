package com.arriva.touristguideapp.data.reviews;

import android.util.Log;
import com.arriva.touristguideapp.Review;
import com.arriva.touristguideapp.data.places.PlacesFirestoreContract;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Date;

/**
 * Data source for interacting with Firestore reviews.
 */
public class FirestoreReviewDataSource {

    private static final String TAG = "FirestoreReviewDataSource";
    private final FirebaseFirestore db;

    public FirestoreReviewDataSource() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Realtime listener for reviews of a place with pagination support.
     * BUG FIX: Removed status filter to allow legacy reviews (without status field) to appear.
     * Filtering is now handled in ReviewMapper.
     */
    public ListenerRegistration listenToReviews(String placeId, int limit, EventListener<QuerySnapshot> listener) {
        Log.d(TAG, "REVIEW_LISTENER_ATTACHED placeId=" + placeId + " limit=" + limit);
        return db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                .document(placeId)
                .collection(ReviewsFirestoreContract.SUB_COLLECTION_REVIEWS)
                .orderBy(ReviewsFirestoreContract.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener(listener);
    }

    /**
     * Realtime listener for the parent place document (to watch ratings change).
     */
    public ListenerRegistration listenToPlace(String placeId, EventListener<DocumentSnapshot> listener) {
        return db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                .document(placeId)
                .addSnapshotListener(listener);
    }

    /**
     * Submits or updates a review for a place.
     * Uses a transaction to update aggregate ratings on the parent place document.
     * Ensure mandatory fields are ALWAYS present and aggregates are consistent.
     */
    public Task<Void> submitReview(String placeId, Review review) {
        DocumentReference placeRef = db.collection(PlacesFirestoreContract.COLLECTION_PLACES).document(placeId);
        DocumentReference reviewRef = placeRef.collection(ReviewsFirestoreContract.SUB_COLLECTION_REVIEWS).document(review.getUserId());

        return db.runTransaction(transaction -> {
            DocumentSnapshot placeSnapshot = transaction.get(placeRef);
            DocumentSnapshot reviewSnapshot = transaction.get(reviewRef);

            boolean isNewReview = !reviewSnapshot.exists();
            float oldRating = 0;
            String oldStatus = Review.STATUS_ACTIVE;
            
            if (!isNewReview) {
                Double r = reviewSnapshot.getDouble(ReviewsFirestoreContract.FIELD_RATING);
                oldRating = r != null ? r.floatValue() : 0;
                String s = reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_STATUS);
                if (s != null) oldStatus = s;
            }
            
            boolean wasActive = !isNewReview && Review.STATUS_ACTIVE.equals(oldStatus);
            float newRating = review.getRating();

            double currentAvg = 0.0;
            if (placeSnapshot.contains(ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING)) {
                Double a = placeSnapshot.getDouble(ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING);
                if (a != null) currentAvg = a;
            }

            long currentTotal = 0;
            if (placeSnapshot.contains(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS)) {
                Long t = placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS);
                if (t != null) currentTotal = t;
            }

            long currentComments = 0;
            if (placeSnapshot.contains(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS)) {
                Long c = placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS);
                if (c != null) currentComments = c;
            }

            double newAvg;
            long newTotal = currentTotal;
            long newComments = currentComments;

            if (isNewReview) {
                newTotal = currentTotal + 1;
                newAvg = ((currentAvg * currentTotal) + newRating) / newTotal;
                if (review.getComment() != null && !review.getComment().trim().isEmpty()) {
                    newComments = currentComments + 1;
                }
                if (review.getCreatedAt() == null) review.setCreatedAt(new Date());
                Log.d(TAG, "REVIEW_CREATED placeId=" + placeId);
            } else {
                // If it was already active, just adjust the average. 
                // If it was NOT active, it's a "new" contribution to the visible total.
                if (wasActive) {
                    newAvg = ((currentAvg * currentTotal) - oldRating + newRating) / currentTotal;
                } else {
                    newTotal = currentTotal + 1;
                    newAvg = ((currentAvg * currentTotal) + newRating) / newTotal;
                }
                
                String oldComment = reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_COMMENT);
                boolean hadComment = wasActive && oldComment != null && !oldComment.trim().isEmpty();
                boolean hasComment = review.getComment() != null && !review.getComment().trim().isEmpty();
                
                if (!hadComment && hasComment) newComments = currentComments + 1;
                else if (hadComment && !hasComment) newComments = currentComments - 1;

                Date originalCreatedAt = reviewSnapshot.getDate(ReviewsFirestoreContract.FIELD_CREATED_AT);
                if (originalCreatedAt != null) review.setCreatedAt(originalCreatedAt);
                else if (review.getCreatedAt() == null) review.setCreatedAt(new Date());

                Log.d(TAG, "REVIEW_UPDATED placeId=" + placeId + " wasActive=" + wasActive);
            }

            review.setUpdatedAt(new Date());
            review.setStatus(Review.STATUS_ACTIVE); // Editing always restores to active

            transaction.set(reviewRef, review);
            transaction.update(placeRef, 
                ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING, newAvg,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS, newTotal,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS, newComments
            );

            return null;
        }).continueWithTask(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "REVIEW_UPLOAD_SUCCESS placeId=" + placeId + " userId=" + review.getUserId());
                return Tasks.forResult(null);
            } else {
                Exception e = task.getException();
                Log.e(TAG, "REVIEW_UPLOAD_FAILED placeId=" + placeId + " error=" + (e != null ? e.getMessage() : "unknown"));
                return Tasks.forException(e != null ? e : new Exception("Transaction failed"));
            }
        });
    }

    public Task<QuerySnapshot> fetchReviews(String placeId) {
        return db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                .document(placeId)
                .collection(ReviewsFirestoreContract.SUB_COLLECTION_REVIEWS)
                .orderBy(ReviewsFirestoreContract.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> Log.i(TAG, "REVIEW_FETCH_SUCCESS count=" + queryDocumentSnapshots.size()))
                .addOnFailureListener(e -> Log.e(TAG, "REVIEW_FETCH_FAILED error=" + e.getMessage()));
    }

    public Task<DocumentSnapshot> getUserReview(String placeId, String userId) {
        return db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                .document(placeId)
                .collection(ReviewsFirestoreContract.SUB_COLLECTION_REVIEWS)
                .document(userId)
                .get();
    }

    public Task<Void> deleteReview(String placeId, String userId) {
        DocumentReference placeRef = db.collection(PlacesFirestoreContract.COLLECTION_PLACES).document(placeId);
        DocumentReference reviewRef = placeRef.collection(ReviewsFirestoreContract.SUB_COLLECTION_REVIEWS).document(userId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot reviewSnapshot = transaction.get(reviewRef);
            if (!reviewSnapshot.exists()) return null;

            float rating = 0;
            Double r = reviewSnapshot.getDouble(ReviewsFirestoreContract.FIELD_RATING);
            if (r != null) rating = r.floatValue();

            String oldComment = reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_COMMENT);
            boolean hadComment = oldComment != null && !oldComment.trim().isEmpty();
            String oldStatus = reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_STATUS);
            boolean wasActive = Review.STATUS_ACTIVE.equals(oldStatus) || oldStatus == null;

            DocumentSnapshot placeSnapshot = transaction.get(placeRef);
            
            double currentAvg = 0.0;
            Double a = placeSnapshot.getDouble(ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING);
            if (a != null) currentAvg = a;

            long currentTotal = 0;
            Long t = placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS);
            if (t != null) currentTotal = t;

            long currentComments = 0;
            Long c = placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS);
            if (c != null) currentComments = c;

            long newTotal = wasActive ? currentTotal - 1 : currentTotal;
            double newAvg = newTotal > 0 ? ((currentAvg * currentTotal) - (wasActive ? rating : 0)) / newTotal : 0.0;
            long newComments = (wasActive && hadComment) ? currentComments - 1 : currentComments;

            transaction.delete(reviewRef);
            transaction.update(placeRef, 
                ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING, newAvg,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS, newTotal,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS, newComments
            );

            return null;
        }).continueWithTask(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "REVIEW_DELETED placeId=" + placeId + " userId=" + userId);
                return Tasks.forResult(null);
            } else {
                Exception e = task.getException();
                Log.e(TAG, "REVIEW_DELETE_FAILED placeId=" + placeId + " userId=" + userId + " error=" + (e != null ? e.getMessage() : "unknown"));
                return Tasks.forException(e != null ? e : new Exception("Delete transaction failed"));
            }
        });
    }

    public Task<Void> reportReview(String placeId, String reviewUserId, String reporterId, String reason) {
        java.util.Map<String, Object> report = new java.util.HashMap<>();
        report.put("placeId", placeId);
        report.put("reviewUserId", reviewUserId);
        report.put("reporterId", reporterId);
        report.put("reason", reason);
        report.put("timestamp", new Date());
        report.put("status", "pending");

        return db.collection("reports").add(report).continueWithTask(task -> {
            if (task.isSuccessful()) {
                // Also mark the review as reported
                return db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                        .document(placeId)
                        .collection(ReviewsFirestoreContract.SUB_COLLECTION_REVIEWS)
                        .document(reviewUserId)
                        .update(ReviewsFirestoreContract.FIELD_STATUS, Review.STATUS_REPORTED);
            }
            return Tasks.forException(task.getException());
        });
    }

    public Task<Void> updateReviewStatus(String placeId, String userId, String newStatus) {
        DocumentReference placeRef = db.collection(PlacesFirestoreContract.COLLECTION_PLACES).document(placeId);
        DocumentReference reviewRef = placeRef.collection(ReviewsFirestoreContract.SUB_COLLECTION_REVIEWS).document(userId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot reviewSnapshot = transaction.get(reviewRef);
            if (!reviewSnapshot.exists()) return null;

            String oldStatus = reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_STATUS);
            if (oldStatus == null) oldStatus = Review.STATUS_ACTIVE;
            
            if (oldStatus.equals(newStatus)) return null;

            boolean wasActive = Review.STATUS_ACTIVE.equals(oldStatus);
            boolean isBecomingActive = Review.STATUS_ACTIVE.equals(newStatus);

            if (wasActive == isBecomingActive) {
                // Both are non-active (e.g., reported -> hidden), just update status
                transaction.update(reviewRef, ReviewsFirestoreContract.FIELD_STATUS, newStatus);
                return null;
            }

            // Status transition affects aggregates
            float rating = 0;
            Double r = reviewSnapshot.getDouble(ReviewsFirestoreContract.FIELD_RATING);
            if (r != null) rating = r.floatValue();

            String comment = reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_COMMENT);
            boolean hasComment = comment != null && !comment.trim().isEmpty();

            DocumentSnapshot placeSnapshot = transaction.get(placeRef);
            double currentAvg = 0.0;
            Double a = placeSnapshot.getDouble(ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING);
            if (a != null) currentAvg = a;

            long currentTotal = 0;
            Long t = placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS);
            if (t != null) currentTotal = t;

            long currentComments = 0;
            Long c = placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS);
            if (c != null) currentComments = c;

            long newTotal;
            double newAvg;
            long newComments;

            if (wasActive) { // Active -> Non-Active
                newTotal = currentTotal - 1;
                newAvg = newTotal > 0 ? ((currentAvg * currentTotal) - rating) / newTotal : 0.0;
                newComments = hasComment ? currentComments - 1 : currentComments;
            } else { // Non-Active -> Active
                newTotal = currentTotal + 1;
                newAvg = ((currentAvg * currentTotal) + rating) / newTotal;
                newComments = hasComment ? currentComments + 1 : currentComments;
            }

            transaction.update(reviewRef, ReviewsFirestoreContract.FIELD_STATUS, newStatus);
            transaction.update(placeRef, 
                ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING, newAvg,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS, newTotal,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS, newComments
            );

            return null;
        });
    }
}
