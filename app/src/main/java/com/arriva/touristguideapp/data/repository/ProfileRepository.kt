package com.arriva.touristguideapp.data.repository

import android.content.Context
import android.net.Uri
import com.arriva.touristguideapp.Place
import com.arriva.touristguideapp.Review
import com.arriva.touristguideapp.User
import com.arriva.touristguideapp.data.places.PlaceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val favPrefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun getUid(): String? = auth.currentUser?.uid

    /**
     * Uploads profile image to Firebase Storage and returns the download URL.
     */
    suspend fun uploadProfileImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val uid = getUid() ?: throw Exception("User not authenticated")
        val ref = storage.reference.child("profile_images/$uid.jpg")
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    /**
     * Updates user profile in Firestore.
     */
    suspend fun updateProfile(updates: Map<String, Any>) {
        val uid = getUid() ?: throw Exception("User not authenticated")
        db.collection("users").document(uid).update(updates).await()
    }

    /**
     * Fetches user data from Firestore.
     */
    suspend fun getUserProfile(): User? {
        val uid = getUid() ?: return null
        return db.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    /**
     * Gets statistics for the profile screen.
     */
    fun getStats(): Flow<Map<String, Long>> = flow {
        val uid = getUid() ?: return@flow
        val stats = mutableMapOf<String, Long>()

        // 1. Saved Places (Favorites) count
        val favorites = favPrefs.all.filter { it.value is Boolean && it.value as Boolean }.size.toLong()
        stats["favorites"] = favorites

        // 2. Reviews count
        val reviewsQuery = db.collection("reviews")
            .whereEqualTo("userId", uid)
            .get()
            .await()
        stats["reviews"] = reviewsQuery.size().toLong()

        // 3. Trips count
        val tripsQuery = db.collection("trips")
            .whereEqualTo("userId", uid)
            .get()
            .await()
        stats["trips"] = tripsQuery.size().toLong()

        emit(stats)
    }

    /**
     * Tracks a place view in the user's history/analytics.
     */
    suspend fun trackPlaceView(placeId: String) {
        val uid = getUid() ?: return
        val historyRef = db.collection("users").document(uid).collection("history").document(placeId)
        val data = mapOf(
            "placeId" to placeId,
            "viewedAt" to FieldValue.serverTimestamp()
        )
        historyRef.set(data).await()
    }

    /**
     * Checks if a place is favorited.
     */
    fun isFavorite(placeId: String): Boolean {
        return favPrefs.getBoolean(placeId, false)
    }

    /**
     * Toggles favorite status.
     */
    fun toggleFavorite(place: Place) {
        val current = isFavorite(place.id)
        favPrefs.edit().putBoolean(place.id, !current).apply()
        
        // Sync with Firestore if user logged in
        val uid = getUid() ?: return
        val favRef = db.collection("users").document(uid).collection("favorites").document(place.id)
        if (!current) {
            favRef.set(place).addOnFailureListener { /* Handle sync failure */ }
        } else {
            favRef.delete()
        }
    }

    /**
     * Deletes user account and all associated data.
     */
    suspend fun deleteAccount() {
        val user = auth.currentUser ?: throw Exception("User not authenticated")
        val uid = user.uid

        // 1. Delete Firestore Data
        // Delete reviews
        val reviews = db.collection("reviews").whereEqualTo("userId", uid).get().await()
        for (doc in reviews) {
            doc.reference.delete()
        }

        // Delete favorites
        val favorites = db.collection("users").document(uid).collection("favorites").get().await()
        for (doc in favorites) {
            doc.reference.delete()
        }

        // Delete user document
        db.collection("users").document(uid).delete().await()

        // 2. Delete Profile Image
        try {
            storage.reference.child("profile_images/$uid.jpg").delete().await()
        } catch (e: Exception) {
            // Ignore if image doesn't exist
        }

        // 3. Delete Auth Account
        user.delete().await()
    }
}
