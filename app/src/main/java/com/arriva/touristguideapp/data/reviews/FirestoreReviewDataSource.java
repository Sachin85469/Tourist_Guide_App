package com.arriva.touristguideapp.data.reviews;

import android.util.Log;
import androidx.annotation.NonNull;
import com.arriva.touristguideapp.Review;
import com.arriva.touristguideapp.data.places.PlacesFirestoreContract;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import java.util.Date;
import java.util.List;

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
            float oldRating = isNewReview ? 0 : reviewSnapshot.getDouble(ReviewsFirestoreContract.FIELD_RATING).floatValue();
            float newRating = review.getRating();

            double currentAvg = placeSnapshot.contains(ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING) ? 
                    placeSnapshot.getDouble(ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING) : 0.0;
            long currentTotal = placeSnapshot.contains(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS) ? 
                    placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS) : 0;
            long currentComments = placeSnapshot.contains(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS) ? 
                    placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS) : 0;

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
            } else {
                newAvg = ((currentAvg * currentTotal) - oldRating + newRating) / currentTotal;
                boolean hadComment = reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_COMMENT) != null && 
                                    !reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_COMMENT).trim().isEmpty();
                boolean hasComment = review.getComment() != null && !review.getComment().trim().isEmpty();
                
                if (!hadComment && hasComment) newComments = currentComments + 1;
                else if (hadComment && !hasComment) newComments = currentComments - 1;
            }

            review.setUpdatedAt(new Date());

            transaction.set(reviewRef, review);
            transaction.update(placeRef, 
                ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING, newAvg,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS, newTotal,
                ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS, newComments
            );

            return null;
        }).addOnSuccessListener(aVoid -> Log.i(TAG, "REVIEW_UPLOAD_SUCCESS placeId=" + placeId + " userId=" + review.getUserId()))
          .addOnFailureListener(e -> Log.e(TAG, "REVIEW_UPLOAD_FAILED placeId=" + placeId + " error=" + e.getMessage()));
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

            float rating = reviewSnapshot.getDouble(ReviewsFirestoreContract.FIELD_RATING).floatValue();
            boolean hadComment = reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_COMMENT) != null && 
                                !reviewSnapshot.getString(ReviewsFirestoreContract.FIELD_COMMENT).trim().isEmpty();

            DocumentSnapshot placeSnapshot = transaction.get(placeRef);
            double currentAvg = placeSnapshot.getDouble(ReviewsFirestoreContract.FIELD_PLACE_AVG_RATING);
            long currentTotal = placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_RATINGS);
            long currentComments = placeSnapshot.getLong(ReviewsFirestoreContract.FIELD_PLACE_TOTAL_COMMENTS);

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
        });
    }
}
