package com.arriva.touristguideapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    @NonNull
    private final List<String> imageRefs;

    public GalleryAdapter(@Nullable List<String> imageRefs) {
        this.imageRefs = copyImageRefs(imageRefs);
    }

    @NonNull
    private static List<String> copyImageRefs(@Nullable List<String> in) {
        List<String> out = new ArrayList<>();
        if (in == null) {
            return out;
        }
        for (String ref : in) {
            if (ref != null) {
                // Keep an empty first reference so a missing cover still renders the default travel image.
                out.add(ref.trim());
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
        ImageUtils.loadImageReference(holder.imageView, imageRefs.get(position));
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        ImageUtils.clear(holder.imageView);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return imageRefs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivGalleryImage);
        }
    }
}
