package com.example.touristguideapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CATEGORIES = 0;
    private static final int TYPE_TOP_PICKS = 1;
    private static final int TYPE_PLAN_TRIP = 2;
    private static final int TYPE_SECTION_HEADER = 3;
    private static final int TYPE_PLACE = 4;

    private List<HomeSection> sections;
    private List<Category> categories;
    private List<Place> topPicks;
    private OnItemClickListener placeClickListener;
    private CategoryAdapter.OnCategoryClickListener categoryClickListener;
    private View.OnClickListener planTripClickListener;
    private int lastPosition = -1;

    public HomeAdapter(List<HomeSection> sections, 
                       List<Category> categories, 
                       List<Place> topPicks,
                       OnItemClickListener placeClickListener,
                       CategoryAdapter.OnCategoryClickListener categoryClickListener,
                       View.OnClickListener planTripClickListener) {
        this.sections = sections;
        this.categories = categories;
        this.topPicks = topPicks;
        this.placeClickListener = placeClickListener;
        this.categoryClickListener = categoryClickListener;
        this.planTripClickListener = planTripClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        String type = sections.get(position).getType();
        switch (type) {
            case HomeSection.TYPE_CATEGORIES: return TYPE_CATEGORIES;
            case HomeSection.TYPE_TOP_PICKS: return TYPE_TOP_PICKS;
            case HomeSection.TYPE_PLAN_TRIP: return TYPE_PLAN_TRIP;
            case HomeSection.TYPE_ALL_PLACES_HEADER: return TYPE_SECTION_HEADER;
            case HomeSection.TYPE_PLACE: return TYPE_PLACE;
            default: return -1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_CATEGORIES:
                return new CategoriesViewHolder(inflater.inflate(R.layout.layout_home_categories, parent, false));
            case TYPE_TOP_PICKS:
                return new HorizontalViewHolder(inflater.inflate(R.layout.layout_home_horizontal_section, parent, false));
            case TYPE_PLAN_TRIP:
                return new PlanTripViewHolder(inflater.inflate(R.layout.layout_home_plan_trip, parent, false));
            case TYPE_SECTION_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.layout_home_section_header, parent, false));
            case TYPE_PLACE:
                return new PlaceViewHolder(inflater.inflate(R.layout.item_place_v2, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HomeSection section = sections.get(position);
        
        setAnimation(holder.itemView, position);

        if (holder instanceof CategoriesViewHolder) {
            ((CategoriesViewHolder) holder).bind(categories, categoryClickListener);
        } else if (holder instanceof HorizontalViewHolder) {
            ((HorizontalViewHolder) holder).bind(section.getTitle(), topPicks, placeClickListener);
        } else if (holder instanceof PlanTripViewHolder) {
            ((PlanTripViewHolder) holder).bind(planTripClickListener);
        } else if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(section.getTitle());
        } else if (holder instanceof PlaceViewHolder) {
            ((PlaceViewHolder) holder).bind(section.getSinglePlace(), position, placeClickListener);
        }
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
        return sections.size();
    }

    static class CategoriesViewHolder extends RecyclerView.ViewHolder {
        RecyclerView rvCategories;
        CategoriesViewHolder(View itemView) {
            super(itemView);
            rvCategories = itemView.findViewById(R.id.rvCategories);
        }
        void bind(List<Category> categories, CategoryAdapter.OnCategoryClickListener listener) {
            rvCategories.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            rvCategories.setAdapter(new CategoryAdapter(categories, listener));
        }
    }

    static class HorizontalViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        RecyclerView rvHorizontal;
        HorizontalViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSectionTitle);
            rvHorizontal = itemView.findViewById(R.id.rvHorizontal);
        }
        void bind(String title, List<Place> data, OnItemClickListener listener) {
            tvTitle.setText(title);
            rvHorizontal.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            rvHorizontal.setAdapter(new TopPickAdapter(data, listener));
        }
    }

    static class PlanTripViewHolder extends RecyclerView.ViewHolder {
        View btnPlanNow;
        PlanTripViewHolder(View itemView) {
            super(itemView);
            btnPlanNow = itemView.findViewById(R.id.btnPlanNow);
        }
        void bind(View.OnClickListener listener) {
            btnPlanNow.setOnClickListener(listener);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHeaderTitle);
        }
        void bind(String title) {
            if (tvTitle != null) tvTitle.setText(title);
        }
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView placeName, placeCategory, placeCity, placeRating;
        ImageView placeImage, btnFavorite;

        PlaceViewHolder(View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.tvPlaceName);
            placeCategory = itemView.findViewById(R.id.tvPlaceCategory);
            placeCity = itemView.findViewById(R.id.tvCity);
            placeRating = itemView.findViewById(R.id.tvRating);
            placeImage = itemView.findViewById(R.id.ivPlaceImage);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        void bind(Place place, int position, OnItemClickListener listener) {
            if (place == null) return;
            Context context = itemView.getContext();
            placeName.setText(place.getName());
            placeCategory.setText(place.getCategory());
            if (placeCity != null) placeCity.setText(place.getCity());
            if (placeRating != null) placeRating.setText(String.format(Locale.getDefault(), "%.1f ⭐", place.getRating()));
            placeImage.setImageResource(place.getImageResId());
            
            updateFavoriteIcon(context, place.getId());

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(place);
            });

            btnFavorite.setOnClickListener(v -> {
                FavoritesManager.toggleFavorite(context, place.getId());
                
                // Update icon immediately
                updateFavoriteIcon(context, place.getId());

                // Animation (bounce effect)
                btnFavorite.setScaleX(0.7f);
                btnFavorite.setScaleY(0.7f);

                btnFavorite.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
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
