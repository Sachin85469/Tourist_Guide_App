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
    private final OnItemClickListener listener;
    private int lastPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(Place place);
    }

    public PlaceAdapter(List<Place> placeList) {
        this(placeList, null);
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
        holder.bind(placeList.get(position), listener);
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

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        PlaceImageHelper.clear(holder.placeImage);
        super.onViewRecycled(holder);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView placeName;
        public final TextView placeCategory;
        public final TextView placeCity;
        public final TextView placeRating;
        public final TextView placeTag;
        public final TextView tvDistance;
        public final TextView txtBadge;
        public final ImageView placeImage;
        public final ImageView btnFavorite;

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

        public void bind(Place place, OnItemClickListener listener) {
            Context context = itemView.getContext();
            placeName.setText(place.getName());
            placeCategory.setText(place.getCategory());
            if (placeCity != null) {
                placeCity.setText(place.getCity());
            }
            if (placeRating != null) {
                if (place.getTotalRatings() > 0) {
                    placeRating.setText(String.format(
                            Locale.getDefault(),
                            "%.1f (%d)",
                            place.getRating(),
                            place.getTotalRatings()
                    ));
                } else {
                    placeRating.setText("New");
                }
                placeRating.setVisibility(View.VISIBLE);
            }
            if (placeTag != null) {
                placeTag.setText(place.getTag());
            }
            PlaceImageHelper.loadThumbnail(placeImage, place);

            if (txtBadge != null) {
                txtBadge.setVisibility(place.isTopPick() ? View.VISIBLE : View.GONE);
            }

            if (tvDistance != null) {
                tvDistance.setText(place.getDistance() >= 0
                        ? String.format(Locale.getDefault(), "%.1f km", place.getDistance())
                        : "Nearby");
            }

            updateFavoriteIcon(context, place.getId());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(place);
                } else {
                    android.content.Intent intent = new android.content.Intent(context, PlaceDetailsActivity.class);
                    PlaceIntentExtras.putPlaceDetails(intent, place);
                    context.startActivity(intent);
                    if (context instanceof Activity) {
                        ((Activity) context).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                }
            });

            btnFavorite.setOnClickListener(v -> {
                FavoritesManager.toggleFavorite(context, place);
                updateFavoriteIcon(context, place.getId());

                if (context instanceof MainActivity) {
                    ((MainActivity) context).refreshQuickStatsOnly();
                }

                v.animate()
                        .scaleX(1.3f)
                        .scaleY(1.3f)
                        .setDuration(150)
                        .setInterpolator(new OvershootInterpolator())
                        .withEndAction(() -> v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(150)
                                .start())
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
