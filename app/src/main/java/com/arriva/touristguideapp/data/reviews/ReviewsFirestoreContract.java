package com.arriva.touristguideapp.data.reviews;

import com.arriva.touristguideapp.data.places.PlacesFirestoreContract;

/**
 * Firestore collection and field names for the reviews system.
 */
public final class ReviewsFirestoreContract {

    /** Parent collection path: {@value PlacesFirestoreContract#COLLECTION_PLACES}/{placeId}/reviews */
    public static final String SUB_COLLECTION_REVIEWS = "reviews";

    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_USER_NAME = "userName";
    public static final String FIELD_USER_PHOTO_URL = "userPhotoUrl";
    public static final String FIELD_RATING = "rating";
    public static final String FIELD_COMMENT = "comment";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_PLACE_ID = "placeId";
    public static final String FIELD_PLACE_NAME = "placeName";
    public static final String FIELD_REVIEW_ID = "reviewId";
    public static final String FIELD_PLACE_IMAGE_URL = "placeImageUrl";

    /** Fields on the parent place document to support fast summaries. */
    public static final String FIELD_PLACE_AVG_RATING = "avgRating";
    public static final String FIELD_PLACE_TOTAL_RATINGS = "totalRatings";
    public static final String FIELD_PLACE_TOTAL_COMMENTS = "totalComments";

    private ReviewsFirestoreContract() {
    }
}
