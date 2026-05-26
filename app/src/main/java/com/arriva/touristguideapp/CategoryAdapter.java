package com.arriva.touristguideapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
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
        holder.categoryName.setText(category.getName());
        holder.categoryIcon.setImageResource(category.getIconResId());
        
        // Highlight logic
        boolean isSelected = position == selectedPosition;
        
        if (isSelected) {
            holder.categoryName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.m3_on_primary_container));
            holder.categoryIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.m3_on_primary_container));
            ((com.google.android.material.card.MaterialCardView) holder.itemView).setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.m3_primary_container));
        } else {
            holder.categoryName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.m3_on_surface_variant));
            holder.categoryIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.m3_primary));
            ((com.google.android.material.card.MaterialCardView) holder.itemView).setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.m3_surface_variant));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            
            // Notify changes to reset previous and set current selection
            notifyItemChanged(previousSelected);
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
