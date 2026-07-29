package com.arriva.touristguideapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.utils.ImageUtils;

import java.util.List;

public class GalleryPreviewAdapter extends RecyclerView.Adapter<GalleryPreviewAdapter.ViewHolder> {

    private final List<String> imageRefs;
    private final OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(int position);
    }

    public GalleryPreviewAdapter(List<String> imageRefs, OnImageClickListener listener) {
        this.imageRefs = imageRefs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageUtils.loadImageReference(holder.imageView, imageRefs.get(position));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageRefs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivGalleryThumb);
        }
    }
}
