package com.arriva.touristguideapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

/**
 * Unified thumbnail loading for {@link Place}: remote URL first (Firestore / direct HTTPS),
 * then bundled drawable via {@link Place#getDrawableAssetKey()}, then {@link Place#getImageResId()}.
 * Always uses Glide.
 */
public final class PlaceImageHelper {

    private static final String TAG = "PlaceImageHelper";

    private PlaceImageHelper() {
    }

    public static void clear(@NonNull ImageView imageView) {
        Glide.with(imageView).clear(imageView);
    }

    public static void loadThumbnail(@NonNull ImageView imageView, @NonNull Place place) {
        Context ctx = imageView.getContext();
        String id = place.getId() != null ? place.getId() : "?";
        String title = place.getName() != null ? place.getName() : "?";

        String url = trimToNull(place.getImageUrl());
        int resolvedKeyRes = resolveDrawableResId(ctx, place.getDrawableAssetKey());
        int resId = place.getImageResId();

        if (url != null) {
            Log.d(TAG, "thumbnail placeId=" + id + " name=" + title
                    + " sourceType=REMOTE_URL"
                    + " remoteUrlUsed=true"
                    + " remoteUrlHost=" + hostOnly(url)
                    + " drawableKeyResolved=" + (resolvedKeyRes != 0)
                    + " drawableAssetKey=" + safeKey(place.getDrawableAssetKey()));
            RequestOptions opts = new RequestOptions()
                    .centerCrop()
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder);
            Glide.with(imageView)
                    .load(url)
                    .apply(opts)
                    .into(imageView);
            return;
        }

        int loadRes;
        String sourceType;
        if (resolvedKeyRes != 0) {
            loadRes = resolvedKeyRes;
            sourceType = "DRAWABLE_ASSET_KEY";
        } else if (resId != 0) {
            loadRes = resId;
            sourceType = "LOCAL_RES_ID";
        } else {
            loadRes = R.drawable.placeholder;
            sourceType = "GENERIC_PLACEHOLDER";
        }

        Log.d(TAG, "thumbnail placeId=" + id + " name=" + title
                + " sourceType=" + sourceType
                + " remoteUrlUsed=false"
                + " drawableKeyResolved=" + (resolvedKeyRes != 0)
                + " drawableAssetKey=" + safeKey(place.getDrawableAssetKey())
                + " localResIdUsed=" + (resId != 0 && loadRes == resId));

        RequestOptions opts = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder);

        Glide.with(imageView)
                .load(loadRes)
                .apply(opts)
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

    @NonNull
    private static String safeKey(@Nullable String k) {
        return k == null ? "null" : k;
    }

    @NonNull
    private static String hostOnly(@NonNull String url) {
        try {
            Uri u = Uri.parse(url);
            String h = u.getHost();
            return h != null ? h : "nohost";
        } catch (Exception e) {
            return "bad-uri";
        }
    }

    private static int resolveDrawableResId(@NonNull Context ctx, @Nullable String assetKey) {
        if (assetKey == null) {
            return 0;
        }
        String key = assetKey.trim();
        if (key.isEmpty()) {
            return 0;
        }
        int rid = ctx.getResources().getIdentifier(key, "drawable", ctx.getPackageName());
        return rid == 0 ? 0 : rid;
    }
}
