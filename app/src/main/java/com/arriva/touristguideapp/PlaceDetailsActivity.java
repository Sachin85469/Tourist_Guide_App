package com.arriva.touristguideapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;

public class PlaceDetailsActivity extends AppCompatActivity {

    private String placeId;
    private ImageView btnFavorite;
    private double lat;
    private double lng;
    private String placeName;
    private ViewPager2 viewPagerGallery;

    // Review UI
    private ReviewRepository reviewRepository;
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
        PerformanceTracker.startTimer("PLACE_DETAILS_INIT");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_details);

        bindViews();
        handleIntentData();
        
        PerformanceTracker.endTimer("PLACE_DETAILS_INIT");
    }

    private void bindViews() {
        viewPagerGallery = findViewById(R.id.viewPagerGallery);
        btnFavorite = findViewById(R.id.btnFavoriteDetails);
        rvReviews = findViewById(R.id.rvReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        pbReviewsLoading = findViewById(R.id.pbReviewsLoading);
        btnRetryReviews = findViewById(R.id.btnRetryReviews);
        nsvPlaceDetails = findViewById(R.id.nsvPlaceDetails);
        cvAddReview = findViewById(R.id.cvAddReview);
        tvRatingSummary = findViewById(R.id.tvRatingSummary);
        rbInputRating = findViewById(R.id.rbInputRating);
        etReviewComment = findViewById(R.id.etReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        pbSubmitReview = findViewById(R.id.pbSubmitReview);
        llNearbyPlaces = findViewById(R.id.llNearbyPlaces);
        rvNearbyPlaces = findViewById(R.id.rvNearbyPlaces);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnShare).setOnClickListener(v -> sharePlace());
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        placeId = intent.getStringExtra("id");
        placeName = intent.getStringExtra("name");
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
        long totalComments = intent.getLongExtra("totalComments", 0);

        // Remote / gallery fields
        String imageUrl = intent.getStringExtra("imageUrl");
        ArrayList<String> galleryImageUrls = intent.getStringArrayListExtra("galleryImageUrls");
        ArrayList<String> galleryUrlsExtra = intent.getStringArrayListExtra("galleryUrls");
        ArrayList<String> mergedGalleryUrls = mergeGalleryUrlExtras(galleryImageUrls, galleryUrlsExtra);
        
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            String u = imageUrl.trim();
            if (!mergedGalleryUrls.contains(u)) {
                mergedGalleryUrls.add(0, u);
            }
        }

        // Basic Info
        ((TextView) findViewById(R.id.detailName)).setText(placeName);
        ((TextView) findViewById(R.id.detailCategory)).setText(category);
        ((TextView) findViewById(R.id.detailDescription)).setText(description);
        ((TextView) findViewById(R.id.detailTips)).setText(Objects.requireNonNullElse(tips, "Explore and enjoy!"));
        ((TextView) findViewById(R.id.detailFunFact)).setText(Objects.requireNonNullElse(funFact, "Discover something new!"));
        ((TextView) findViewById(R.id.detailStation)).setText(String.format("Nearest: %s", station != null ? station : "City Center"));

        // Expandable Description
        setupExpandableDescription(description);

        // Chips
        setupChips(bestTime, crowdLevel, budget);

        // Gallery
        setupGallery(mergedGalleryUrls);

        // Actions
        setupActionButtons();

        // Review UI
        setupReviewUI();
        updateRatingSummary(avgRating, totalRatings, totalComments);
        
        // Tracking & History
        trackVisit(imageUrl, category, avgRating, totalRatings, intent);

        // Nearby
        setupNearbyUI();

        updateFavoriteIcon();
        
        animateEntrance();
    }

    private void animateEntrance() {
        View content = findViewById(R.id.nsvPlaceDetails);
        if (content != null) {
            content.setAlpha(0f);
            content.setTranslationY(100f);
            content.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        }
        
        View fab = findViewById(R.id.btnFavoriteDetails);
        if (fab != null) {
            fab.setScaleX(0f);
            fab.setScaleY(0f);
            fab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();
        }
    }

    private void setupExpandableDescription(String description) {
        TextView tvDesc = findViewById(R.id.detailDescription);
        TextView btnReadMore = findViewById(R.id.btnReadMore);
        
        if (description != null && description.length() > 200) {
            tvDesc.setMaxLines(4);
            btnReadMore.setVisibility(View.VISIBLE);
            btnReadMore.setOnClickListener(v -> {
                if (tvDesc.getMaxLines() == 4) {
                    tvDesc.setMaxLines(Integer.MAX_VALUE);
                    btnReadMore.setText("Show Less");
                } else {
                    tvDesc.setMaxLines(4);
                    btnReadMore.setText("Read More");
                }
            });
        } else {
            btnReadMore.setVisibility(View.GONE);
        }
    }

    private void setupChips(String bestTime, String crowd, String budget) {
        Chip chipTime = findViewById(R.id.chipBestTime);
        Chip chipCrowd = findViewById(R.id.chipCrowd);
        Chip chipBudget = findViewById(R.id.chipBudget);

        if (bestTime != null) chipTime.setText(String.format("Best: %s", bestTime));
        else chipTime.setVisibility(View.GONE);

        if (crowd != null) chipCrowd.setText(String.format("%s Crowd", crowd));
        else chipCrowd.setVisibility(View.GONE);

        if (budget != null) chipBudget.setText(String.format("Budget: %s", budget));
        else chipBudget.setVisibility(View.GONE);
    }

    private void setupGallery(List<String> urls) {
        if (urls.isEmpty()) return;

        GalleryAdapter adapter = new GalleryAdapter(urls);
        viewPagerGallery.setAdapter(adapter);

        RecyclerView rvPreview = findViewById(R.id.rvGalleryPreview);
        rvPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        GalleryPreviewAdapter previewAdapter = new GalleryPreviewAdapter(urls, position -> viewPagerGallery.setCurrentItem(position, true));
        rvPreview.setAdapter(previewAdapter);
    }

    private void setupActionButtons() {
        findViewById(R.id.btnDirections).setOnClickListener(v -> openDirections());
        findViewById(R.id.btnCall).setOnClickListener(v -> makeCall());
        findViewById(R.id.btnSave).setOnClickListener(v -> toggleFavorite());
        findViewById(R.id.btnExploreMap).setOnClickListener(v -> openDirections());
        
        btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void openDirections() {
        Uri uri = Uri.parse("google.navigation:q=" + lat + "," + lng);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    private void makeCall() {
        Toast.makeText(this, "Call feature coming soon for verified spots!", Toast.LENGTH_SHORT).show();
    }

    private void sharePlace() {
        String shareText = String.format(Locale.getDefault(), "Check out %s on Tourist Guide App!\nLocation: https://www.google.com/maps/search/?api=1&query=%f,%f",
                placeName, lat, lng);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, placeName);
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void toggleFavorite() {
        btnFavorite.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(150)
            .withEndAction(() -> btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(150));

        FavoritesManager.toggleFavorite(this, placeId);
        updateFavoriteIcon();
    }

    private void trackVisit(String imageUrl, String category, double avgRating, long totalRatings, Intent intent) {
        Place currentPlace = new Place();
        currentPlace.setId(placeId);
        currentPlace.setName(placeName);
        currentPlace.setCategory(category);
        currentPlace.setImageUrl(imageUrl);
        currentPlace.setRating(avgRating);
        currentPlace.setTotalRatings(totalRatings);
        currentPlace.setTotalComments(intent.getLongExtra("totalComments", 0));
        currentPlace.setBudget(intent.getStringExtra("budget"));
        currentPlace.setLatitude(lat);
        currentPlace.setLongitude(lng);
        currentPlace.setCity(intent.getStringExtra("city"));
        currentPlace.setTag(intent.getStringExtra("tag"));
        
        new com.arriva.touristguideapp.data.places.RecentlyViewedManager(this).addRecentlyViewed(currentPlace);
    }

    private void setupNearbyUI() {
        if (rvNearbyPlaces != null) {
            rvNearbyPlaces.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            
            new PlaceRepository().fetchPublishedPlaces((places, origin, message) -> {
                if (!places.isEmpty()) {
                    List<Place> otherPlaces = new ArrayList<>();
                    for (Place p : places) {
                        if (!Objects.equals(p.getId(), placeId)) {
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
        new AnalyticsRepository().trackPlaceView(placeId);

        btnRetryReviews.setOnClickListener(v -> {
            stopListeners();
            startReviewListener();
            startPlaceListener();
        });

        rvReviews.setHasFixedSize(false);
        rvReviews.setItemViewCacheSize(20);
        rvReviews.setNestedScrollingEnabled(false);

        reviewAdapter = new ReviewAdapter();
        reviewAdapter.setOnReviewDeleteListener(this::deleteReview);
        reviewAdapter.setOnReviewReportListener(this::showReportDialog);
        
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);

        nsvPlaceDetails.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > oldScrollY) {
                View child = v.getChildAt(v.getChildCount() - 1);
                int diff = (child.getBottom() - (v.getHeight() + v.getScrollY()));
                if (diff <= 500 && !isPaginationLoading && hasMoreReviews) {
                    loadMoreReviews();
                }
            }
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            cvAddReview.setVisibility(View.VISIBLE);
            reviewRepository.getUserReview(placeId, currentUser.getUid(), (review, error) -> {
                if (review != null) {
                    rbInputRating.setRating(review.getRating());
                    etReviewComment.setText(review.getComment());
                    btnSubmitReview.setText(R.string.update_review);
                }
            });

            btnSubmitReview.setOnClickListener(v -> submitReview(currentUser));
        } else {
            cvAddReview.setVisibility(View.GONE);
        }
    }

    private void showReportDialog(Review review) {
        final String[] reasons = {"Spam", "Inappropriate content", "Hate speech", "Harassment", "Other"};
        new AlertDialog.Builder(this)
            .setTitle("Report Review")
            .setItems(reasons, (dialog, which) -> reportReview(review, reasons[which]))
            .setNegativeButton("Cancel", null)
            .show();
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
            
            if (error != null) {
                btnRetryReviews.setVisibility(View.VISIBLE);
                return;
            }

            if (reviews.isEmpty() && currentReviewLimit == 10) {
                tvNoReviews.setVisibility(View.VISIBLE);
                rvReviews.setVisibility(View.GONE);
                hasMoreReviews = false;
            } else {
                tvNoReviews.setVisibility(View.GONE);
                rvReviews.setVisibility(View.VISIBLE);
                hasMoreReviews = reviews.size() >= currentReviewLimit;
                reviewAdapter.setReviews(reviews);
            }
        });
    }

    private void loadMoreReviews() {
        isPaginationLoading = true;
        currentReviewLimit += 10;
        if (reviewsListener != null) {
            reviewsListener.remove();
            reviewsListener = null;
        }
        startReviewListener();
    }

    private void startPlaceListener() {
        if (placeListener != null) return;
        placeListener = reviewRepository.listenToPlace(placeId, this::updateRatingSummary);
    }

    private void stopListeners() {
        if (reviewsListener != null) {
            reviewsListener.remove();
            reviewsListener = null;
        }
        if (placeListener != null) {
            placeListener.remove();
            placeListener = null;
        }
    }

    private void deleteReview(Review review) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Review")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete", (d, w) -> {
                reviewRepository.deleteReview(placeId, review.getUserId()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Review deleted", Toast.LENGTH_SHORT).show();
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null && Objects.equals(user.getUid(), review.getUserId())) {
                            rbInputRating.setRating(0);
                            etReviewComment.setText("");
                            btnSubmitReview.setText(R.string.submit_review);
                        }
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void reportReview(Review review, String reason) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        reviewRepository.reportReview(placeId, review.getUserId(), user.getUid(), reason)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Review reported", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void submitReview(FirebaseUser user) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSubmitTime < SUBMIT_COOLDOWN_MS) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = rbInputRating.getRating();
        String comment = etReviewComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitReview.setEnabled(false);
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
                lastSubmitTime = System.currentTimeMillis();
                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                btnSubmitReview.setText(R.string.update_review);
            }
        });
    }

    private void updateRatingSummary(double avg, long total, long comments) {
        if (tvRatingSummary != null) {
            if (total == 0) tvRatingSummary.setText("No reviews yet");
            else tvRatingSummary.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d reviews)", avg, total));
        }
    }

    @NonNull
    private static ArrayList<String> mergeGalleryUrlExtras(@Nullable ArrayList<String> a, @Nullable ArrayList<String> b) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (a != null) for (String s : a) if (s != null && !s.trim().isEmpty()) set.add(s.trim());
        if (b != null) for (String s : b) if (s != null && !s.trim().isEmpty()) set.add(s.trim());
        return new ArrayList<>(set);
    }

    private void updateFavoriteIcon() {
        if (btnFavorite != null && placeId != null) {
            boolean fav = FavoritesManager.isFavorite(this, placeId);
            btnFavorite.setImageResource(fav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            
            Button btnSave = findViewById(R.id.btnSave);
            if (btnSave != null) {
                btnSave.setText(fav ? "Saved" : "Save");
                btnSave.setCompoundDrawablesWithIntrinsicBounds(fav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border, 0, 0, 0);
            }
        }
    }
}
