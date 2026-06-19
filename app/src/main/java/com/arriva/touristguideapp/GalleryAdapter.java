package com.arriva.touristguideapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.data.repository.ImageRepository;
import com.arriva.touristguideapp.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    @NonNull
    private final List<Integer> imageResources;

    public GalleryAdapter(@Nullable List<Integer> imageResources) {
        this.imageResources = copyValidResources(imageResources);
    }

    @NonNull
    private static List<Integer> copyValidResources(@Nullable List<Integer> in) {
        List<Integer> out = new ArrayList<>();
        if (in == null) {
            return out;
        }
        for (Integer res : in) {
            if (res != null && res != 0) {
                out.add(res);
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
        @DrawableRes int res = imageResources.get(position);
        ImageUtils.loadDrawable(holder.imageView, res == 0 ? ImageRepository.PLACEHOLDER_IMAGE : res);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        ImageUtils.clear(holder.imageView);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return imageResources.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivGalleryImage);
        }
    }
}
