package com.arriva.touristguideapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SECTION_HEADER = 3;
    private static final int TYPE_PLACE = 4;
    private static final int TYPE_WELCOME = 5;
    private static final int TYPE_FEATURED_CAROUSEL = 10;

    private List<HomeSection> sections;
    private List<Category> categories;
    private List<Place> topPicks;
    private OnItemClickListener placeClickListener;
    private CategoryAdapter.OnCategoryClickListener categoryClickListener;
    private View.OnClickListener planTripClickListener;
    private View.OnClickListener phrasebookClickListener;
    private int lastPosition = -1;

    public HomeAdapter(List<HomeSection> sections, 
                       List<Category> categories, 
                       List<Place> topPicks,
                       OnItemClickListener placeClickListener,
                       CategoryAdapter.OnCategoryClickListener categoryClickListener,
                       View.OnClickListener planTripClickListener,
                       View.OnClickListener phrasebookClickListener) {
        this.sections = sections;
        this.categories = categories;
        this.topPicks = topPicks;
        this.placeClickListener = placeClickListener;
        this.categoryClickListener = categoryClickListener;
        this.planTripClickListener = planTripClickListener;
        this.phrasebookClickListener = phrasebookClickListener;
        setHasStableIds(true);
    }

    public void updateSections(List<HomeSection> newSections) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return sections.size();
            }

            @Override
            public int getNewListSize() {
                return newSections.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                HomeSection oldS = sections.get(oldItemPosition);
                HomeSection newS = newSections.get(newItemPosition);
                if (!oldS.getType().equals(newS.getType())) return false;
                
                if (HomeSection.TYPE_PLACE.equals(oldS.getType())) {
                    return oldS.getSinglePlace().getId().equals(newS.getSinglePlace().getId());
                }
                return true; // Other sections are singleton types
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return sections.get(oldItemPosition).equals(newSections.get(newItemPosition));
            }
        });
        this.sections.clear();
        this.sections.addAll(newSections);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        HomeSection s = sections.get(position);
        if (HomeSection.TYPE_PLACE.equals(s.getType())) {
            return s.getSinglePlace().getId().hashCode();
        }
        return s.getType().hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        String type = sections.get(position).getType();
        switch (type) {
            case HomeSection.TYPE_WELCOME: return TYPE_WELCOME;
            case HomeSection.TYPE_ALL_PLACES_HEADER: return TYPE_SECTION_HEADER;
            case HomeSection.TYPE_PLACE: return TYPE_PLACE;
            case HomeSection.TYPE_FEATURED_CAROUSEL: return TYPE_FEATURED_CAROUSEL;
            default: return -1;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_WELCOME:
                return new WelcomeViewHolder(inflater.inflate(R.layout.item_welcome_stats, parent, false), planTripClickListener, phrasebookClickListener);
            case TYPE_SECTION_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.layout_home_section_header, parent, false));
            case TYPE_PLACE:
                return new PlaceViewHolder(inflater.inflate(R.layout.item_place_v2, parent, false));
            case TYPE_FEATURED_CAROUSEL:
                return new FeaturedViewHolder(inflater.inflate(R.layout.item_featured_carousel, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HomeSection section = sections.get(position);
        
        if (holder instanceof WelcomeViewHolder) {
            ((WelcomeViewHolder) holder).bind();
        } else if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(section.getTitle());
        } else if (holder instanceof PlaceViewHolder) {
            ((PlaceViewHolder) holder).bind(section.getSinglePlace(), position, placeClickListener);
        } else if (holder instanceof FeaturedViewHolder) {
            ((FeaturedViewHolder) holder).bind(section.getSinglePlace(), placeClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof PlaceViewHolder) {
            PlaceImageHelper.clear(((PlaceViewHolder) holder).placeImage);
        }
        super.onViewRecycled(holder);
    }

    static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFeatured, btnFavorite;
        TextView tvTitle, tvSubtitle, tvTag, tvRating, tvDistance;
        FeaturedViewHolder(View itemView) {
            super(itemView);
            ivFeatured = itemView.findViewById(R.id.ivFeatured);
            tvTitle = itemView.findViewById(R.id.tvFeaturedTitle);
            tvSubtitle = itemView.findViewById(R.id.tvFeaturedCategory);
            tvTag = itemView.findViewById(R.id.tvFeaturedTag);
            tvRating = itemView.findViewById(R.id.tvFeaturedRating);
            tvDistance = itemView.findViewById(R.id.tvFeaturedDistance);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
        void bind(Place place, OnItemClickListener listener) {
            if (place == null) return;
            Context context = itemView.getContext();
            tvTitle.setText(place.getName());
            tvSubtitle.setText(place.getCategory());
            if (tvTag != null) tvTag.setText(place.getTag());
            
            if (tvRating != null) {
                if (place.getTotalRatings() > 0) {
                    tvRating.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));
                } else {
                    tvRating.setText("New");
                }
            }

            if (tvDistance != null) {
                if (place.getDistance() >= 0) {
                    tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", place.getDistance()));
                    tvDistance.setVisibility(View.VISIBLE);
                } else {
                    tvDistance.setVisibility(View.GONE);
                }
            }

            PlaceImageHelper.loadThumbnail(ivFeatured, place);
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(place);
            });

            if (btnFavorite != null) {
                boolean isFav = FavoritesManager.isFavorite(context, place.getId());
                btnFavorite.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                btnFavorite.setOnClickListener(v -> {
                    FavoritesManager.toggleFavorite(context, place);
                    boolean updated = FavoritesManager.isFavorite(context, place.getId());
                    btnFavorite.setImageResource(updated ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                    
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).refreshQuickStatsOnly();
                    }

                    // Animation
                    btnFavorite.setScaleX(0.7f);
                    btnFavorite.setScaleY(0.7f);
                    btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(200);
                });
            }
        }
    }

    static class WelcomeViewHolder extends RecyclerView.ViewHolder {
        private final OnClickListener plannerListener;
        private final OnClickListener translatorListener;

        WelcomeViewHolder(View itemView, OnClickListener plannerListener, OnClickListener translatorListener) {
            super(itemView);
            this.plannerListener = plannerListener;
            this.translatorListener = translatorListener;
        }

        void bind() {
            View btnSOS = itemView.findViewById(R.id.btnLargeSOS);
            if (btnSOS != null) {
                btnSOS.setOnClickListener(v -> {
                    com.arriva.touristguideapp.sos.manager.SOSManager.getInstance(itemView.getContext())
                        .startSOSFlow(itemView.getContext(), "Home Screen");
                });
            }

            View btnPlanner = itemView.findViewById(R.id.btnPlanner);
            if (btnPlanner != null) {
                btnPlanner.setOnClickListener(plannerListener);
            }

            View btnTranslate = itemView.findViewById(R.id.btnTranslate);
            if (btnTranslate != null) {
                btnTranslate.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), com.arriva.touristguideapp.communication.CommunicationHubActivity.class);
                    intent.putExtra(com.arriva.touristguideapp.communication.CommunicationHubActivity.EXTRA_INITIAL_TAB, 1);
                    itemView.getContext().startActivity(intent);
                });
            }

            View btnNearby = itemView.findViewById(R.id.btnNearby);
            if (btnNearby != null) {
                btnNearby.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), com.arriva.touristguideapp.MapActivity.class);
                    itemView.getContext().startActivity(intent);
                });
            }

            View btnAi = itemView.findViewById(R.id.btnAiAssistant);
            if (btnAi != null) {
                btnAi.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), com.arriva.touristguideapp.AiChatActivity.class);
                    itemView.getContext().startActivity(intent);
                });
            }

            View btnPhrasebook = itemView.findViewById(R.id.btnPhrasebook);
            if (btnPhrasebook != null) {
                btnPhrasebook.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), com.arriva.touristguideapp.PhrasebookActivity.class);
                    itemView.getContext().startActivity(intent);
                });
            }
        }
    }


    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
        void bind(String title) {
            if (tvTitle != null) tvTitle.setText(title);
        }
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView placeName, placeCategory, placeCity, placeRating, placeTag, placeDistance;
        ImageView placeImage, btnFavorite;

        PlaceViewHolder(View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.tvPlaceName);
            placeCategory = itemView.findViewById(R.id.tvPlaceCategory);
            placeCity = itemView.findViewById(R.id.tvCity);
            placeRating = itemView.findViewById(R.id.tvRating);
            placeTag = itemView.findViewById(R.id.tvTag);
            placeDistance = itemView.findViewById(R.id.tvDistance);
            placeImage = itemView.findViewById(R.id.ivPlaceImage);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        void bind(Place place, int position, OnItemClickListener listener) {
            if (place == null) return;
            Context context = itemView.getContext();
            placeName.setText(place.getName());
            placeCategory.setText(place.getCategory());
            if (placeCity != null) placeCity.setText(place.getCity());
            if (placeRating != null) {
                if (place.getTotalRatings() > 0) {
                    placeRating.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));
                    placeRating.setVisibility(View.VISIBLE);
                    android.util.Log.d("HomeAdapter", "CARD_REAL_RATING: " + place.getName() + " -> " + place.getRating());
                    if (place.getRating() == 4.0) {
                        android.util.Log.v("HomeAdapter", "CARD_FAKE_RATING_DETECTED: potential static 4.0 for " + place.getName());
                    }
                } else {
                    placeRating.setText("New");
                    placeRating.setVisibility(View.VISIBLE);
                    android.util.Log.d("HomeAdapter", "CARD_UNRATED: " + place.getName());
                }
            }
            if (placeTag != null) placeTag.setText(place.getTag());
            
            if (placeDistance != null) {
                if (place.getDistance() >= 0) {
                    placeDistance.setText(String.format(Locale.getDefault(), "%.1f km away", place.getDistance()));
                    placeDistance.setVisibility(View.VISIBLE);
                } else {
                    placeDistance.setVisibility(View.GONE);
                }
            }

            PlaceImageHelper.loadThumbnail(placeImage, place);
            
            boolean isFav = FavoritesManager.isFavorite(context, place.getId());
            btnFavorite.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(place);
                } else {
                    Intent intent = new Intent(context, PlaceDetailsActivity.class);
                    PlaceIntentExtras.putPlaceDetails(intent, place);
                    context.startActivity(intent);
                }
            });

            btnFavorite.setOnClickListener(v -> {
                FavoritesManager.toggleFavorite(context, place);
                boolean updated = FavoritesManager.isFavorite(context, place.getId());
                btnFavorite.setImageResource(updated ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

                if (context instanceof MainActivity) {
                    ((MainActivity) context).refreshQuickStatsOnly();
                }

                // Animation
                btnFavorite.setScaleX(0.7f);
                btnFavorite.setScaleY(0.7f);

                btnFavorite.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200);
            });
        }
    }
}
