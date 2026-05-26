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
import com.arriva.touristguideapp.data.places.PlaceRepository;
import com.arriva.touristguideapp.data.analytics.AnalyticsRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;
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
    private AnalyticsRepository analyticsRepository;
    private ReviewAdapter reviewAdapter;
    private com.google.firebase.firestore.ListenerRegistration reviewsListener;
    private com.google.firebase.firestore.ListenerRegistration placeListener;
    private int currentReviewLimit = 10;
    private boolean isPaginationLoading = false;
    private boolean hasMoreReviews = true;
    private RecyclerView rvReviews;
    private TextView tvNoReviews, tvRatingSummary;
    private ProgressBar pbReviewsLoading;
    private Button btnRetryReviews;
    private RecyclerView rvNearbyPlaces;
    private View llNearbyPlaces;
    private androidx.core.widget.NestedScrollView nsvPlaceDetails;
    private View cvAddReview;
    private RatingBar rbInputRating;
    private EditText etReviewComment;
    private Button btnSubmitReview;
    private ProgressBar pbSubmitReview;

    private long lastSubmitTime = 0;
    private static final long SUBMIT_COOLDOWN_MS = 10000; // 10 seconds anti-spam

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
            
            // Add to Recently Viewed (Requirement 5 - Improved)
            Place currentPlace = new Place();
            currentPlace.setId(placeId);
            currentPlace.setName(name);
            currentPlace.setCategory(category);
            currentPlace.setImageUrl(imageUrl);
            currentPlace.setRating(avgRating);
            currentPlace.setTotalRatings(totalRatings);
            currentPlace.setTotalComments(intent.getLongExtra("totalComments", 0));
            currentPlace.setBudget(budget);
            currentPlace.setLatitude(lat);
            currentPlace.setLongitude(lng);
            currentPlace.setCity(intent.getStringExtra("city"));
            currentPlace.setTag(intent.getStringExtra("tag"));
            
            new com.arriva.touristguideapp.data.places.RecentlyViewedManager(this).addRecentlyViewed(currentPlace);

            // Setup Nearby UI
            setupNearbyUI();

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

    private void setupNearbyUI() {
        llNearbyPlaces = findViewById(R.id.llNearbyPlaces);
        rvNearbyPlaces = findViewById(R.id.rvNearbyPlaces);
        
        if (rvNearbyPlaces != null) {
            rvNearbyPlaces.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            
            // Load all places and filter for nearby
            new PlaceRepository().fetchPublishedPlaces((places, origin, message) -> {
                if (!places.isEmpty()) {
                    // Filter out current place
                    List<Place> otherPlaces = new ArrayList<>();
                    for (Place p : places) {
                        if (!p.getId().equals(placeId)) {
                            otherPlaces.add(p);
                        }
                    }
                    
                    List<Place> nearby = LocationUtils.getNearbyPlaces(otherPlaces, lat, lng, 5);
                    if (!nearby.isEmpty()) {
                        llNearbyPlaces.setVisibility(View.VISIBLE);
                        TopPickAdapter adapter = new TopPickAdapter(nearby, p -> {
                            Intent intent = new Intent(PlaceDetailsActivity.this, PlaceDetailsActivity.class);
                            PlaceIntentExtras.putPlaceDetails(intent, p);
                            startActivity(intent);
                        });
                        rvNearbyPlaces.setAdapter(adapter);
                    }
                }
            });
        }
    }

    private void setupReviewUI() {
        reviewRepository = new ReviewRepository();
        analyticsRepository = new AnalyticsRepository();
        
        // Track View
        analyticsRepository.trackPlaceView(placeId);

        tvRatingSummary = findViewById(R.id.tvRatingSummary);
        rvReviews = findViewById(R.id.rvReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        pbReviewsLoading = findViewById(R.id.pbReviewsLoading);
        btnRetryReviews = findViewById(R.id.btnRetryReviews);
        nsvPlaceDetails = findViewById(R.id.nsvPlaceDetails);
        cvAddReview = findViewById(R.id.cvAddReview);
        
        btnRetryReviews.setOnClickListener(v -> {
            stopListeners();
            startReviewListener();
            startPlaceListener();
        });

        // RecyclerView Optimizations
        rvReviews.setHasFixedSize(false); // Comments vary in length
        rvReviews.setItemViewCacheSize(20);
        rvReviews.setNestedScrollingEnabled(false);

        rbInputRating = findViewById(R.id.rbInputRating);
        etReviewComment = findViewById(R.id.etReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        pbSubmitReview = findViewById(R.id.pbSubmitReview);

        reviewAdapter = new ReviewAdapter();
        reviewAdapter.setOnReviewDeleteListener(review -> {
            // TODO: Add moderation/report feature here for other users' reviews
            new AlertDialog.Builder(this)
                .setTitle("Delete Review")
                .setMessage("Are you sure you want to delete your review?")
                .setPositiveButton("Delete", (dialog, which) -> deleteReview(review))
                .setNegativeButton("Cancel", null)
                .show();
        });
        
        reviewAdapter.setOnReviewReportListener(review -> {
            final String[] reasons = {"Spam", "Inappropriate content", "Hate speech", "Harassment", "Other"};
            new AlertDialog.Builder(this)
                .setTitle("Report Review")
                .setItems(reasons, (dialog, which) -> {
                    reportReview(review, reasons[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvReviews.setLayoutManager(layoutManager);
        rvReviews.setAdapter(reviewAdapter);

        // Pagination Scroll Listener on NestedScrollView
        nsvPlaceDetails.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > oldScrollY) { // Scrolling down
                // Check if we are near the bottom
                View child = v.getChildAt(v.getChildCount() - 1);
                int diff = (child.getBottom() - (v.getHeight() + v.getScrollY()));
                
                if (diff <= 300) { // 300px before bottom
                    if (!isPaginationLoading && hasMoreReviews) {
                        loadMoreReviews();
                    }
                }
            }
        });

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        startReviewListener();
        startPlaceListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopListeners();
    }

    private void startReviewListener() {
        if (reviewsListener != null) return;

        pbReviewsLoading.setVisibility(View.VISIBLE);
        btnRetryReviews.setVisibility(View.GONE);
        reviewsListener = reviewRepository.listenToReviews(placeId, currentReviewLimit, (reviews, error) -> {
            pbReviewsLoading.setVisibility(View.GONE);
            isPaginationLoading = false;
            Log.d(TAG, "REVIEW_REALTIME_EVENT: trigger=FirestoreSnapshot count=" + reviews.size());
            Log.d(TAG, "REVIEW_REALTIME_UPDATE count=" + reviews.size() + " limit=" + currentReviewLimit);
            
            if (error != null) {
                Toast.makeText(this, "Error syncing reviews: " + error, Toast.LENGTH_SHORT).show();
                btnRetryReviews.setVisibility(View.VISIBLE);
                return;
            }

            btnRetryReviews.setVisibility(View.GONE);
            if (reviews.isEmpty() && currentReviewLimit == 10) {
                tvNoReviews.setVisibility(View.VISIBLE);
                rvReviews.setVisibility(View.GONE);
                hasMoreReviews = false;
            } else {
                tvNoReviews.setVisibility(View.GONE);
                rvReviews.setVisibility(View.VISIBLE);
                
                // If the number of reviews returned is less than current limit, there are no more reviews
                hasMoreReviews = reviews.size() >= currentReviewLimit;
                
                Log.d(TAG, "REVIEW_ADAPTER_SUBMIT: size=" + reviews.size());
                reviewAdapter.setReviews(reviews);
                Log.d(TAG, "REVIEW_LIST_FINAL_SIZE: " + reviewAdapter.getItemCount());
            }
        });
        Log.d(TAG, "REVIEW_LISTENER_ATTACHED placeId=" + placeId + " limit=" + currentReviewLimit);
    }

    private void loadMoreReviews() {
        Log.d(TAG, "LOAD_MORE_REVIEWS triggered");
        isPaginationLoading = true;
        currentReviewLimit += 10;
        
        // Remove old listener and start new one with larger limit
        if (reviewsListener != null) {
            reviewsListener.remove();
            reviewsListener = null;
        }
        startReviewListener();
    }

    private void startPlaceListener() {
        if (placeListener != null) return;

        placeListener = reviewRepository.listenToPlace(placeId, (avg, total, comments) -> {
            updateRatingSummary(avg, total);
        });
    }

    private void stopListeners() {
        if (reviewsListener != null) {
            reviewsListener.remove();
            reviewsListener = null;
            Log.d(TAG, "REVIEW_LISTENER_REMOVED placeId=" + placeId);
        }
        if (placeListener != null) {
            placeListener.remove();
            placeListener = null;
        }
    }

    private void deleteReview(Review review) {
        reviewRepository.deleteReview(placeId, review.getUserId()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Review deleted", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "REVIEW_DELETED userId=" + review.getUserId());
                
                // Clear input if it was the deleted review
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getUid().equals(review.getUserId())) {
                    rbInputRating.setRating(0);
                    etReviewComment.setText("");
                    btnSubmitReview.setText(R.string.submit_review);
                }
            } else {
                Toast.makeText(this, "Failed to delete review", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reportReview(Review review, String reason) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login to report", Toast.LENGTH_SHORT).show();
            return;
        }

        reviewRepository.reportReview(placeId, review.getUserId(), user.getUid(), reason)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Review reported. Thank you.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to report review", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void submitReview(FirebaseUser user) {
        // Anti-spam: Rapid duplicate prevention
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSubmitTime < SUBMIT_COOLDOWN_MS) {
            Toast.makeText(this, "Please wait a few seconds before submitting again.", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = rbInputRating.getRating();
        String comment = etReviewComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (comment.length() > 500) {
            Toast.makeText(this, "Comment is too long (max 500 chars)", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitReview.setEnabled(false);
        btnSubmitReview.setText("");
        pbSubmitReview.setVisibility(View.VISIBLE);

        // TODO: Future moderation - scan comment for banned words/spam patterns
        
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
                lastSubmitTime = System.currentTimeMillis();
                Toast.makeText(this, "Review submitted successfully", Toast.LENGTH_SHORT).show();
                btnSubmitReview.setText(R.string.update_review);
                
                // Heuristic: New reviews appear at top. 
                // Updated reviews might be further down, but listener will refresh the list.
                rvReviews.postDelayed(() -> rvReviews.smoothScrollToPosition(0), 500);
            } else {
                Exception e = task.getException();
                String error = e != null ? e.getMessage() : "Unknown error";
                
                if (error != null && error.contains("offline")) {
                    Log.d(TAG, "OFFLINE_REVIEW_QUEUE: Review will sync when online");
                    Toast.makeText(this, "Offline: Review will sync when you're back online", Toast.LENGTH_LONG).show();
                } else if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException && 
                           ((com.google.firebase.firestore.FirebaseFirestoreException)e).getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(TAG, "SECURITY_RULE_BLOCK: You don't have permission to perform this action");
                    Toast.makeText(this, "Action denied by security policy", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to submit review: " + error, Toast.LENGTH_SHORT).show();
                }
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
