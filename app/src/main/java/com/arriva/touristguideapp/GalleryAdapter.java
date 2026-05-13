package com.arriva.touristguideapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gallery pager: remote URLs first, else drawable resource names ({@code galleryDrawableKeys}),
 * else legacy {@code int} drawable ids. Always uses Glide.
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private static final String TAG = "GalleryAdapter";

    private enum PrimarySource {
        REMOTE_URLS,
        DRAWABLE_KEYS,
        RESOURCE_IDS
    }

    @NonNull
    private final List<Integer> drawableImages;
    @NonNull
    private final List<String> imageUrls;
    @NonNull
    private final List<String> drawableKeys;
    @NonNull
    private final PrimarySource primary;

    public GalleryAdapter(@NonNull List<Integer> images) {
        this(images, null, null);
    }

    public GalleryAdapter(@NonNull List<Integer> drawableImages, @Nullable List<String> imageUrls) {
        this(drawableImages, imageUrls, null);
    }

    public GalleryAdapter(@NonNull List<Integer> drawableImages,
                          @Nullable List<String> imageUrls,
                          @Nullable List<String> galleryDrawableKeys) {
        this.drawableImages = drawableImages != null ? drawableImages : Collections.emptyList();
        this.imageUrls = copyNonEmptyUrls(imageUrls);
        this.drawableKeys = copyNonEmptyKeys(galleryDrawableKeys);

        if (!this.imageUrls.isEmpty()) {
            primary = PrimarySource.REMOTE_URLS;
        } else if (!this.drawableKeys.isEmpty()) {
            primary = PrimarySource.DRAWABLE_KEYS;
        } else {
            primary = PrimarySource.RESOURCE_IDS;
        }

        String primaryLoad = (primary == PrimarySource.REMOTE_URLS) ? "REMOTE_URL" : "DRAWABLE_FALLBACK";
        Log.d(TAG, "gallery init primaryLoad=" + primaryLoad
                + " remoteUrlCount=" + this.imageUrls.size()
                + " drawableKeyCount=" + this.drawableKeys.size()
                + " legacyResCount=" + this.drawableImages.size());
    }

    @NonNull
    private static List<String> copyNonEmptyUrls(@Nullable List<String> in) {
        if (in == null || in.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s != null && !s.trim().isEmpty()) {
                out.add(s.trim());
            }
        }
        return out;
    }

    @NonNull
    private static List<String> copyNonEmptyKeys(@Nullable List<String> in) {
        if (in == null || in.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String s : in) {
            if (s != null && !s.trim().isEmpty()) {
                out.add(s.trim());
            }
        }
        return out;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context ctx = holder.imageView.getContext();
        RequestOptions opts = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder);

        final String loadSource = (primary == PrimarySource.REMOTE_URLS) ? "REMOTE_URL" : "DRAWABLE_FALLBACK";

        if (primary == PrimarySource.REMOTE_URLS) {
            String url = imageUrls.get(position);
            Log.d(TAG, "galleryBind position=" + position + " loadSource=" + loadSource + " urlHost=" + hostOnly(url));
            Glide.with(holder.imageView)
                    .load(url)
                    .apply(opts)
                    .into(holder.imageView);
            return;
        }
        if (primary == PrimarySource.DRAWABLE_KEYS) {
            String key = drawableKeys.get(position);
            int resId = ctx.getResources().getIdentifier(key, "drawable", ctx.getPackageName());
            Log.d(TAG, "galleryBind position=" + position + " loadSource=" + loadSource
                    + " drawableKey=" + key + " resolvedRes=" + (resId != 0));
            if (resId == 0) {
                Glide.with(holder.imageView)
                        .load(R.drawable.placeholder)
                        .apply(opts)
                        .into(holder.imageView);
            } else {
                Glide.with(holder.imageView)
                        .load(resId)
                        .apply(opts)
                        .into(holder.imageView);
            }
            return;
        }
        int resId = drawableImages.get(position);
        Log.d(TAG, "galleryBind position=" + position + " loadSource=" + loadSource + " resId=" + resId);
        if (resId != 0) {
            Glide.with(holder.imageView)
                    .load(resId)
                    .apply(opts)
                    .into(holder.imageView);
        } else {
            Glide.with(holder.imageView)
                    .load(R.drawable.placeholder)
                    .apply(opts)
                    .into(holder.imageView);
        }
    }

    @NonNull
    private static String hostOnly(@NonNull String url) {
        try {
            android.net.Uri u = android.net.Uri.parse(url);
            String h = u.getHost();
            return h != null ? h : "nohost";
        } catch (Exception e) {
            return "bad-uri";
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        Glide.with(holder.imageView).clear(holder.imageView);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        switch (primary) {
            case REMOTE_URLS:
                return imageUrls.size();
            case DRAWABLE_KEYS:
                return drawableKeys.size();
            default:
                return drawableImages.size();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivGalleryImage);
        }
    }
}
