package com.example.touristguideapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    private List<Place> favoriteList;
    private OnItemClickListener listener;

    public FavoriteAdapter(List<Place> favoriteList, OnItemClickListener listener) {
        this.favoriteList = favoriteList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView name;

        public ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.favImage);
            name = view.findViewById(R.id.favName);
        }

        public void bind(final Place place, final OnItemClickListener listener) {
            name.setText(place.getName());
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
