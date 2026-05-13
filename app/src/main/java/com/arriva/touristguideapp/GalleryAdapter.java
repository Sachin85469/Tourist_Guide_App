package com.arriva.touristguideapp;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gallery pager: exclusively uses remote URLs from Firestore.
 * Always uses Glide.
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private static final String TAG = "GalleryAdapter";

    @NonNull
    private final List<String> imageUrls;

    public GalleryAdapter(@Nullable List<String> imageUrls) {
        this.imageUrls = copyNonEmptyUrls(imageUrls);
        Log.d(TAG, "gallery init remoteUrlCount=" + this.imageUrls.size());
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
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = imageUrls.get(position);
        
        RequestOptions opts = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder);

        Log.d(TAG, "galleryBind position=" + position + " urlHost=" + hostOnly(url));

        Glide.with(holder.imageView)
                .load(url)
                .apply(opts)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "REMOTE_IMAGE_FAILED galleryPos=" + position + " url=" + url + " msg=" + (e != null ? e.getMessage() : "unknown"));
                        Log.d(TAG, "PLACEHOLDER_USED galleryPos=" + position + " reason=load_failed");
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.i(TAG, "REMOTE_IMAGE_SUCCESS galleryPos=" + position + " source=" + dataSource);
                        return false;
                    }
                })
                .into(holder.imageView);
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
        return imageUrls.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivGalleryImage);
        }
    }
}
