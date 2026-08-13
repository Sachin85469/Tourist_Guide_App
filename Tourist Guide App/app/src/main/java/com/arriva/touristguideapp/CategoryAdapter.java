package com.arriva.touristguideapp;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = -1;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        
        // Premium Entrance Animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationX(30f);
        holder.itemView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(300)
            .setStartDelay(position * 50L)
            .start();

        holder.categoryName.setText(category.getName());
        holder.categoryIcon.setImageResource(category.getIconResId());
        holder.categoryIcon.clearColorFilter();
        
        // Highlight logic (purple border + scale)
        boolean isSelected = position == selectedPosition;
        float targetScale = isSelected ? 1.04f : 1f;

        MaterialCardView card = (MaterialCardView) holder.itemView;
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        if (isSelected) {
            card.setStrokeWidth((int) (2 * density));
            card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.premium_purple));
            card.setCardElevation(8 * density);
        } else {
            card.setStrokeWidth(0);
            card.setCardElevation(4 * density);
        }
        holder.itemView.animate().scaleX(targetScale).scaleY(targetScale).setDuration(180).start();
        holder.categoryName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_on_surface));

        holder.itemView.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(isSelected ? 1.02f : 0.97f).scaleY(isSelected ? 1.02f : 0.97f).setDuration(120).start();
                holder.categoryIcon.animate().scaleX(0.94f).scaleY(0.94f).setDuration(120).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(targetScale).scaleY(targetScale).setDuration(140).start();
                holder.categoryIcon.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }
            selectedPosition = currentPosition;
            
            // Notify changes to reset previous and set current selection
            if (previousSelected != RecyclerView.NO_POSITION && previousSelected >= 0) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onCategoryClick(category.getName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIcon;
        TextView categoryName;
        View container;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            categoryName = itemView.findViewById(R.id.categoryName);
            container = itemView.findViewById(R.id.categoryContainer);
        }
    }
}
