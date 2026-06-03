package com.arriva.touristguideapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
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
        setHasStableIds(true);
    }

    public void updateList(List<Place> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return favoriteList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return favoriteList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return favoriteList.get(oldItemPosition).equals(newList.get(newItemPosition));
            }
        });
        this.favoriteList.clear();
        this.favoriteList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        return favoriteList.get(position).getId().hashCode();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image, favoriteIcon;
        public TextView name, rating, location, category;
        public View btnPlanTrip, btnShare;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.favImage);
            name = view.findViewById(R.id.favName);
            rating = view.findViewById(R.id.favRating);
            favoriteIcon = view.findViewById(R.id.favIcon);
            location = view.findViewById(R.id.favLocation);
            category = view.findViewById(R.id.favCategory);
            btnPlanTrip = view.findViewById(R.id.btnPlanTrip);
            btnShare = view.findViewById(R.id.btnShare);
        }

        public void bind(final Place place, final OnItemClickListener listener) {
            Context context = itemView.getContext();
            name.setText(place.getName());
            
            if (location != null) location.setText(place.getCity());
            if (category != null) category.setText(place.getCategory() != null ? place.getCategory().toUpperCase() : "");

            // Fix: REAL FIRESTORE RATINGS (Requirement 9)
            if (rating != null) {
                if (place.getTotalRatings() > 0) {
                    rating.setText(String.format(java.util.Locale.getDefault(), "%.1f", place.getRating()));
                    rating.setVisibility(View.VISIBLE);
                } else {
                    rating.setText("New");
                    rating.setVisibility(View.VISIBLE);
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
                FavoritesManager.toggleFavorite(context, place);
                boolean updated = FavoritesManager.isFavorite(context, place.getId());
                favoriteIcon.setImageResource(updated ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

                // notify activity to refresh if needed (e.g. update count)
                if (context instanceof FavoritesActivity) {
                    ((FavoritesActivity) context).updateCount();
                }

                // animation
                favoriteIcon.setScaleX(0.7f);
                favoriteIcon.setScaleY(0.7f);

                favoriteIcon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200);
            });

            if (btnPlanTrip != null) {
                btnPlanTrip.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(context, PlanTripActivity.class);
                    context.startActivity(intent);
                });
            }

            if (btnShare != null) {
                btnShare.setOnClickListener(v -> {
                    String shareText = "Check out this place in " + place.getCity() + ": " + place.getName();
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
                    context.startActivity(android.content.Intent.createChooser(intent, "Share via"));
                });
            }
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
