package com.arriva.touristguideapp;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

/**
 * Unified thumbnail loading for {@link Place}: exclusively uses remote URLs from Firestore.
 * Always uses Glide with placeholder/error drawables.
 */
public final class PlaceImageHelper {

    private static final String TAG = "PlaceImageHelper";

    private PlaceImageHelper() {
    }

    public static void clear(@NonNull ImageView imageView) {
        Glide.with(imageView).clear(imageView);
    }

    /**
     * Loads the hero image for a place using a local hardcoded drawable.
     */
    public static void loadThumbnail(@NonNull ImageView imageView, @NonNull Place place) {
        String name = place.getName() != null ? place.getName() : "";
        int drawableResId = getLocalImageResource(name);

        RequestOptions opts = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder);

        Glide.with(imageView)
                .load(drawableResId)
                .apply(opts)
                .into(imageView);
    }

    /**
     * Maps a place name to a local drawable resource.
     * To add a new image, put the image file in app/src/main/res/drawable/
     * and add a new case in this switch statement!
     */
    public static int getLocalImageResource(@NonNull String placeName) {
        // We use a normalized string to avoid case/space matching issues
        String normalized = placeName.trim().toLowerCase();

        // Hardcode your drawables here:
        if (normalized.contains("vit pune")) {
            // return R.drawable.vit_pune; // example
        } else if (normalized.contains("aga khan")) {
            // return R.drawable.aga_khan_palace;
        } else if (normalized.contains("dagdusheth")) {
            // return R.drawable.dagdusheth;
        } else if (normalized.contains("iskcon")) {
            // return R.drawable.iskcon;
        } else if (normalized.contains("saras baug")) {
            // return R.drawable.saras_baug;
        } else if (normalized.contains("shaniwar")) {
            // return R.drawable.shaniwar_wada;
        } else if (normalized.contains("sinhagad")) {
            // return R.drawable.sinhagad;
        } else if (normalized.contains("zoo") || normalized.contains("rajiv gandhi")) {
            // return R.drawable.pune_zoo;
        }

        // Default placeholder if no hardcoded image is found
        return R.drawable.placeholder;
    }
}
