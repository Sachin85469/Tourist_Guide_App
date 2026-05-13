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
     * Realtime listener for reviews of a place.
     */
    public ListenerRegistration listenToReviews(String placeId, EventListener<QuerySnapshot> listener) {
        Log.d(TAG, "REVIEW_LISTENER_ATTACHED placeId=" + placeId);
        return db.collection(PlacesFirestoreContract.COLLECTION_PLACES)
                .document(placeId)
                .collection(ReviewsFirestoreContract.SUB_COLLECTION_REVIEWS)
                .orderBy(ReviewsFirestoreContract.FIELD_CREATED_AT, Query.Direction.DESCENDING)
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
     */
    public Task<Void> submitReview(String placeId, Review review) {
        DocumentReference placeRef = db.collection(PlacesFirestoreContract.COLLECTION_PLACES).document(placeId);
        DocumentReference reviewRef = placeRef.collection(ReviewsFirestoreContract.SUB_COLLECTION_REVIEWS).document(review.getUserId());

        return db.runTransaction(transaction -> {
            DocumentSnapshot placeSnapshot = transaction.get(placeRef);
            DocumentSnapshot reviewSnapshot = transaction.get(reviewRef);

            boolean isNewReview = !reviewSnapshot.exists();
            float oldRating = 0;
            if (!isNewReview) {
                Double r = reviewSnapshot.getDouble(ReviewsFirestoreContract.FIELD_RATING);
                oldRating = r != null ? r.floatValue() : 0;
            }
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
                review.setCreatedAt(new Date());
                Log.d(TAG, "REVIEW_CREATED placeId=" + placeId);
            } else {
                newAvg = ((currentAvg * currentTotal) - oldRating + newRating) / currentTotal;
                String oldComment = reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_COMMENT);
                boolean hadComment = oldComment != null && !oldComment.trim().isEmpty();
                boolean hasComment = review.getComment() != null && !review.getComment().trim().isEmpty();
                
                if (!hadComment && hasComment) newComments = currentComments + 1;
                else if (hadComment && !hasComment) newComments = currentComments - 1;
                Log.d(TAG, "REVIEW_UPDATED placeId=" + placeId);
            }

            review.setUpdatedAt(new Date());

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

            long newTotal = currentTotal - 1;
            double newAvg = newTotal > 0 ? ((currentAvg * currentTotal) - rating) / newTotal : 0.0;
            long newComments = hadComment ? currentComments - 1 : currentComments;

            transaction.delete(reviewRef);
            transaction.update(placeRef, 
                ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING, newAvg,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS, newTotal,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS, newComments
            );

            return null;
        }).continueWithTask(task -> {
            if (task.isSuccessful()) {
                return Tasks.forResult(null);
            } else {
                Exception e = task.getException();
                return Tasks.forException(e != null ? e : new Exception("Delete transaction failed"));
            }
        });
    }
}
