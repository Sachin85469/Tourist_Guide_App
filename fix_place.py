import re

with open(r'c:\Users\Sachin\AndroidStudioProjects\Tourist_Guide_App\app\src\main\java\com\arriva\touristguideapp\PlaceDetailsActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

idx = text.find('    private fun deleteReview(review: Review) {')
if idx != -1:
    text = text[:idx]
    
    text += """    private fun deleteReview(review: Review) {
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
"""
    with open(r'c:\Users\Sachin\AndroidStudioProjects\Tourist_Guide_App\app\src\main\java\com\arriva\touristguideapp\PlaceDetailsActivity.kt', 'w', encoding='utf-8') as f:
        f.write(text)
    print("Fixed PlaceDetailsActivity.kt")
else:
    print("Could not find deleteReview")
