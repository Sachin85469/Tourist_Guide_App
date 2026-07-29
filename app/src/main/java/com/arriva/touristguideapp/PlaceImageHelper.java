package com.arriva.touristguideapp;

import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.arriva.touristguideapp.utils.ImageUtils;

public final class PlaceImageHelper {

    private PlaceImageHelper() {
    }

    public static void clear(@NonNull ImageView imageView) {
        ImageUtils.clear(imageView);
    }

    public static void loadThumbnail(@NonNull ImageView imageView, @NonNull Place place) {
        ImageUtils.loadPlaceMainImage(imageView, place);
    }

}
