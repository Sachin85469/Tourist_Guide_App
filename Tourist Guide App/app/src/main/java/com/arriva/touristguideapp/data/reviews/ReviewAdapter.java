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

    public interface OnReviewReportListener {
        void onReportClick(Review review);
    }

    public interface OnReviewEditListener {
        void onEditClick(Review review);
    }

    private List<Review> reviews = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private OnReviewDeleteListener deleteListener;
    private OnReviewReportListener reportListener;
    private OnReviewEditListener editListener;
    private final String currentUserId;

    public ReviewAdapter() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    public void setReviews(List<Review> newReviews) {
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return reviews.size();
            }

            @Override
            public int getNewListSize() {
                return newReviews.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return reviews.get(oldItemPosition).getUserId().equals(newReviews.get(newItemPosition).getUserId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Review oldR = reviews.get(oldItemPosition);
                Review newR = newReviews.get(newItemPosition);
                return oldR.getRating() == newR.getRating() &&
                        java.util.Objects.equals(oldR.getComment(), newR.getComment()) &&
                        java.util.Objects.equals(oldR.getUserName(), newR.getUserName()) &&
                        java.util.Objects.equals(oldR.getUserPhotoUrl(), newR.getUserPhotoUrl()) &&
                        java.util.Objects.equals(oldR.getStatus(), newR.getStatus());
            }
        });

        this.reviews = new ArrayList<>(newReviews);
        diffResult.dispatchUpdatesTo(this);
    }

    public void setOnReviewDeleteListener(OnReviewDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnReviewReportListener(OnReviewReportListener listener) {
        this.reportListener = listener;
    }

    public void setOnReviewEditListener(OnReviewEditListener listener) {
        this.editListener = listener;
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
        ImageView ivUserPhoto, btnDelete, btnReport, btnEdit;
        TextView tvUserName, tvDate, tvComment, tvInitial;
        RatingBar rbRating;

        ViewHolder(View itemView) {
            super(itemView);
            ivUserPhoto = itemView.findViewById(R.id.ivReviewUserPhoto);
            tvInitial = itemView.findViewById(R.id.tvReviewInitial);
            tvUserName = itemView.findViewById(R.id.tvReviewUserName);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            rbRating = itemView.findViewById(R.id.rbReviewStars);
            btnEdit = itemView.findViewById(R.id.btnEditReview);
            btnDelete = itemView.findViewById(R.id.btnDeleteReview);
            btnReport = itemView.findViewById(R.id.btnReportReview);
        }

        void bind(Review review) {
            String userName = review.getUserName();
            if (userName == null || userName.trim().isEmpty()) {
                userName = "Traveler";
            }

            tvUserName.setText(userName);
            tvComment.setText(review.getComment());
            rbRating.setRating(review.getRating());
            tvInitial.setText(userName.substring(0, 1).toUpperCase(Locale.getDefault()));
            
            if (review.getCreatedAt() != null) {
                tvDate.setText(dateFormat.format(review.getCreatedAt()));
            }

            if (review.getUserPhotoUrl() != null && !review.getUserPhotoUrl().isEmpty()) {
                tvInitial.setVisibility(View.GONE);
                ivUserPhoto.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(review.getUserPhotoUrl())
                        .apply(new RequestOptions()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .circleCrop()
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL))
                        .into(ivUserPhoto);
            } else {
                Glide.with(itemView.getContext()).clear(ivUserPhoto);
                ivUserPhoto.setVisibility(View.GONE);
                tvInitial.setVisibility(View.VISIBLE);
            }

            // Only show delete button if this is the current user's review
            if (currentUserId != null && currentUserId.equals(review.getUserId())) {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    if (editListener != null) {
                        editListener.onEditClick(review);
                    }
                });
                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onDeleteClick(review);
                    }
                });
                btnReport.setVisibility(View.GONE);
            } else {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
                btnReport.setVisibility(View.VISIBLE);
                btnReport.setOnClickListener(v -> {
                    if (reportListener != null) {
                        reportListener.onReportClick(review);
                    }
                });
            }

            itemView.setAlpha(0f);
            itemView.setTranslationY(16f);
            itemView.animate().alpha(1f).translationY(0f).setDuration(220).start();
        }
    }
}
