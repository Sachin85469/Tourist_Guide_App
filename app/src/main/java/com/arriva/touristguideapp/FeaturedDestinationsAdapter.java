package com.arriva.touristguideapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class FeaturedDestinationsAdapter extends RecyclerView.Adapter<FeaturedDestinationsAdapter.ViewHolder> {

    private final List<Place> featuredPlaces;
    private final OnItemClickListener listener;

    public FeaturedDestinationsAdapter(List<Place> featuredPlaces, OnItemClickListener listener) {
        this.featuredPlaces = featuredPlaces;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_destination, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Place place = featuredPlaces.get(position);
        holder.tvTitle.setText(place.getName());
        holder.tvRating.setText(String.format(Locale.getDefault(), "⭐ %.1f", place.getRating()));
        holder.tvTag.setText(place.getTag() != null ? place.getTag() : "Popular");
        
        PlaceImageHelper.loadThumbnail(holder.ivFeatured, place);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(place);
        });
    }

    @Override
    public int getItemCount() {
        return featuredPlaces.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFeatured;
        TextView tvTitle, tvRating, tvTag;

        ViewHolder(View itemView) {
            super(itemView);
            ivFeatured = itemView.findViewById(R.id.ivFeatured);
            tvTitle = itemView.findViewById(R.id.tvFeaturedTitle);
            tvRating = itemView.findViewById(R.id.tvFeaturedRating);
            tvTag = itemView.findViewById(R.id.tvFeaturedTag);
        }
    }
}
