package com.arriva.touristguideapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dual-mode gallery adapter that supports:
 * <ul>
 *   <li>Legacy local drawable resource IDs ({@code List<Integer>})</li>
 *   <li>Remote Firestore image URLs ({@code List<String>})</li>
 * </ul>
 * When remote URLs are available and non-empty, they take priority.
 * Otherwise the adapter falls back to the drawable list.
 * A system placeholder is shown while a URL is loading or if it fails.
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    /** Local drawable resource IDs (legacy). */
    @NonNull
    private final List<Integer> drawableImages;

    /** Remote gallery URLs from Firestore. */
    @NonNull
    private final List<String> imageUrls;

    /** True when we should render from {@link #imageUrls} rather than {@link #drawableImages}. */
    private final boolean useUrls;

    /**
     * Legacy constructor — drawable-only gallery (unchanged from original behavior).
     */
    public GalleryAdapter(@NonNull List<Integer> images) {
        this.drawableImages = images != null ? images : Collections.emptyList();
        this.imageUrls = Collections.emptyList();
        this.useUrls = false;
    }

    /**
     * Dual-mode constructor.
     *
     * @param drawableImages fallback local drawables (may be empty)
     * @param imageUrls      remote gallery URLs from Firestore (may be null or empty)
     */
    public GalleryAdapter(@NonNull List<Integer> drawableImages,
                          @Nullable List<String> imageUrls) {
        this.drawableImages = drawableImages != null ? drawableImages : Collections.emptyList();
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : Collections.emptyList();
        this.useUrls = !this.imageUrls.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (useUrls) {
            // Load remote URL via Glide with a system placeholder
            String url = imageUrls.get(position);
            Glide.with(holder.imageView.getContext())
                    .load(url)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            // Legacy drawable resource
            int resId = drawableImages.get(position);
            if (resId != 0) {
                holder.imageView.setImageResource(resId);
            } else {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }

    @Override
    public int getItemCount() {
        return useUrls ? imageUrls.size() : drawableImages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivGalleryImage);
        }
    }
}
