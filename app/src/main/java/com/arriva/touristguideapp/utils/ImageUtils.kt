package com.arriva.touristguideapp.utils

import android.widget.ImageView
import com.arriva.touristguideapp.Place
import com.arriva.touristguideapp.R
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

/** Loads place images from Firebase Storage references produced by the uploader. */
object ImageUtils {

    private const val GS_PREFIX = "gs://"

    @JvmField
    val LOADING_PLACEHOLDER: Int = R.drawable.shimmer_placeholder

    @JvmField
    val DEFAULT_TRAVEL_IMAGE: Int = R.drawable.default_travel_image

    @JvmStatic
    fun loadDrawable(imageView: ImageView, resId: Int) {
        val imageResId = if (resId == 0) DEFAULT_TRAVEL_IMAGE else resId

        Glide.with(imageView.context)
            .load(imageResId)
            .centerCrop()
            .placeholder(LOADING_PLACEHOLDER)
            .error(DEFAULT_TRAVEL_IMAGE)
            .into(imageView)
    }

    @JvmStatic
    fun loadPlaceMainImage(imageView: ImageView, place: Place) {
        loadImageReference(imageView, place.imageRef)
    }

    /**
     * Resolves a Firebase Storage path/reference at display time. Catalog documents store this
     * stable reference rather than a hand-entered or expiring download link.
     */
    @JvmStatic
    fun loadImageReference(imageView: ImageView, imageRef: String?) {
        val normalizedRef = imageRef?.trim().orEmpty()
        Glide.with(imageView.context).clear(imageView)

        if (normalizedRef.isEmpty()) {
            imageView.setTag(R.id.tag_place_image_reference, null)
            imageView.setImageResource(DEFAULT_TRAVEL_IMAGE)
            return
        }

        imageView.setTag(R.id.tag_place_image_reference, normalizedRef)
        imageView.setImageResource(LOADING_PLACEHOLDER)
        storageReference(normalizedRef)
            ?.downloadUrl
            ?.addOnSuccessListener { downloadUri ->
                if (imageView.getTag(R.id.tag_place_image_reference) != normalizedRef) return@addOnSuccessListener
                Glide.with(imageView.context)
                    .load(downloadUri)
                    .centerCrop()
                    .placeholder(LOADING_PLACEHOLDER)
                    .error(DEFAULT_TRAVEL_IMAGE)
                    .into(imageView)
            }
            ?.addOnFailureListener {
                if (imageView.getTag(R.id.tag_place_image_reference) == normalizedRef) {
                    imageView.setImageResource(DEFAULT_TRAVEL_IMAGE)
                }
            }
            ?: imageView.setImageResource(DEFAULT_TRAVEL_IMAGE)
    }

    @JvmStatic
    fun clear(imageView: ImageView) {
        imageView.setTag(R.id.tag_place_image_reference, null)
        Glide.with(imageView.context).clear(imageView)
    }

    private fun storageReference(imageRef: String): StorageReference? = try {
        val storage = FirebaseStorage.getInstance()
        if (imageRef.startsWith(GS_PREFIX, ignoreCase = true)) {
            storage.getReferenceFromUrl(imageRef)
        } else {
            storage.reference.child(imageRef.removePrefix("/"))
        }
    } catch (_: Exception) {
        null
    }
}
