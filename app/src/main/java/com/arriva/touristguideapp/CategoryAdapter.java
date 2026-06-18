package com.arriva.touristguideapp;

import android.view.LayoutInflater;
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
        
        // Highlight logic
        boolean isSelected = position == selectedPosition;

        MaterialCardView card = (MaterialCardView) holder.itemView;
        if (isSelected) {
            float density = holder.itemView.getResources().getDisplayMetrics().density;
            card.setStrokeWidth((int) (2 * density));
            card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_primary));
        } else {
            card.setStrokeWidth(0);
        }
        holder.categoryName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

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
