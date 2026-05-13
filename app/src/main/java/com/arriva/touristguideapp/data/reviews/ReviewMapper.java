package com.arriva.touristguideapp.data.reviews;

import com.arriva.touristguideapp.Review;
import java.util.ArrayList;
import java.util.List;

public final class ReviewMapper {

    private ReviewMapper() {}

    public static Review toReview(ReviewDto dto) {
        if (dto == null) return null;
        Review review = new Review();
        review.setUserId(dto.getUserId());
        review.setUserName(dto.getUserName());
        review.setUserPhotoUrl(dto.getUserPhotoUrl());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setCreatedAt(dto.getCreatedAt());
        review.setUpdatedAt(dto.getUpdatedAt());
        return review;
    }

    public static List<Review> toReviews(List<ReviewDto> dtos) {
        List<Review> reviews = new ArrayList<>();
        if (dtos == null) return reviews;
        for (ReviewDto dto : dtos) {
            Review r = toReview(dto);
            if (r != null) reviews.add(r);
        }
        return reviews;
    }
}
