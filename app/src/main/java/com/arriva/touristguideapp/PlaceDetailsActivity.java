package com.arriva.touristguideapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.arriva.touristguideapp.data.reviews.ReviewAdapter;
import com.arriva.touristguideapp.data.reviews.ReviewRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

public class PlaceDetailsActivity extends AppCompatActivity {

    private static final String TAG = "PlaceDetailsActivity";
    private String placeId;
    private ImageView btnFavorite;
    private double lat;
    private double lng;
    private ViewPager2 viewPagerGallery;

    // Review UI
    private ReviewRepository reviewRepository;
    private ReviewAdapter reviewAdapter;
    private RecyclerView rvReviews;
    private TextView tvNoReviews, tvRatingSummary;
    private ProgressBar pbReviewsLoading;
    private View cvAddReview;
    private RatingBar rbInputRating;
    private EditText etReviewComment;
    private Button btnSubmitReview;
    private ProgressBar pbSubmitReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_details);

        viewPagerGallery = findViewById(R.id.viewPagerGallery);
        TextView tvName = findViewById(R.id.detailName);
        TextView tvCategory = findViewById(R.id.detailCategory);
        TextView tvDescription = findViewById(R.id.detailDescription);
        TextView tvBudget = findViewById(R.id.detailBudget);
        TextView tvCrowd = findViewById(R.id.detailCrowd);
        TextView tvBestTime = findViewById(R.id.detailBestTime);
        TextView tvTips = findViewById(R.id.detailTips);
        TextView tvFunFact = findViewById(R.id.detailFunFact);
        TextView tvStation = findViewById(R.id.detailStation);
        
        // Favorite button in details
        btnFavorite = findViewById(R.id.btnFavoriteDetails);

        // Receive data from Intent
        Intent intent = getIntent();
        if (intent != null) {
            placeId = intent.getStringExtra("id");
            String name = intent.getStringExtra("name");
            String description = intent.getStringExtra("description");
            String category = intent.getStringExtra("category");
            String budget = intent.getStringExtra("budget");
            String crowdLevel = intent.getStringExtra("crowdLevel");
            String bestTime = intent.getStringExtra("bestTime");
            String tips = intent.getStringExtra("tips");
            String funFact = intent.getStringExtra("funFact");
            String station = intent.getStringExtra("nearestStation");
            lat = intent.getDoubleExtra("lat", 0);
            lng = intent.getDoubleExtra("lng", 0);
            double avgRating = intent.getDoubleExtra("avgRating", 0.0);
            long totalRatings = intent.getLongExtra("totalRatings", 0);

            // Remote / gallery fields
            String imageUrl = intent.getStringExtra("imageUrl");
            ArrayList<String> galleryImageUrls = intent.getStringArrayListExtra("galleryImageUrls");
            ArrayList<String> galleryUrlsExtra = intent.getStringArrayListExtra("galleryUrls");
            ArrayList<String> mergedGalleryUrls = mergeGalleryUrlExtras(galleryImageUrls, galleryUrlsExtra);
            
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                String u = imageUrl.trim();
                if (mergedGalleryUrls.isEmpty()) {
                    mergedGalleryUrls.add(u);
                } else if (!mergedGalleryUrls.contains(u)) {
                    mergedGalleryUrls.add(0, u);
                }
            }

            // Display data
            if (tvName != null) tvName.setText(name);
            if (tvDescription != null) tvDescription.setText(description);
            if (tvCategory != null) tvCategory.setText(category);
            if (tvBudget != null) tvBudget.setText(budget);
            if (tvCrowd != null) tvCrowd.setText(crowdLevel);
            if (tvBestTime != null) tvBestTime.setText(bestTime);
            if (tvTips != null) tvTips.setText(tips != null ? tips : "No tips available.");
            if (tvFunFact != null) tvFunFact.setText(funFact != null ? funFact : "Did you know? Pune is amazing!");
            if (tvStation != null) tvStation.setText(station != null ? station : "Not specified.");
            
            // Setup Review UI
            setupReviewUI();
            updateRatingSummary(avgRating, totalRatings);
            loadReviews();

            // Show fun fact popup
            new AlertDialog.Builder(this)
                .setTitle("Did you know?")
                .setMessage(funFact != null ? funFact : "Did you know? Pune is amazing!")
                .setPositiveButton("Cool!", null)
                .show();

            // Setup Gallery — exclusively uses remote URLs
            GalleryAdapter galleryAdapter = new GalleryAdapter(mergedGalleryUrls);
            if (viewPagerGallery != null) {
                viewPagerGallery.setAdapter(galleryAdapter);
            }

            updateFavoriteIcon();
            
            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> {
                    v.animate()
                        .scaleX(1.3f)
                        .scaleY(1.3f)
                        .setDuration(150)
                        .withEndAction(() -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(150);
                        });

                    FavoritesManager.toggleFavorite(this, placeId);
                    updateFavoriteIcon();
                });
            }

            View btnDirections = findViewById(R.id.btnDirections);
            if (btnDirections != null) {
                btnDirections.setOnClickListener(v -> {
                    Uri uri = Uri.parse("google.navigation:q=" + lat + "," + lng);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        // Fallback to any browser if Maps app is not available
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
                });
            }
        }
    }

    private void setupReviewUI() {
        reviewRepository = new ReviewRepository();
        tvRatingSummary = findViewById(R.id.tvRatingSummary);
        rvReviews = findViewById(R.id.rvReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        pbReviewsLoading = findViewById(R.id.pbReviewsLoading);
        cvAddReview = findViewById(R.id.cvAddReview);
        rbInputRating = findViewById(R.id.rbInputRating);
        etReviewComment = findViewById(R.id.etReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        pbSubmitReview = findViewById(R.id.pbSubmitReview);

        reviewAdapter = new ReviewAdapter();
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            cvAddReview.setVisibility(View.VISIBLE);
            // Preload user review if exists
            reviewRepository.getUserReview(placeId, currentUser.getUid(), (review, error) -> {
                if (review != null) {
                    rbInputRating.setRating(review.getRating());
                    etReviewComment.setText(review.getComment());
                    btnSubmitReview.setText(R.string.update_review);
                }
            });

            btnSubmitReview.setOnClickListener(v -> {
                Log.d(TAG, "REVIEW_SUBMIT_CLICKED placeId=" + placeId);
                submitReview(currentUser);
            });
        } else {
            cvAddReview.setVisibility(View.GONE);
        }
        
        Log.d(TAG, "REVIEW_UI_LOADED placeId=" + placeId);
    }

    private void loadReviews() {
        pbReviewsLoading.setVisibility(View.VISIBLE);
        rvReviews.setVisibility(View.GONE);
        tvNoReviews.setVisibility(View.GONE);

        reviewRepository.fetchReviews(placeId, (reviews, error) -> {
            pbReviewsLoading.setVisibility(View.GONE);
            if (error != null) {
                Toast.makeText(PlaceDetailsActivity.this, "Failed to load reviews: " + error, Toast.LENGTH_SHORT).show();
                return;
            }

            if (reviews.isEmpty()) {
                tvNoReviews.setVisibility(View.VISIBLE);
                rvReviews.setVisibility(View.GONE);
            } else {
                tvNoReviews.setVisibility(View.GONE);
                rvReviews.setVisibility(View.VISIBLE);
                reviewAdapter.setReviews(reviews);
                Log.d(TAG, "REVIEW_LIST_UPDATED count=" + reviews.size());
            }
        });
    }

    private void submitReview(FirebaseUser user) {
        float rating = rbInputRating.getRating();
        String comment = etReviewComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitReview.setEnabled(false);
        btnSubmitReview.setText("");
        pbSubmitReview.setVisibility(View.VISIBLE);

        Review review = new Review(user.getUid(), 
                                   user.getDisplayName() != null ? user.getDisplayName() : "Anonymous", 
                                   user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null, 
                                   rating, 
                                   comment);

        reviewRepository.submitReview(placeId, review).addOnCompleteListener(task -> {
            btnSubmitReview.setEnabled(true);
            btnSubmitReview.setText(R.string.submit_review);
            pbSubmitReview.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                Toast.makeText(this, "Review submitted successfully", Toast.LENGTH_SHORT).show();
                loadReviews(); // Refresh list
                btnSubmitReview.setText(R.string.update_review);
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Failed to submit review: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRatingSummary(double avg, long total) {
        if (tvRatingSummary != null) {
            if (total == 0) {
                tvRatingSummary.setText("⭐ No ratings yet");
            } else {
                tvRatingSummary.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d reviews)", avg, total));
            }
        }
    }

    @NonNull
    private static ArrayList<String> mergeGalleryUrlExtras(@Nullable ArrayList<String> a,
                                                           @Nullable ArrayList<String> b) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (a != null) {
            for (String s : a) {
                if (s != null && !s.trim().isEmpty()) {
                    set.add(s.trim());
                }
            }
        }
        if (b != null) {
            for (String s : b) {
                if (s != null && !s.trim().isEmpty()) {
                    set.add(s.trim());
                }
            }
        }
        return new ArrayList<>(set);
    }

    private void updateFavoriteIcon() {
        if (btnFavorite != null && placeId != null) {
            if (FavoritesManager.isFavorite(this, placeId)) {
                btnFavorite.setImageResource(R.drawable.ic_favorite);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
        }
    }
}
