package com.arriva.touristguideapp;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    private List<Place> placeList;
    private OnItemClickListener listener;
    private int lastPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(Place place);
    }

    public PlaceAdapter(List<Place> placeList) {
        this.placeList = placeList;
    }

    public PlaceAdapter(List<Place> placeList, OnItemClickListener listener) {
        this.placeList = placeList;
        this.listener = listener;
    }

    public void updateList(List<Place> newList) {
        this.placeList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_v2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(placeList.get(position), position, listener);
        setAnimation(holder.itemView, position);
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), R.anim.fade_in);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView placeName;
        public TextView placeCategory;
        public TextView placeCity;
        public TextView placeRating;
        public TextView placeTag;
        public TextView tvDistance;
        public TextView txtBadge;
        public ImageView placeImage;
        public ImageView btnFavorite;

        public ViewHolder(View view) {
            super(view);
            placeName = view.findViewById(R.id.tvPlaceName);
            placeCategory = view.findViewById(R.id.tvPlaceCategory);
            placeCity = view.findViewById(R.id.tvCity);
            placeRating = view.findViewById(R.id.tvRating);
            placeTag = view.findViewById(R.id.tvTag);
            tvDistance = view.findViewById(R.id.tvDistance);
            txtBadge = view.findViewById(R.id.txtBadge);
            placeImage = view.findViewById(R.id.ivPlaceImage);
            btnFavorite = view.findViewById(R.id.btnFavorite);
        }

        public void bind(Place place, int position, OnItemClickListener listener) {
            Context context = itemView.getContext();
            placeName.setText(place.getName());
            placeCategory.setText(place.getCategory());
            if (placeCity != null) placeCity.setText(place.getCity());
            if (placeRating != null) placeRating.setText(String.format(Locale.getDefault(), "%.1f ⭐", place.getRating()));
            if (placeTag != null) placeTag.setText(place.getTag());
            placeImage.setImageResource(place.getImageResId());
            
            // Display Top Pick Badge
            if (txtBadge != null) {
                txtBadge.setVisibility(place.isTopPick() ? View.VISIBLE : View.GONE);
            }

            // Display distance
            if (tvDistance != null) {
                if (place.getDistance() >= 0) {
                    tvDistance.setText(String.format(Locale.getDefault(), "%.1f km away", place.getDistance()));
                } else {
                    tvDistance.setText("Distance unavailable");
                }
            }
            
            // Set icon based on favorite status
            updateFavoriteIcon(context, place.getId());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(place);
                } else {
                    android.content.Intent intent = new android.content.Intent(context, PlaceDetailsActivity.class);
                    intent.putExtra("id", place.getId());
                    intent.putExtra("name", place.getName());
                    intent.putExtra("description", place.getDescription());
                    intent.putExtra("category", place.getCategory());
                    intent.putExtra("budget", place.getBudget());
                    intent.putExtra("crowdLevel", place.getCrowdLevel());
                    intent.putExtra("bestTime", place.getBestTime());
                    intent.putExtra("imageResId", place.getImageResId());
                    intent.putExtra("tips", place.getTips());
                    intent.putExtra("funFact", place.getFunFact());
                    intent.putExtra("nearestStation", place.getNearestStation());
                    context.startActivity(intent);
                    if (context instanceof Activity) {
                        ((Activity) context).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                }
            });

            btnFavorite.setOnClickListener(v -> {
                // Toggle favorite state
                FavoritesManager.toggleFavorite(context, place.getId());
                
                // Immediately update UI
                updateFavoriteIcon(context, place.getId());

                // Smooth heart animation
                v.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(150)
                    .setInterpolator(new OvershootInterpolator())
                    .withEndAction(() -> {
                        v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start();
                    })
                    .start();
            });
        }

        private void updateFavoriteIcon(Context context, String placeId) {
            if (FavoritesManager.isFavorite(context, placeId)) {
                btnFavorite.setImageResource(R.drawable.ic_favorite);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
        }
    }
}
