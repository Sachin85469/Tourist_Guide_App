package com.arriva.touristguideapp;

import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

/**
 * Loads a place thumbnail: HTTPS from Firestore when {@link Place#getImageUrl()} is set,
 * otherwise the local drawable from {@link Place#getImageResId()}.
 */
public final class PlaceImageHelper {

    private PlaceImageHelper() {
    }

    public static void loadThumbnail(@NonNull ImageView imageView, @NonNull Place place) {
        String url = place.getImageUrl();
        if (url != null && !url.trim().isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(url.trim())
                    .placeholder(place.getImageResId())
                    .error(place.getImageResId())
                    .centerCrop()
                    .into(imageView);
        } else {
            Glide.with(imageView.getContext()).clear(imageView);
            imageView.setImageResource(place.getImageResId());
        }
    }
}
