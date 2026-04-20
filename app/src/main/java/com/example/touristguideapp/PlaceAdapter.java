package com.example.touristguideapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

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
                .inflate(R.layout.item_place, parent, false);
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
        public TextView txtName;
        public TextView txtCategory;
        public ImageView imgPlace;

        public ViewHolder(View view) {
            super(view);
            txtName = view.findViewById(R.id.txtName);
            txtCategory = view.findViewById(R.id.txtCategory);
            imgPlace = view.findViewById(R.id.imgPlace);
        }

        public void bind(Place place, int position, OnItemClickListener listener) {
            Context context = itemView.getContext();
            txtName.setText(place.getName());
            txtCategory.setText(place.getCategory());
            imgPlace.setImageResource(place.getImage());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(place);
                } else {
                    Intent intent = new Intent(context, MapActivity.class);
                    intent.putExtra("lat", place.getLat());
                    intent.putExtra("lng", place.getLng());
                    intent.putExtra("name", place.getName());
                    context.startActivity(intent);
                }
            });
        }
    }
}
