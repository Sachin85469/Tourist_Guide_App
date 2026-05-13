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
     * Loads the hero image for a place. Only uses remote URL.
     */
    public static void loadThumbnail(@NonNull ImageView imageView, @NonNull Place place) {
        String id = place.getId() != null ? place.getId() : "?";
        String title = place.getName() != null ? place.getName() : "?";

        String url = trimToNull(place.getImageUrl());

        if (url == null) {
            Log.d(TAG, "PLACEHOLDER_USED placeId=" + id + " reason=null_url");
            Glide.with(imageView)
                    .load(R.drawable.placeholder)
                    .into(imageView);
            return;
        }

        RequestOptions opts = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder);

        Glide.with(imageView)
                .load(url)
                .apply(opts)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "REMOTE_IMAGE_FAILED placeId=" + id + " url=" + url + " msg=" + (e != null ? e.getMessage() : "unknown"));
                        Log.d(TAG, "PLACEHOLDER_USED placeId=" + id + " reason=load_failed");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.i(TAG, "REMOTE_IMAGE_SUCCESS placeId=" + id + " source=" + dataSource);
                        return false;
                    }
                })
                .into(imageView);
    }

    @Nullable
    private static String trimToNull(@Nullable String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
