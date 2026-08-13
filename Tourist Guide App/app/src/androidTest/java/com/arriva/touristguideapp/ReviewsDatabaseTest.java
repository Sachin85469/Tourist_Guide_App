package com.arriva.touristguideapp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.arriva.touristguideapp.data.reviews.ReviewRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ReviewsDatabaseTest {

    private ReviewRepository reviewRepository;
    private FirebaseAuth auth;

    @Before
    public void setUp() {
        reviewRepository = new ReviewRepository();
        auth = FirebaseAuth.getInstance();
    }

    @Test
    public void testFetchReviews_doesNotThrowFailedPrecondition() throws InterruptedException {
        // Find if there's a logged-in user or use a dummy ID
        FirebaseUser currentUser = auth.getCurrentUser();
        String uid = (currentUser != null) ? currentUser.getUid() : "DG5hDHXlfSbfSXXCOByuUsRMet62";

        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] errorHolder = new Throwable[1];

        reviewRepository.fetchUserReviews(uid, (reviews, exception, error) -> {
            if (exception != null) {
                errorHolder[0] = exception;
            } else if (error != null) {
                errorHolder[0] = new Exception(error);
            }
            latch.countDown();
        });

        assertTrue("Timeout waiting for reviews fetch", latch.await(10, TimeUnit.SECONDS));
        
        if (errorHolder[0] != null) {
            fail("Fetch reviews failed: " + errorHolder[0].getMessage());
        }
    }

    @Test
    public void testSubmitAndDeleteReview_runsSuccessfully() throws InterruptedException {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Test requires an authenticated user context
            return;
        }

        String placeId = "sinhagad_valley_view_point";
        String comment = "Automated integration test review - " + UUID.randomUUID().toString();
        float rating = 5.0f;

        Review review = new Review(
                currentUser.getUid(),
                currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Test User",
                currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null,
                rating,
                comment
        );
        review.setPlaceId(placeId);
        review.setPlaceName("Sinhagad Valley View Point");
        review.setReviewId(placeId + "_" + currentUser.getUid());
        review.setCreatedAt(new Date());

        // 1. Submit review
        CountDownLatch submitLatch = new CountDownLatch(1);
        final Throwable[] submitError = new Throwable[1];

        reviewRepository.submitReview(placeId, review).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                submitError[0] = task.getException();
            }
            submitLatch.countDown();
        });

        assertTrue("Timeout waiting for submitReview", submitLatch.await(15, TimeUnit.SECONDS));
        assertNull("Submit review failed", submitError[0]);

        // 2. Fetch specific review
        CountDownLatch fetchLatch = new CountDownLatch(1);
        final Review[] fetchedHolder = new Review[1];

        reviewRepository.getUserReview(placeId, currentUser.getUid(), (fetchedReview, error) -> {
            fetchedHolder[0] = fetchedReview;
            fetchLatch.countDown();
        });

        assertTrue("Timeout waiting for getUserReview", fetchLatch.await(10, TimeUnit.SECONDS));
        assertNotNull("Fetched review should not be null", fetchedHolder[0]);

        // 3. Delete review
        CountDownLatch deleteLatch = new CountDownLatch(1);
        final Throwable[] deleteError = new Throwable[1];

        reviewRepository.deleteReview(placeId, currentUser.getUid()).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                deleteError[0] = task.getException();
            }
            deleteLatch.countDown();
        });

        assertTrue("Timeout waiting for deleteReview", deleteLatch.await(10, TimeUnit.SECONDS));
        assertNull("Delete review failed", deleteError[0]);
    }
}
