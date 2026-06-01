package com.arriva.touristguideapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TopPickAdapter extends RecyclerView.Adapter<TopPickAdapter.ViewHolder> {

    private List<Place> topPickList;
    private OnItemClickListener listener;

    public TopPickAdapter(List<Place> topPickList, OnItemClickListener listener) {
        this.topPickList = topPickList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image, favoriteIcon;
        public TextView name, rating, budget, distance, crowdStatus, bestSeason;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.topPickImage);
            name = view.findViewById(R.id.topPickName);
            rating = view.findViewById(R.id.topPickRating);
            budget = view.findViewById(R.id.topPickBudget);
            distance = view.findViewById(R.id.topPickDistance);
            crowdStatus = view.findViewById(R.id.tvCrowdStatus);
            bestSeason = view.findViewById(R.id.tvBestSeason);
            favoriteIcon = view.findViewById(R.id.btnFavorite);
        }

        public void bind(final Place place, final OnItemClickListener listener) {
            Context context = itemView.getContext();
            name.setText(place.getName());
            
            if (rating != null) {
                if (place.getTotalRatings() > 0) {
                    rating.setText(String.format(java.util.Locale.getDefault(), "%.1f", place.getRating()));
                } else {
                    rating.setText("New");
                }
            }

            if (budget != null) budget.setText(String.format("%s Budget", place.getBudget()));
            if (crowdStatus != null) crowdStatus.setText(String.format("%s Crowd", place.getCrowdLevel()));
            if (bestSeason != null) bestSeason.setText(place.getBestTime());
            
            PlaceImageHelper.loadThumbnail(image, place);

            if (distance != null) {
                if (place.getDistance() >= 0) {
                    distance.setText(String.format(java.util.Locale.getDefault(), "%.1f km", place.getDistance()));
                } else {
                    distance.setText("");
                }
            }

            if (favoriteIcon != null) {
                boolean isFav = FavoritesManager.isFavorite(context, place.getId());
                favoriteIcon.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

                favoriteIcon.setOnClickListener(v -> {
                    FavoritesManager.toggleFavorite(context, place.getId());
                    boolean updated = FavoritesManager.isFavorite(context, place.getId());
                    favoriteIcon.setImageResource(updated ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

                    favoriteIcon.setScaleX(0.7f);
                    favoriteIcon.setScaleY(0.7f);
                    favoriteIcon.animate().scaleX(1f).scaleY(1f).setDuration(200);
                });
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(place);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_pick, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(topPickList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return topPickList.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        PlaceImageHelper.clear(holder.image);
        super.onViewRecycled(holder);
    }
}
