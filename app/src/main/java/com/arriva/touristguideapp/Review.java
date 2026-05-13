package com.arriva.touristguideapp;

import androidx.annotation.Nullable;
import java.io.Serializable;
import java.util.Date;

/**
 * Data model for a user review.
 */
public class Review implements Serializable {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_HIDDEN = "hidden";
    public static final String STATUS_REPORTED = "reported";
    public static final String STATUS_REMOVED = "removed";

    private String userId;
    private String userName;
    @Nullable
    private String userPhotoUrl;
    private float rating;
    private String comment;
    private String status = STATUS_ACTIVE;
    private Date createdAt;
    private Date updatedAt;

    public Review() {
        // Required for Firestore
    }

    public Review(String userId, String userName, @Nullable String userPhotoUrl, float rating, String comment) {
        this.userId = userId;
        this.userName = userName;
        this.userPhotoUrl = userPhotoUrl;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    @Nullable
    public String getUserPhotoUrl() { return userPhotoUrl; }
    public void setUserPhotoUrl(@Nullable String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
