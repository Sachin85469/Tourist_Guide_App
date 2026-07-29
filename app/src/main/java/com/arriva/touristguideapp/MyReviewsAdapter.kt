package com.arriva.touristguideapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

class MyReviewsAdapter(
    private var reviews: List<Review>,
    private val onReviewClick: (Review) -> Unit,
    private val onEditClick: (Review) -> Unit,
    private val onDeleteClick: (Review) -> Unit
) : RecyclerView.Adapter<MyReviewsAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun updateReviews(newReviews: List<Review>) {
        this.reviews = newReviews
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPlaceImage: ImageView = itemView.findViewById(R.id.ivReviewPlaceImage)
        private val tvPlaceName: TextView = itemView.findViewById(R.id.tvReviewPlaceName)
        private val rbStars: RatingBar = itemView.findViewById(R.id.rbUserReviewStars)
        private val tvComment: TextView = itemView.findViewById(R.id.tvUserReviewComment)
        private val tvDate: TextView = itemView.findViewById(R.id.tvUserReviewDate)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEditUserReview)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteUserReview)

        fun bind(review: Review) {
            tvPlaceName.text = review.placeName ?: "Unknown Destination"
            rbStars.rating = review.rating
            tvComment.text = review.comment ?: ""
            
            if (review.createdAt != null) {
                tvDate.text = "Posted on ${dateFormat.format(review.createdAt)}"
            } else {
                tvDate.text = ""
            }

            // Reviews do not persist a place image reference; use the standard travel fallback.
            ImageUtils.loadImageReference(ivPlaceImage, null)

            itemView.setOnClickListener { onReviewClick(review) }
            btnEdit.setOnClickListener { onEditClick(review) }
            btnDelete.setOnClickListener { onDeleteClick(review) }
        }
    }
}
