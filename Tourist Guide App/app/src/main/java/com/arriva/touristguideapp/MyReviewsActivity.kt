package com.arriva.touristguideapp

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.data.reviews.ReviewRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MyReviewsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var adapter: MyReviewsAdapter

    private lateinit var etSearch: EditText
    private lateinit var cgFilters: ChipGroup
    private lateinit var pbLoading: ProgressBar
    private lateinit var llEmptyState: View
    private lateinit var rvReviews: RecyclerView
    private lateinit var btnExplore: Button

    private val allReviews = mutableListOf<Review>()
    private var searchQuery = ""
    private var ratingFilter = -1 // -1 means no filter, otherwise 1-5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reviews)

        auth = FirebaseAuth.getInstance()
        reviewRepository = ReviewRepository()

        initViews()
        setupListeners()
        loadUserReviews()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearchReviews)
        cgFilters = findViewById(R.id.cgStarFilters)
        pbLoading = findViewById(R.id.pbReviewsLoading)
        llEmptyState = findViewById(R.id.llEmptyState)
        rvReviews = findViewById(R.id.rvUserReviews)
        btnExplore = findViewById(R.id.btnExploreDestinations)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        rvReviews.layoutManager = LinearLayoutManager(this)
        adapter = MyReviewsAdapter(
            reviews = emptyList(),
            onReviewClick = { showReviewDetails(it) },
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { confirmDelete(it) }
        )
        rvReviews.adapter = adapter
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cgFilters.setOnCheckedChangeListener { _, checkedId ->
            ratingFilter = when (checkedId) {
                R.id.chip5Star -> 5
                R.id.chip4Star -> 4
                R.id.chip3Star -> 3
                R.id.chip2Star -> 2
                R.id.chip1Star -> 1
                else -> -1
            }
            applyFilters()
        }

        btnExplore.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserReviews() {
        val uid = auth.currentUser?.uid ?: return
        pbLoading.visibility = View.VISIBLE
        rvReviews.visibility = View.GONE
        llEmptyState.visibility = View.GONE

        val collectionName = "reviews"
        val queryParams = "collection=$collectionName, filter=(userId == $uid)"
        android.util.Log.d("MyReviewsActivity", "Review Fetch Started. Parameters: $queryParams")

        try {
            reviewRepository.fetchUserReviews(uid) { reviews, exception, error ->
                pbLoading.visibility = View.GONE
                if (exception != null || error != null) {
                    val errorCode = (exception as? com.google.firebase.firestore.FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
                    val errorMessage = exception?.message ?: error ?: "Unknown error"
                    android.util.Log.e("MyReviewsActivity", "Review Fetch Failed. Code: $errorCode, Message: $errorMessage, Parameters: $queryParams", exception)
                    Toast.makeText(this, "Failed to load reviews: $errorMessage", Toast.LENGTH_SHORT).show()
                    toggleEmptyState(true)
                    return@fetchUserReviews
                }

                android.util.Log.d("MyReviewsActivity", "Review Fetch Success. Loaded count: ${reviews.size}")
                allReviews.clear()
                allReviews.addAll(reviews.sortedByDescending { it.createdAt ?: Date(0) })
                applyFilters()
            }
        } catch (e: Exception) {
            pbLoading.visibility = View.GONE
            val errorCode = (e as? com.google.firebase.firestore.FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
            android.util.Log.e("MyReviewsActivity", "Review Fetch Block Failed with Exception. Code: $errorCode, Message: ${e.message}, Params: $queryParams", e)
            Toast.makeText(this, "Failed to load reviews: ${e.message}", Toast.LENGTH_SHORT).show()
            toggleEmptyState(true)
        }
    }

    private fun applyFilters() {
        val filteredList = allReviews.filter { review ->
            // Apply star rating filter
            val matchesStar = (ratingFilter == -1) || (review.rating.toInt() == ratingFilter)

            // Apply search query filter (destination name or comment content)
            val placeNameMatch = review.placeName?.contains(searchQuery, ignoreCase = true) == true
            val commentMatch = review.comment?.contains(searchQuery, ignoreCase = true) == true
            val matchesSearch = searchQuery.isEmpty() || placeNameMatch || commentMatch

            matchesStar && matchesSearch
        }

        adapter.updateReviews(filteredList)
        toggleEmptyState(filteredList.isEmpty())
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            llEmptyState.visibility = View.VISIBLE
            rvReviews.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            rvReviews.visibility = View.VISIBLE
        }
    }

    private fun showReviewDetails(review: Review) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_review_details)
        dialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvPlaceName: TextView = dialog.findViewById(R.id.tvDialogPlaceName)
        val rbStars: RatingBar = dialog.findViewById(R.id.rbDialogStars)
        val tvComment: TextView = dialog.findViewById(R.id.tvDialogComment)
        val tvDate: TextView = dialog.findViewById(R.id.tvDialogDate)
        val btnClose: Button = dialog.findViewById(R.id.btnDialogClose)

        tvPlaceName.text = review.placeName ?: "Unknown Destination"
        rbStars.rating = review.rating
        tvComment.text = review.comment ?: ""

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        tvDate.text = "Submitted on: " + (review.createdAt?.let { dateFormat.format(it) } ?: "N/A")

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showEditDialog(review: Review) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_review)
        dialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle: TextView = dialog.findViewById(R.id.tvEditReviewTitle)
        val rbStars: RatingBar = dialog.findViewById(R.id.rbEditReviewStars)
        val etComment: EditText = dialog.findViewById(R.id.etEditReviewComment)
        val btnCancel: Button = dialog.findViewById(R.id.btnEditReviewCancel)
        val btnSave: Button = dialog.findViewById(R.id.btnEditReviewSave)
        val pbSaving: ProgressBar = dialog.findViewById(R.id.pbEditReviewSaving)

        tvTitle.text = "Edit Review for " + (review.placeName ?: "Destination")
        rbStars.rating = review.rating
        etComment.setText(review.comment ?: "")

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newRating = rbStars.rating
            val newComment = etComment.text.toString().trim()

            if (newRating == 0f) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnCancel.isEnabled = false
            pbSaving.visibility = View.VISIBLE

            // Update local review fields
            review.rating = newRating
            review.comment = newComment
            review.updatedAt = Date()

            reviewRepository.updateReview(review.placeId, review).addOnCompleteListener { task ->
                pbSaving.visibility = View.GONE
                btnSave.isEnabled = true
                btnCancel.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Review updated successfully", Toast.LENGTH_SHORT).show()
                    com.arriva.touristguideapp.profile.ProfileActivityTracker.log(
                        this@MyReviewsActivity,
                        com.arriva.touristguideapp.profile.ProfileActivityTracker.Action.REVIEW_EDITED,
                        review.placeName ?: "destination"
                    )
                    try {
                        com.arriva.touristguideapp.data.notifications.NotificationRepository(this@MyReviewsActivity).addNotification(
                            "Review Updated",
                            "Your review for ${review.placeName ?: "Destination"} was updated successfully.",
                            com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_REVIEW
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    dialog.dismiss()
                    loadUserReviews() // Reload the list
                } else {
                    Toast.makeText(this, "Failed to update review: " + task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun confirmDelete(review: Review) {
        AlertDialog.Builder(this)
            .setTitle("Delete Review")
            .setMessage("Delete this review?")
            .setPositiveButton("Delete") { _, _ ->
                pbLoading.visibility = View.VISIBLE
                val uid = auth.currentUser?.uid ?: return@setPositiveButton
                reviewRepository.deleteReview(review.placeId, uid).addOnCompleteListener { task ->
                    pbLoading.visibility = View.GONE
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Review deleted successfully", Toast.LENGTH_SHORT).show()
                        try {
                            com.arriva.touristguideapp.data.notifications.NotificationRepository(this@MyReviewsActivity).addNotification(
                                "Review Deleted",
                                "Your review for ${review.placeName ?: "Destination"} was deleted successfully.",
                                com.arriva.touristguideapp.data.notifications.NotificationModel.TYPE_REVIEW
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        loadUserReviews() // Reload list to refresh
                    } else {
                        Toast.makeText(this, "Failed to delete review: " + task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
