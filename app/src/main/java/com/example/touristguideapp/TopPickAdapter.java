package com.example.touristguideapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TopPickAdapter extends RecyclerView.Adapter<TopPickAdapter.ViewHolder> {

    private List<Place> topPickList;
    private OnItemClickListener listener;

    public TopPickAdapter(List<Place> topPickList, OnItemClickListener listener) {
        this.topPickList = topPickList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView name, rating, budget;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.topPickImage);
            name = view.findViewById(R.id.topPickName);
            rating = view.findViewById(R.id.topPickRating);
            budget = view.findViewById(R.id.topPickBudget);
        }

        public void bind(final Place place, final OnItemClickListener listener) {
            name.setText(place.getName());
            rating.setText(place.getRating() + " ⭐");
            budget.setText(place.getBudget());
            image.setImageResource(place.getImageResId());
            
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(place);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_pick, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(topPickList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return topPickList.size();
    }
}
