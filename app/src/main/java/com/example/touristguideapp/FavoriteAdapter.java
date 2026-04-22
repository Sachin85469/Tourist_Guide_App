package com.example.touristguideapp;

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
        public TextView name;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.favImage);
            name = view.findViewById(R.id.favName);
            favoriteIcon = view.findViewById(R.id.favIcon);
        }

        public void bind(final Place place, final OnItemClickListener listener) {
            Context context = itemView.getContext();
            name.setText(place.getName());
            image.setImageResource(place.getImageResId());
            
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
}
