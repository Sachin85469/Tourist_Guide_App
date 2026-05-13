package com.arriva.touristguideapp.data.reviews;

import com.arriva.touristguideapp.Review;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public final class ReviewMapper {

    private ReviewMapper() {}

    public static Review fromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            android.util.Log.w("ReviewMapper", "REVIEW_RAW_DOCUMENT: null or non-existent");
            return null;
        }

        android.util.Log.d("ReviewMapper", "REVIEW_RAW_DOCUMENT: id=" + doc.getId() + " data=" + doc.getData());
        
        try {
            ReviewDto dto = doc.toObject(ReviewDto.class);
            if (dto == null) {
                android.util.Log.e("ReviewMapper", "REVIEW_PARSE_FAILED: toObject returned null for " + doc.getId());
                return null;
            }
            // Ensure userId is set even if missing in document body
            if (dto.getUserId() == null) {
                dto.setUserId(doc.getId());
            }
            return toReview(dto);
        } catch (Exception e) {
            android.util.Log.e("ReviewMapper", "REVIEW_PARSE_FAILED: exception=" + e.getMessage() + " for " + doc.getId());
            return null;
        }
    }

    public static Review toReview(ReviewDto dto) {
        if (dto == null) {
            android.util.Log.w("ReviewMapper", "REVIEW_PARSE_FAILED: dto is null");
            return null;
        }
        
        if (dto.getUserId() == null) {
            android.util.Log.w("ReviewMapper", "REVIEW_SKIPPED: missing userId");
            return null;
        }

        Review review = new Review();
        review.setUserId(dto.getUserId());
        review.setUserName(dto.getUserName() != null ? dto.getUserName() : "Anonymous");
        review.setUserPhotoUrl(dto.getUserPhotoUrl());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        
        // Handle legacy status
        String status = dto.getStatus();
        if (status == null) {
            status = Review.STATUS_ACTIVE;
        }
        review.setStatus(status);
        
        // Filter out non-active reviews for regular users
        // Note: Admin filtering should happen elsewhere or via different DTO
        if (Review.STATUS_HIDDEN.equals(status) || Review.STATUS_REMOVED.equals(status)) {
            android.util.Log.d("ReviewMapper", "REVIEW_SKIPPED: status=" + status + " userId=" + dto.getUserId());
            return null;
        }

        review.setCreatedAt(dto.getCreatedAt());
        review.setUpdatedAt(dto.getUpdatedAt());
        
        android.util.Log.d("ReviewMapper", "REVIEW_PARSE_SUCCESS: userId=" + dto.getUserId());
        return review;
    }

    public static List<Review> fromSnapshots(List<DocumentSnapshot> snapshots) {
        List<Review> reviews = new ArrayList<>();
        if (snapshots == null) return reviews;
        
        android.util.Log.d("ReviewMapper", "REVIEW_FIRESTORE_FETCH: count=" + snapshots.size());
        
        for (DocumentSnapshot snap : snapshots) {
            Review r = fromSnapshot(snap);
            if (r != null) {
                reviews.add(r);
            } else {
                android.util.Log.w("ReviewMapper", "REVIEW_SKIPPED_REASON: Parsing failed or status filtered for " + snap.getId());
            }
        }
        return reviews;
    }

    public static List<Review> toReviews(List<ReviewDto> dtos) {
        List<Review> reviews = new ArrayList<>();
        if (dtos == null) return reviews;
        for (ReviewDto dto : dtos) {
            android.util.Log.d("ReviewMapper", "REVIEW_DOC_FETCHED docId=" + (dto != null ? dto.getUserId() : "null"));
            Review r = toReview(dto);
            if (r != null) {
                reviews.add(r);
            }
        }
        return reviews;
    }
}
