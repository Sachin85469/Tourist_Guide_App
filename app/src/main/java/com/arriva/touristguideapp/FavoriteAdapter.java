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

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Place place);
    }

    private List<Place> favoriteList;
    private OnItemClickListener listener;

    public FavoriteAdapter(List<Place> favoriteList, OnItemClickListener listener) {
        this.favoriteList = favoriteList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image, favoriteIcon;
        public TextView name, rating;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.favImage);
            name = view.findViewById(R.id.favName);
            rating = view.findViewById(R.id.favRating);
            favoriteIcon = view.findViewById(R.id.favIcon);
        }

        public void bind(final Place place, final OnItemClickListener listener) {
            Context context = itemView.getContext();
            name.setText(place.getName());
            
            // Fix: REAL FIRESTORE RATINGS (Requirement 9)
            if (rating != null) {
                if (place.getTotalRatings() > 0) {
                    rating.setText(String.format(java.util.Locale.getDefault(), "%.1f ⭐", place.getRating()));
                    rating.setVisibility(View.VISIBLE);
                    android.util.Log.d("FavoriteAdapter", "CARD_REAL_RATING: " + place.getName() + " -> " + place.getRating());
                    if (place.getRating() == 4.0) {
                        android.util.Log.v("FavoriteAdapter", "CARD_FAKE_RATING_DETECTED: potential static 4.0 for " + place.getName());
                    }
                } else {
                    // Requirement 3: Show "New" badge instead of 0.0 or fake rating
                    rating.setText("New");
                    rating.setVisibility(View.VISIBLE);
                    android.util.Log.d("FavoriteAdapter", "CARD_UNRATED: " + place.getName());
                }
            }
            
            // Exclusively remote URLs via PlaceImageHelper
            PlaceImageHelper.loadThumbnail(image, place);
            
            boolean isFav = FavoritesManager.isFavorite(context, place.getId());
            favoriteIcon.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(place);
                    }
                }
            });

            favoriteIcon.setOnClickListener(v -> {
                FavoritesManager.toggleFavorite(context, place.getId());
                boolean updated = FavoritesManager.isFavorite(context, place.getId());
                favoriteIcon.setImageResource(updated ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

                // animation
                favoriteIcon.setScaleX(0.7f);
                favoriteIcon.setScaleY(0.7f);

                favoriteIcon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200);
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(favoriteList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        PlaceImageHelper.clear(holder.image);
        super.onViewRecycled(holder);
    }
}
