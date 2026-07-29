package com.arriva.touristguideapp

import android.content.Intent
import android.content.res.ColorStateList
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
import com.arriva.touristguideapp.utils.ImageUtils
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
    private var tvNoReviews: View? = null
    private var tvRatingSummary: TextView? = null
    private var tvLargeRating: TextView? = null
    private var tvLargeStars: TextView? = null
    private var tvReviewCount: TextView? = null
    private var tvRatingFeedback: TextView? = null
    private var tvSubmitReviewLabel: TextView? = null
    private var tvCoordinates: TextView? = null
    private var pbReviewsLoading: ProgressBar? = null
    private var btnRetryReviews: Button? = null
    private var rvNearbyPlaces: RecyclerView? = null
    private var llNearbyPlaces: View? = null
    private var offlineCacheBanner: View? = null
    private var nsvPlaceDetails: androidx.core.widget.NestedScrollView? = null
    private var cvAddReview: View? = null
    private var rbInputRating: RatingBar? = null
    private var etReviewComment: EditText? = null
    private var btnSubmitReview: View? = null
    private var pbSubmitReview: ProgressBar? = null

    private var lastSubmitTime: Long = 0
    private val SUBMIT_COOLDOWN_MS: Long = 10000
    private var offlineCacheBannerDismissed = false

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
        tvLargeRating = findViewById(R.id.tvLargeRating)
        tvLargeStars = findViewById(R.id.tvLargeStars)
        tvReviewCount = findViewById(R.id.tvReviewCount)
        tvRatingFeedback = findViewById(R.id.tvRatingFeedback)
        tvSubmitReviewLabel = findViewById(R.id.tvSubmitReviewLabel)
        tvCoordinates = findViewById(R.id.tvCoordinates)
        rbInputRating = findViewById(R.id.rbInputRating)
        etReviewComment = findViewById(R.id.etReviewComment)
        btnSubmitReview = findViewById(R.id.btnSubmitReview)
        pbSubmitReview = findViewById(R.id.pbSubmitReview)
        llNearbyPlaces = findViewById(R.id.llNearbyPlaces)
        rvNearbyPlaces = findViewById(R.id.rvNearbyPlaces)
        offlineCacheBanner = findViewById(R.id.offlineCacheBanner)
        findViewById<View>(R.id.btnDismissOfflineBanner)?.setOnClickListener {
            offlineCacheBannerDismissed = true
            showOfflineCacheBanner(false)
        }

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
        val imageRef = intent.getStringExtra("imageRef")
        val galleryImageRefs = intent.getStringArrayListExtra("galleryImageRefs") ?: arrayListOf()

        // Show content hide loading
        findViewById<View>(R.id.loadingState).visibility = View.GONE
        findViewById<View>(R.id.emptyState).visibility = View.GONE

        // Basic Info
        findViewById<TextView>(R.id.detailName).text = placeName
        findViewById<TextView>(R.id.detailCategory).text = category ?: "Destination"
        findViewById<TextView>(R.id.detailDescription).text = description ?: "Destination details will be updated soon."
        
        // Bullet point tips
        val formattedTips = if (tips.isNullOrBlank()) {
            "• Explore and enjoy!"
        } else {
            tips.split("\n", ",").filter { it.isNotBlank() }.joinToString("\n") { "• ${it.trim()}" }
        }
        findViewById<TextView>(R.id.detailTips).text = formattedTips
        
        findViewById<TextView>(R.id.detailFunFact).text = funFact ?: "Discover something new!"
        findViewById<TextView>(R.id.detailStation).text = "Nearest: ${station ?: "City Center"}"
        
        // Map Coordinates
        tvCoordinates?.text = String.format(Locale.getDefault(), "%.4f° N, %.4f° E", lat, lng)

        val cleanTips = if (tips.isNullOrBlank()) {
            "- Explore at a relaxed pace."
        } else {
            tips.split("\n", ",").filter { it.isNotBlank() }.joinToString("\n") { "- ${it.trim()}" }
        }
        val funFactText = funFact ?: "A memorable stop for travelers exploring Maharashtra."
        val stationText = "Nearest: ${station ?: "City Center"}"
        findViewById<TextView>(R.id.detailTips).text = cleanTips
        findViewById<TextView>(R.id.tvTipsDisplay).text = cleanTips
        findViewById<TextView>(R.id.detailFunFact).text = funFactText
        findViewById<TextView>(R.id.tvFunFactDisplay).text = funFactText
        findViewById<TextView>(R.id.detailStation).text = stationText
        tvCoordinates?.text = String.format(Locale.getDefault(), "%.4f N, %.4f E", lat, lng)

        setupExpandableDescription(description)
        setupChips(bestTime, crowdLevel, budget)
        setupQuickFacts(budget, avgRating, totalRatings)
        setupInfoCards(bestTime, crowdLevel, budget, stationText)
        
        // The hero always contains one item: a Storage reference or the default travel image.
        val imageRefs = LinkedHashSet<String>()
        imageRef?.trim()?.takeIf { it.isNotEmpty() }?.let(imageRefs::add)
        galleryImageRefs.map { it.trim() }.filter { it.isNotEmpty() }.forEach(imageRefs::add)
        val galleryRefs = imageRefs.toList()
        val heroList = if (galleryRefs.isEmpty()) listOf("") else galleryRefs
        viewPagerGallery?.adapter = GalleryAdapter(heroList)

        setupGallery(galleryRefs)
        
        setupActionButtons()

        checkStatus()
        setupReviewUI()
        updateRatingSummary(avgRating, totalRatings, totalComments)
        trackVisit(category, avgRating, totalRatings, intent)
        
        AnalyticsRepository().trackPlaceView(placeId)

        setupNearbyUI()
        animateEntrance()
    }

    private fun loadPlaceDetails(id: String) {
        findViewById<View>(R.id.loadingState).visibility = View.VISIBLE
        // Try remote
        PlaceRepository(this).getPlacesOfflineFirst(null, null) { places, _, _, _ ->
            val place = places.find { it.id == id }
            if (place != null) {
                val intent = Intent()
                PlaceIntentExtras.putPlaceDetails(intent, place)
                placeName = place.name
                initUiWithIntent(intent)
            } else {
                showEmptyState()
            }
        }
    }

    private fun showEmptyState() {
        findViewById<View>(R.id.loadingState).visibility = View.GONE
        findViewById<View>(R.id.emptyState).visibility = View.VISIBLE
        findViewById<View>(R.id.btnRetry).setOnClickListener {
            placeId?.let { loadPlaceDetails(it) }
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
        // btnSave is now a MaterialCardView with an ImageView inside — update the icon
        val saveIcon = findViewById<ImageView?>(R.id.ivSaveIcon)
        saveIcon?.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        saveIcon?.imageTintList = ColorStateList.valueOf(
            if (isFav) android.graphics.Color.WHITE else getColor(R.color.color_primary)
        )

        findViewById<View?>(R.id.btnSave)?.apply {
            backgroundTintList = ColorStateList.valueOf(
                if (isFav) getColor(R.color.color_primary) else getColor(R.color.place_detail_soft_purple)
            )
            elevation = if (isFav) resources.displayMetrics.density * 4f else 0f
        }
    }

    private fun setupActionButtons() {
        findViewById<View>(R.id.btnDirections).setOnClickListener {
            animateTap(it)
            openDirections()
        }
        findViewById<View>(R.id.btnQuickShare)?.setOnClickListener {
            animateTap(it)
            sharePlace()
        }
        findViewById<View>(R.id.btnCall).setOnClickListener {
            animateTap(it)
            makeCall()
        }
        findViewById<View>(R.id.btnSave).setOnClickListener {
            animateTap(it)
            toggleFavorite()
        }
        findViewById<View>(R.id.btnExploreMap).setOnClickListener {
            animateTap(it)
            openDirections()
        }
        findViewById<View>(R.id.btnNavigateNow)?.setOnClickListener {
            animateNavigateIcon()
            openDirections()
        }

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
        rvReviews?.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        reviewAdapter = ReviewAdapter().apply {
            setOnReviewEditListener { editReview(it) }
            setOnReviewDeleteListener { deleteReview(it) }
            setOnReviewReportListener { showReportDialog(it) }
        }
        rvReviews?.adapter = reviewAdapter

        rbInputRating?.setOnRatingBarChangeListener { _, rating, fromUser ->
            updateRatingFeedback(rating)
            if (fromUser) {
                tvRatingFeedback?.alpha = 0f
                tvRatingFeedback?.animate()?.alpha(1f)?.setDuration(180)?.start()
            }
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            cvAddReview?.visibility = View.VISIBLE
            reviewRepository.getUserReview(placeId, currentUser.uid) { review, _ ->
                if (review != null) {
                    rbInputRating?.rating = review.rating
                    etReviewComment?.setText(review.comment)
                    tvSubmitReviewLabel?.setText(R.string.update_review)
                    updateRatingFeedback(review.rating)
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
            reviewId = "${this@PlaceDetailsActivity.placeId}_${user.uid}"
        }

        android.util.Log.d("PlaceDetailsActivity", "Review submission started")
        reviewRepository.submitReview(placeId ?: "", review).addOnCompleteListener { task ->
            btnSubmitReview?.isEnabled = true
            pbSubmitReview?.visibility = View.GONE

            if (task.isSuccessful) {
                android.util.Log.d("PlaceDetailsActivity", "Review saved successfully")
                val isUpdate = tvSubmitReviewLabel?.text?.toString() == getString(R.string.update_review)
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
                tvSubmitReviewLabel?.setText(R.string.update_review)
                btnSubmitReview?.scaleX = 0.96f
                btnSubmitReview?.scaleY = 0.96f
                btnSubmitReview?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(180)?.start()
                
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
        val summary = if (total == 0L) "No reviews yet" else String.format(Locale.getDefault(), "⭐ %.1f (%d reviews)", avg, total)
        tvRatingSummary?.text = summary

        if (total == 0L) {
            tvLargeRating?.text = "New"
            tvLargeStars?.visibility = View.GONE
            return
        }

        tvLargeStars?.visibility = View.VISIBLE
        
        tvLargeRating?.text = String.format(Locale.getDefault(), "%.1f", if (avg > 0) avg else 0.0)
        
        val stars = StringBuilder()
        val fullStars = avg.toInt()
        for (i in 1..5) {
            if (i <= fullStars) stars.append("★") else stars.append("☆")
        }
        tvLargeStars?.text = stars.toString()
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
        val intent = Intent(this, RouteActivity::class.java).apply {
            putExtra("destLat", lat)
            putExtra("destLng", lng)
            putExtra("destName", placeName)
        }
        startActivity(intent)
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
            if (bestTime != null) text = "Best Time: $bestTime" else visibility = View.GONE
        }
        findViewById<Chip>(R.id.chipCrowd).apply {
            if (crowd != null) text = "Crowd Level: $crowd" else visibility = View.GONE
        }
        findViewById<Chip>(R.id.chipBudget).apply {
            if (budget != null) text = "Budget: $budget" else visibility = View.GONE
        }
    }

    private fun setupGallery(imageRefs: List<String>) {
        if (imageRefs.isNotEmpty()) {
            findViewById<View>(R.id.llGalleryContent)?.visibility = View.VISIBLE
            findViewById<View>(R.id.tvNoGallery)?.visibility = View.GONE
        } else {
            findViewById<View>(R.id.llGalleryContent)?.visibility = View.GONE
            findViewById<TextView>(R.id.tvNoGallery)?.apply {
                visibility = View.VISIBLE
                text = "No gallery images available yet."
            }
        }

        val imageIds = intArrayOf(
            R.id.ivGalleryGrid1,
            R.id.ivGalleryGrid2,
            R.id.ivGalleryGrid3,
            R.id.ivGalleryGrid4
        )
        val cardIds = intArrayOf(
            R.id.galleryCard1,
            R.id.galleryCard2,
            R.id.galleryCard3,
            R.id.galleryCard4
        )

        for (i in imageIds.indices) {
            bindGalleryTile(cardIds[i], imageIds[i], imageRefs.getOrNull(i), i, imageRefs.size)
        }

        findViewById<View>(R.id.btnViewAllPhotos)?.setOnClickListener {
            if (imageRefs.isNotEmpty()) {
                viewPagerGallery?.setCurrentItem(0, true)
                nsvPlaceDetails?.smoothScrollTo(0, 0)
            }
        }
    }

    private fun trackVisit(category: String?, avgRating: Double, totalRatings: Long, intent: Intent) {
        currentPlace = Place().apply {
            id = placeId
            name = placeName
            this.category = category
            rating = avgRating
            this.totalRatings = totalRatings
            this.totalComments = intent.getLongExtra("totalComments", 0)
            budget = intent.getStringExtra("budget")
            latitude = lat
            longitude = lng
            city = intent.getStringExtra("city")
            tag = intent.getStringExtra("tag")
            description = intent.getStringExtra("description")
            imageRef = intent.getStringExtra("imageRef")
            galleryImageRefs = intent.getStringArrayListExtra("galleryImageRefs") ?: arrayListOf()
        }
        currentPlace?.let { com.arriva.touristguideapp.data.places.RecentlyViewedManager(this).addRecentlyViewed(it) }
    }

    private fun setupNearbyUI() {
        rvNearbyPlaces?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        // Radius options: 2, 5, 10 km
        val currentRadius = 5.0
        
        PlaceRepository(this).getPlacesOfflineFirst(null, null) { places, origin, cacheEmpty, _ ->
            val nearby = LocationUtils.getNearbyPlaces(places.filter { it.id != placeId }, lat, lng, 10)
            // Filter by current radius (simplification: showing top 10 within 5km by default)
            val filteredNearby = nearby.filter { it.distance <= currentRadius }
            
            if (filteredNearby.isNotEmpty()) {
                llNearbyPlaces?.visibility = View.VISIBLE
                showOfflineCacheBanner(origin == PlaceRepository.DataOrigin.ROOM_CACHE && !cacheEmpty)
                rvNearbyPlaces?.adapter = TopPickAdapter(filteredNearby) {
                    val intent = Intent(this, PlaceDetailsActivity::class.java)
                    PlaceIntentExtras.putPlaceDetails(intent, it)
                    startActivity(intent)
                }
            } else {
                showOfflineCacheBanner(false)
            }
        }
    }

    private fun showOfflineCacheBanner(show: Boolean) {
        offlineCacheBanner?.visibility = if (show && !offlineCacheBannerDismissed) View.VISIBLE else View.GONE
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
        findViewById<LinearLayout?>(R.id.placeContentRoot)?.let { root ->
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                child.alpha = 0f
                child.translationY = 28f
                child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay((80L + i * 45L).coerceAtMost(520L))
                    .setDuration(360L)
                    .start()
            }
        }
        animateNavigateIcon()
    }

    private fun bindGalleryTile(
        cardId: Int,
        imageId: Int,
        imageRef: String?,
        position: Int,
        imageCount: Int
    ) {
        val image = findViewById<ImageView?>(imageId) ?: return
        if (imageRef.isNullOrBlank()) {
            findViewById<View?>(cardId)?.visibility = View.GONE
        } else {
            findViewById<View?>(cardId)?.visibility = View.VISIBLE
            ImageUtils.loadImageReference(image, imageRef)
            image.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        findViewById<View?>(cardId)?.setOnClickListener { card ->
            animateTap(card)
            if (imageCount > 0) {
                viewPagerGallery?.setCurrentItem(position.coerceAtMost(imageCount - 1), true)
                nsvPlaceDetails?.smoothScrollTo(0, 0)
            }
        }
    }

    private fun animateTap(view: View?) {
        view ?: return
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(90L)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(130L).start()
            }
            .start()
    }

    private fun animateNavigateIcon() {
        // Implementation for navigation icon animation if needed
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
                        tvSubmitReviewLabel?.setText(R.string.submit_review)
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

    private fun editReview(review: Review) {
        rbInputRating?.rating = review.rating
        etReviewComment?.setText(review.comment)
        tvSubmitReviewLabel?.setText(R.string.update_review)
        updateRatingFeedback(review.rating)
        cvAddReview?.let {
            nsvPlaceDetails?.smoothScrollTo(0, it.top)
        }
    }

    private fun updateRatingFeedback(rating: Float) {
        val feedback = when {
            rating >= 4.5f -> "Excellent!"
            rating >= 3.5f -> "Good"
            rating >= 2.5f -> "Average"
            rating >= 1.5f -> "Poor"
            rating > 0f -> "Terrible"
            else -> getString(R.string.place_rating_feedback_default)
        }
        tvRatingFeedback?.text = feedback
    }

    private fun setupQuickFacts(budget: String?, avgRating: Double, totalRatings: Long) {
        // Implementation for quick facts
    }

    private fun setupInfoCards(bestTime: String?, crowdLevel: String?, budget: String?, stationText: String) {
        // Implementation for info cards
    }
}
