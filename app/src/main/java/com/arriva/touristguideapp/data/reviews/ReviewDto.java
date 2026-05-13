package com.arriva.touristguideapp.data.reviews;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.util.Date;

@IgnoreExtraProperties
public class ReviewDto {
    @PropertyName(ReviewsFirestoreContract.FIELD_USER_ID)
    private String userId;
    @PropertyName(ReviewsFirestoreContract.FIELD_USER_NAME)
    private String userName;
    @PropertyName(ReviewsFirestoreContract.FIELD_USER_PHOTO_URL)
    private String userPhotoUrl;
    @PropertyName(ReviewsFirestoreContract.FIELD_RATING)
    private float rating;
    @PropertyName(ReviewsFirestoreContract.FIELD_COMMENT)
    private String comment;
    @PropertyName(ReviewsFirestoreContract.FIELD_STATUS)
    private String status;
    @PropertyName(ReviewsFirestoreContract.FIELD_CREATED_AT)
    private Date createdAt;
    @PropertyName(ReviewsFirestoreContract.FIELD_UPDATED_AT)
    private Date updatedAt;

    public ReviewDto() {}

    @PropertyName(ReviewsFirestoreContract.FIELD_USER_ID)
    public String getUserId() { return userId; }
    @PropertyName(ReviewsFirestoreContract.FIELD_USER_ID)
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName(ReviewsFirestoreContract.FIELD_USER_NAME)
    public String getUserName() { return userName; }
    @PropertyName(ReviewsFirestoreContract.FIELD_USER_NAME)
    public void setUserName(String userName) { this.userName = userName; }

    @PropertyName(ReviewsFirestoreContract.FIELD_USER_PHOTO_URL)
    public String getUserPhotoUrl() { return userPhotoUrl; }
    @PropertyName(ReviewsFirestoreContract.FIELD_USER_PHOTO_URL)
    public void setUserPhotoUrl(String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    @PropertyName(ReviewsFirestoreContract.FIELD_RATING)
    public float getRating() { return rating; }
    @PropertyName(ReviewsFirestoreContract.FIELD_RATING)
    public void setRating(float rating) { this.rating = rating; }

    @PropertyName(ReviewsFirestoreContract.FIELD_COMMENT)
    public String getComment() { return comment; }
    @PropertyName(ReviewsFirestoreContract.FIELD_COMMENT)
    public void setComment(String comment) { this.comment = comment; }

    @PropertyName(ReviewsFirestoreContract.FIELD_STATUS)
    public String getStatus() { return status; }
    @PropertyName(ReviewsFirestoreContract.FIELD_STATUS)
    public void setStatus(String status) { this.status = status; }

    @PropertyName(ReviewsFirestoreContract.FIELD_CREATED_AT)
    public Date getCreatedAt() { return createdAt; }
    @PropertyName(ReviewsFirestoreContract.FIELD_CREATED_AT)
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @PropertyName(ReviewsFirestoreContract.FIELD_UPDATED_AT)
    public Date getUpdatedAt() { return updatedAt; }
    @PropertyName(ReviewsFirestoreContract.FIELD_UPDATED_AT)
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
