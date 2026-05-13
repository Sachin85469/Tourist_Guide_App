package com.arriva.touristguideapp.data.reviews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.Review;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    public interface OnReviewDeleteListener {
        void onDeleteClick(Review review);
    }

    private List<Review> reviews = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private OnReviewDeleteListener deleteListener;
    private final String currentUserId;

    public ReviewAdapter() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    public void setReviews(List<Review> newReviews) {
        this.reviews = new ArrayList<>(newReviews);
        notifyDataSetChanged();
    }

    public void setOnReviewDeleteListener(OnReviewDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserPhoto, btnDelete;
        TextView tvUserName, tvDate, tvComment;
        RatingBar rbRating;

        ViewHolder(View itemView) {
            super(itemView);
            ivUserPhoto = itemView.findViewById(R.id.ivReviewUserPhoto);
            tvUserName = itemView.findViewById(R.id.tvReviewUserName);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            rbRating = itemView.findViewById(R.id.rbReviewStars);
            btnDelete = itemView.findViewById(R.id.btnDeleteReview);
        }

        void bind(Review review) {
            tvUserName.setText(review.getUserName());
            tvComment.setText(review.getComment());
            rbRating.setRating(review.getRating());
            
            if (review.getCreatedAt() != null) {
                tvDate.setText(dateFormat.format(review.getCreatedAt()));
            }

            if (review.getUserPhotoUrl() != null && !review.getUserPhotoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(review.getUserPhotoUrl())
                        .apply(new RequestOptions().placeholder(android.R.drawable.ic_menu_gallery).circleCrop())
                        .into(ivUserPhoto);
            } else {
                ivUserPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Only show delete button if this is the current user's review
            if (currentUserId != null && currentUserId.equals(review.getUserId())) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDeleteClick(review);
                    }
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }
        }
    }
}
