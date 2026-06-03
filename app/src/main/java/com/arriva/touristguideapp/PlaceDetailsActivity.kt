package com.arriva.touristguideapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.arriva.touristguideapp.data.analytics.AnalyticsRepository
import com.arriva.touristguideapp.data.places.PlaceRepository
import com.arriva.touristguideapp.data.reviews.ReviewAdapter
import com.arriva.touristguideapp.data.reviews.ReviewRepository
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class PlaceDetailsActivity : BaseActivity() {

    private var placeId: String? = null
    private var btnFavorite: ImageView? = null
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var placeName: String? = null
    private var currentPlace: Place? = null
    private var viewPagerGallery: ViewPager2? = null

    // Review UI
    private lateinit var reviewRepository: ReviewRepository
    private var reviewAdapter: ReviewAdapter? = null
    private var reviewsListener: ListenerRegistration? = null
    private var placeListener: ListenerRegistration? = null
    private var currentReviewLimit = 10
    private var isPaginationLoading = false
    private var hasMoreReviews = true
    private var rvReviews: RecyclerView? = null
    private var tvNoReviews: TextView? = null
    private var tvRatingSummary: TextView? = null
    private var pbReviewsLoading: ProgressBar? = null
    private var btnRetryReviews: Button? = null
    private var rvNearbyPlaces: RecyclerView? = null
    private var llNearbyPlaces: View? = null
    private var nsvPlaceDetails: androidx.core.widget.NestedScrollView? = null
    private var cvAddReview: View? = null
    private var rbInputRating: RatingBar? = null
    private var etReviewComment: EditText? = null
    private var btnSubmitReview: Button? = null
    private var pbSubmitReview: ProgressBar? = null

    private var lastSubmitTime: Long = 0
    private val SUBMIT_COOLDOWN_MS: Long = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        PerformanceTracker.startTimer("PLACE_DETAILS_INIT")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place_details)

        bindViews()
        handleIntentData()
        
        PerformanceTracker.endTimer("PLACE_DETAILS_INIT")
    }

    private fun bindViews() {
        viewPagerGallery = findViewById(R.id.viewPagerGallery)
        btnFavorite = findViewById(R.id.btnFavoriteDetails)
        rvReviews = findViewById(R.id.rvReviews)
        tvNoReviews = findViewById(R.id.tvNoReviews)
        pbReviewsLoading = findViewById(R.id.pbReviewsLoading)
        btnRetryReviews = findViewById(R.id.btnRetryReviews)
        nsvPlaceDetails = findViewById(R.id.nsvPlaceDetails)
        cvAddReview = findViewById(R.id.cvAddReview)
        tvRatingSummary = findViewById(R.id.tvRatingSummary)
        rbInputRating = findViewById(R.id.rbInputRating)
        etReviewComment = findViewById(R.id.etReviewComment)
        btnSubmitReview = findViewById(R.id.btnSubmitReview)
        pbSubmitReview = findViewById(R.id.pbSubmitReview)
        llNearbyPlaces = findViewById(R.id.llNearbyPlaces)
        rvNearbyPlaces = findViewById(R.id.rvNearbyPlaces)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnShare).setOnClickListener { sharePlace() }
    }

    private fun handleIntentData() {
        val intent = intent ?: return

        placeId = intent.getStringExtra("id")
        placeName = intent.getStringExtra("name")
        
        // If we only have ID, try to load from repository (Local or Remote)
        if (placeName == null && placeId != null) {
            loadPlaceDetails(placeId!!)
        } else {
            initUiWithIntent(intent)
        }
    }

    private fun initUiWithIntent(intent: Intent) {
        val description = intent.getStringExtra("description")
        val category = intent.getStringExtra("category")
        val budget = intent.getStringExtra("budget")
        val crowdLevel = intent.getStringExtra("crowdLevel")
        val bestTime = intent.getStringExtra("bestTime")
        val tips = intent.getStringExtra("tips")
        val funFact = intent.getStringExtra("funFact")
        val station = intent.getStringExtra("nearestStation")
        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng", 0.0)
        val avgRating = intent.getDoubleExtra("avgRating", 0.0)
        val totalRatings = intent.getLongExtra("totalRatings", 0)
        val totalComments = intent.getLongExtra("totalComments", 0)

        // Remote / gallery fields
        val imageUrl = intent.getStringExtra("imageUrl")
        val galleryImageUrls = intent.getStringArrayListExtra("galleryImageUrls")
        val galleryUrlsExtra = intent.getStringArrayListExtra("galleryUrls")
        val mergedGalleryUrls = mergeGalleryUrlExtras(galleryImageUrls, galleryUrlsExtra)
        
        if (!imageUrl.isNullOrBlank()) {
            val u = imageUrl.trim()
            if (!mergedGalleryUrls.contains(u)) {
                mergedGalleryUrls.add(0, u)
            }
        }

        // Basic Info
        findViewById<TextView>(R.id.detailName).text = placeName
        findViewById<TextView>(R.id.detailCategory).text = category
        findViewById<TextView>(R.id.detailDescription).text = description
        findViewById<TextView>(R.id.detailTips).text = tips ?: "Explore and enjoy!"
        findViewById<TextView>(R.id.detailFunFact).text = funFact ?: "Discover something new!"
        findViewById<TextView>(R.id.detailStation).text = "Nearest: ${station ?: "City Center"}"

        setupExpandableDescription(description)
        setupChips(bestTime, crowdLevel, budget)
        setupGallery(mergedGalleryUrls)
        setupActionButtons()

        checkStatus()
        setupReviewUI()
        updateRatingSummary(avgRating, totalRatings, totalComments)
        trackVisit(imageUrl, category, avgRating, totalRatings, intent)
        
        AnalyticsRepository().trackPlaceView(placeId)

        setupNearbyUI()
        animateEntrance()
    }

    private fun loadPlaceDetails(id: String) {
        // Try remote
        PlaceRepository().fetchPublishedPlaces { places, _, _ ->
            val place = places.find { it.id == id }
            if (place != null) {
                val intent = Intent()
                PlaceIntentExtras.putPlaceDetails(intent, place)
                placeName = place.name
                initUiWithIntent(intent)
            }
        }
    }

    private fun checkStatus() {
        val id = placeId ?: return
        val isFav = FavoritesManager.isFavorite(this, id)
        updateFavoriteIcon(isFav)
        updateDownloadIcon(false)
    }

    private fun updateDownloadIcon(isDownloaded: Boolean) {
        // btnDownload is not in layout yet
    }

    private fun updateFavoriteIcon(isFav: Boolean) {
        btnFavorite?.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        findViewById<Button>(R.id.btnSave)?.apply {
            text = if (isFav) "Saved" else "Save"
            setCompoundDrawablesWithIntrinsicBounds(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border, 0, 0, 0)
        }
    }

    private fun setupActionButtons() {
        findViewById<View>(R.id.btnDirections).setOnClickListener { openDirections() }
        findViewById<View>(R.id.btnCall).setOnClickListener { makeCall() }
        findViewById<View>(R.id.btnSave).setOnClickListener { toggleFavorite() }
        findViewById<View>(R.id.btnExploreMap).setOnClickListener { openDirections() }
        
        val btnNavigateNow = findViewById<Button>(R.id.btnDirections)
        btnNavigateNow.text = "Navigate Now"

        btnFavorite?.setOnClickListener { toggleFavorite() }
    }

    private fun downloadPlace() {
        // Feature disabled as ProfileRepository is missing
        Toast.makeText(this, "Offline downloads coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFavorite() {
        val place = currentPlace ?: return
        btnFavorite?.animate()?.scaleX(1.3f)?.scaleY(1.3f)?.setDuration(150)?.withEndAction {
            btnFavorite?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)
        }

        val wasFavorite = FavoritesManager.isFavorite(this, place.id)
        FavoritesManager.toggleFavorite(this, place)
        val isFav = FavoritesManager.isFavorite(this, place.id)
        updateFavoriteIcon(isFav)

        // Generate notification for favorite change
        try {
            val notifTitle = if (isFav) "Destination Saved" else "Destination Removed"
            val notifMessage = if (isFav) {
                "${place.name} was added to your favorites."
            } else {
                "${place.name} was removed from your favorites."
            }
            val notif = com.arriva.touristguideapp.data.notifications.NotificationModel(
                java.util.UUID.randomUUID().toString(),
                notifTitle,
                notifMessage,
                com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_FAVORITE,
                place.id,
                System.currentTimeMillis()
            ).apply {
                userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                isRead = false
            }
            com.arriva.touristguideapp.data.notifications.NotificationRepository(this).saveToHistory(notif)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupReviewUI() {
        reviewRepository = ReviewRepository()
        AnalyticsRepository().trackPlaceView(placeId)

        btnRetryReviews?.setOnClickListener {
            stopListeners()
            startReviewListener()
            startPlaceListener()
        }

        rvReviews?.layoutManager = LinearLayoutManager(this)
        reviewAdapter = ReviewAdapter().apply {
            setOnReviewDeleteListener { deleteReview(it) }
            setOnReviewReportListener { showReportDialog(it) }
        }
        rvReviews?.adapter = reviewAdapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            cvAddReview?.visibility = View.VISIBLE
            reviewRepository.getUserReview(placeId, currentUser.uid) { review, _ ->
                if (review != null) {
                    rbInputRating?.rating = review.rating
                    etReviewComment?.setText(review.comment)
                    btnSubmitReview?.setText(R.string.update_review)
                }
            }
            btnSubmitReview?.setOnClickListener { submitReview(currentUser) }
        } else {
            cvAddReview?.visibility = View.GONE
        }
    }

    private fun submitReview(user: com.google.firebase.auth.FirebaseUser) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSubmitTime < SUBMIT_COOLDOWN_MS) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        val rating = rbInputRating?.rating ?: 0f
        val comment = etReviewComment?.text?.toString()?.trim() ?: ""

        if (rating == 0f) {
            Toast.makeText(this, "Select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitReview?.isEnabled = false
        pbSubmitReview?.visibility = View.VISIBLE

        val review = Review(user.uid, user.displayName ?: "Anonymous", user.photoUrl?.toString(), rating, comment).apply {
            placeId = this@PlaceDetailsActivity.placeId
            placeName = this@PlaceDetailsActivity.placeName
            placeImageUrl = this@PlaceDetailsActivity.currentPlace?.imageUrl
            reviewId = "${this@PlaceDetailsActivity.placeId}_${user.uid}"
        }

        android.util.Log.d("PlaceDetailsActivity", "Review submission started")
        reviewRepository.submitReview(placeId ?: "", review).addOnCompleteListener { task ->
            btnSubmitReview?.isEnabled = true
            pbSubmitReview?.visibility = View.GONE

            if (task.isSuccessful) {
                android.util.Log.d("PlaceDetailsActivity", "Review saved successfully")
                val isUpdate = btnSubmitReview?.text?.toString() == getString(R.string.update_review)
                val placeLabel = review.placeName ?: placeName ?: "destination"
                com.arriva.touristguideapp.profile.ProfileActivityTracker.log(
                    this,
                    if (isUpdate) {
                        com.arriva.touristguideapp.profile.ProfileActivityTracker.Action.REVIEW_EDITED
                    } else {
                        com.arriva.touristguideapp.profile.ProfileActivityTracker.Action.REVIEW_POSTED
                    },
                    placeLabel
                )
                lastSubmitTime = System.currentTimeMillis()
                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
                btnSubmitReview?.setText(R.string.update_review)
                
                // Add success notification
                try {
                    val notifTitle = if (isUpdate) "Review Edited" else "Review Submitted"
                    val notifMessage = if (isUpdate) {
                        "Your review for ${review.placeName ?: "Destination"} was updated successfully."
                    } else {
                        "Your review for ${review.placeName ?: "Destination"} was posted successfully."
                    }
                    val notif = com.arriva.touristguideapp.data.notifications.NotificationModel(
                        UUID.randomUUID().toString(),
                        notifTitle,
                        notifMessage,
                        com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_REVIEW,
                        System.currentTimeMillis()
                    ).apply {
                        userId = user.uid
                        isRead = false
                    }
                    com.arriva.touristguideapp.data.notifications.NotificationRepository(this).saveToHistory(notif)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                android.util.Log.e("PlaceDetailsActivity", "Review save failed: ${task.exception?.message}")
                Toast.makeText(this, "Failed to post review: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startReviewListener() {
        val id = placeId ?: return
        if (reviewsListener != null) return

        pbReviewsLoading?.visibility = View.VISIBLE
        btnRetryReviews?.visibility = View.GONE
        reviewsListener = reviewRepository.listenToReviews(id, currentReviewLimit) { reviews, exception, error ->
            pbReviewsLoading?.visibility = View.GONE
            isPaginationLoading = false
            
            if (exception != null || error != null) {
                btnRetryReviews?.visibility = View.VISIBLE
                return@listenToReviews
            }

            if (reviews.isEmpty() && currentReviewLimit == 10) {
                tvNoReviews?.visibility = View.VISIBLE
                rvReviews?.visibility = View.GONE
                hasMoreReviews = false
            } else {
                tvNoReviews?.visibility = View.GONE
                rvReviews?.visibility = View.VISIBLE
                hasMoreReviews = reviews.size >= currentReviewLimit
                reviewAdapter?.setReviews(reviews)
            }
        }
    }

    private fun startPlaceListener() {
        val id = placeId ?: return
        if (placeListener != null) return
        placeListener = reviewRepository.listenToPlace(id) { avg, total, comments ->
            updateRatingSummary(avg, total, comments)
        }
    }

    private fun stopListeners() {
        reviewsListener?.remove()
        reviewsListener = null
        placeListener?.remove()
        placeListener = null
    }

    private fun updateRatingSummary(avg: Double, total: Long, comments: Long) {
        tvRatingSummary?.text = if (total == 0L) "No reviews yet" else String.format(Locale.getDefault(), "⭐ %.1f (%d reviews)", avg, total)
    }

    private fun sharePlace() {
        val shareText = "Check out $placeName on Tourist Guide App!\nLocation: https://www.google.com/maps/search/?api=1&query=$lat,$lng"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, placeName)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun openDirections() {
        val uri = Uri.parse("google.navigation:q=$lat,$lng")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun makeCall() {
        Toast.makeText(this, "Call feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun setupExpandableDescription(description: String?) {
        val tvDesc = findViewById<TextView>(R.id.detailDescription)
        val btnReadMore = findViewById<TextView>(R.id.btnReadMore)
        
        if (description != null && description.length > 200) {
            tvDesc.maxLines = 4
            btnReadMore.visibility = View.VISIBLE
            btnReadMore.setOnClickListener {
                if (tvDesc.maxLines == 4) {
                    tvDesc.maxLines = Int.MAX_VALUE
                    btnReadMore.text = "Show Less"
                } else {
                    tvDesc.maxLines = 4
                    btnReadMore.text = "Read More"
                }
            }
        } else {
            btnReadMore.visibility = View.GONE
        }
    }

    private fun setupChips(bestTime: String?, crowd: String?, budget: String?) {
        findViewById<Chip>(R.id.chipBestTime).apply {
            if (bestTime != null) text = "Best: $bestTime" else visibility = View.GONE
        }
        findViewById<Chip>(R.id.chipCrowd).apply {
            if (crowd != null) text = "$crowd Crowd" else visibility = View.GONE
        }
        findViewById<Chip>(R.id.chipBudget).apply {
            if (budget != null) text = "Budget: $budget" else visibility = View.GONE
        }
    }

    private fun setupGallery(urls: List<String>) {
        if (urls.isEmpty()) return
        viewPagerGallery?.adapter = GalleryAdapter(urls)
        findViewById<RecyclerView>(R.id.rvGalleryPreview).apply {
            layoutManager = LinearLayoutManager(this@PlaceDetailsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = GalleryPreviewAdapter(urls) { viewPagerGallery?.setCurrentItem(it, true) }
        }
    }

    private fun trackVisit(imageUrl: String?, category: String?, avgRating: Double, totalRatings: Long, intent: Intent) {
        currentPlace = Place().apply {
            id = placeId
            name = placeName
            this.category = category
            this.imageUrl = imageUrl
            rating = avgRating
            this.totalRatings = totalRatings
            this.totalComments = intent.getLongExtra("totalComments", 0)
            budget = intent.getStringExtra("budget")
            latitude = lat
            longitude = lng
            city = intent.getStringExtra("city")
            tag = intent.getStringExtra("tag")
            description = intent.getStringExtra("description")
        }
        currentPlace?.let { com.arriva.touristguideapp.data.places.RecentlyViewedManager(this).addRecentlyViewed(it) }
    }

    private fun setupNearbyUI() {
        rvNearbyPlaces?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        // Radius options: 2, 5, 10 km
        val currentRadius = 5.0
        
        PlaceRepository().fetchPublishedPlaces { places, _, _ ->
            val nearby = LocationUtils.getNearbyPlaces(places.filter { it.id != placeId }, lat, lng, 10)
            // Filter by current radius (simplification: showing top 10 within 5km by default)
            val filteredNearby = nearby.filter { it.distance <= currentRadius }
            
            if (filteredNearby.isNotEmpty()) {
                llNearbyPlaces?.visibility = View.VISIBLE
                rvNearbyPlaces?.adapter = TopPickAdapter(filteredNearby) {
                    val intent = Intent(this, PlaceDetailsActivity::class.java)
                    PlaceIntentExtras.putPlaceDetails(intent, it)
                    startActivity(intent)
                }
            }
        }
    }

    private fun animateEntrance() {
        findViewById<View>(R.id.nsvPlaceDetails)?.apply {
            alpha = 0f
            translationY = 100f
            animate().alpha(1f).translationY(0f).setDuration(600).start()
        }
        findViewById<View>(R.id.btnFavoriteDetails)?.apply {
            scaleX = 0f
            scaleY = 0f
            animate().scaleX(1f).scaleY(1f).setDuration(400).setStartDelay(400).start()
        }
    }

    private fun mergeGalleryUrlExtras(a: ArrayList<String>?, b: ArrayList<String>?): ArrayList<String> {
        val set = LinkedHashSet<String>()
        a?.forEach { if (it.isNotBlank()) set.add(it.trim()) }
        b?.forEach { if (it.isNotBlank()) set.add(it.trim()) }
        return ArrayList(set)
    }

    override fun onStart() {
        super.onStart()
        startReviewListener()
        startPlaceListener()
    }

    override fun onStop() {
        super.onStop()
        stopListeners()
    }

    private fun deleteReview(review: Review) {
        AlertDialog.Builder(this)
            .setTitle("Delete Review")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                reviewRepository.deleteReview(placeId, review.userId).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Review deleted", Toast.LENGTH_SHORT).show()
                        try {
                            com.arriva.touristguideapp.data.notifications.NotificationRepository(this@PlaceDetailsActivity).addNotification(
                                "Review Deleted",
                                "Your review for ${review.placeName ?: "Destination"} was deleted successfully.",
                                com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_REVIEW
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        rbInputRating?.rating = 0f
                        etReviewComment?.setText("")
                        btnSubmitReview?.setText(R.string.submit_review)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReportDialog(review: Review) {
        val reasons = arrayOf("Spam", "Inappropriate content", "Hate speech", "Harassment", "Other")
        AlertDialog.Builder(this)
            .setTitle("Report Review")
            .setItems(reasons) { _, which -> reportReview(review, reasons[which]) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reportReview(review: Review, reason: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        reviewRepository.reportReview(placeId, review.userId, uid, reason).addOnCompleteListener {
            if (it.isSuccessful) Toast.makeText(this, "Review reported", Toast.LENGTH_SHORT).show()
        }
    }
}
