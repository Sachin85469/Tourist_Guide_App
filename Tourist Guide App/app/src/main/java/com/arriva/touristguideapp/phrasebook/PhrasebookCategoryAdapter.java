package com.arriva.touristguideapp.phrasebook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arriva.touristguideapp.R;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class PhrasebookCategoryAdapter extends RecyclerView.Adapter<PhrasebookCategoryAdapter.CategoryViewHolder> {

    public interface OnCategorySelectedListener {
        void onCategorySelected(@NonNull String category);
    }

    private final List<String> categories = new ArrayList<>();
    private String selectedCategory;
    private final OnCategorySelectedListener listener;

    public PhrasebookCategoryAdapter(@NonNull OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    public void setCategories(@NonNull List<String> categories, @NonNull String selected) {
        this.categories.clear();
        this.categories.addAll(categories);
        this.selectedCategory = selected;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_phrase_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        holder.bind(category, category.equals(selectedCategory), v -> {
            if (!category.equals(selectedCategory)) {
                selectedCategory = category;
                notifyDataSetChanged();
                listener.onCategorySelected(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final Chip chip;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = (Chip) itemView;
        }

        void bind(@NonNull String label, boolean selected, @NonNull View.OnClickListener clickListener) {
            chip.setText(label);
            chip.setChecked(selected);
            chip.setOnClickListener(clickListener);
            int bg = selected ? R.color.primary : R.color.adaptive_surface;
            int text = selected ? R.color.white : R.color.text_primary;
            chip.setChipBackgroundColorResource(bg);
            chip.setTextColor(ContextCompat.getColor(chip.getContext(), text));
        }
    }
}
