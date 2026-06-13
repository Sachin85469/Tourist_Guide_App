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
import com.arriva.touristguideapp.profile.ProfileActivityItem
import com.arriva.touristguideapp.profile.ProfileActivityTracker
import com.arriva.touristguideapp.sos.manager.SOSPreferences
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val profileCachePrefs = context.getSharedPreferences("user_profile_cache", Context.MODE_PRIVATE)
    private val gson = com.google.gson.Gson()

    fun getCachedUserProfile(): User? {
        val json = profileCachePrefs.getString("profile_data", null)
        return if (json != null) {
            gson.fromJson(json, User::class.java)
        } else {
            null
        }
    }

    fun cacheUserProfile(user: User) {
        profileCachePrefs.edit().putString("profile_data", gson.toJson(user)).apply()
    }

    /**
     * Updates user profile in Firestore.
     */
    suspend fun updateProfile(updates: Map<String, Any>) {
        val uid = getUid() ?: throw Exception("User not authenticated")
        db.collection("users").document(uid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
        
        // Refresh local cache
        val doc = db.collection("users").document(uid).get().await()
        val remoteUser = doc.toObject(User::class.java)
        if (remoteUser != null) {
            cacheUserProfile(remoteUser)
        }
    }

    /**
     * Updates user profile with complete object.
     */
    suspend fun saveUserProfile(user: User) {
        val uid = getUid() ?: throw Exception("User not authenticated")
        user.uid = uid
        db.collection("users").document(uid).set(user, com.google.firebase.firestore.SetOptions.merge()).await()
        cacheUserProfile(user)
    }

    /**
     * Fetches user data from Firestore.
     */
    suspend fun getUserProfile(): User? {
        val uid = getUid() ?: return null
        val cached = getCachedUserProfile()
        try {
            val doc = db.collection("users").document(uid).get().await()
            val remoteUser = doc.toObject(User::class.java)
            if (remoteUser != null) {
                cacheUserProfile(remoteUser)
                return remoteUser
            }
        } catch (e: Exception) {
            if (cached != null) return cached
        }
        return cached
    }

    /**
     * Gets statistics for the profile screen (one-shot flow emission).
     */
    fun getStats(): Flow<Map<String, Long>> = flow {
        emit(fetchStats())
    }

    /**
     * Loads live dashboard counts from Firestore (and local notification cache).
     */
    suspend fun fetchStats(): Map<String, Long> = withContext(Dispatchers.IO) {
        val uid = getUid() ?: return@withContext emptyMap()
        val stats = mutableMapOf<String, Long>()

        var favorites: Long = 0
        try {
            val querySnapshot = db.collection("favorites")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            favorites = querySnapshot.size().toLong()

            val editor = favPrefs.edit()
            val allEntries = favPrefs.all
            for ((key, value) in allEntries) {
                if (value is Boolean) {
                    editor.remove(key)
                }
            }
            for (doc in querySnapshot.documents) {
                val destId = doc.getString("destinationId")
                if (destId != null) {
                    editor.putBoolean(destId, true)
                }
            }
            editor.apply()
        } catch (e: Exception) {
            favorites = favPrefs.all.filter { it.value is Boolean && it.value as Boolean }.size.toLong()
        }
        stats["favorites"] = favorites

        try {
            val reviewsQuery = db.collection("reviews")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            stats["reviews"] = reviewsQuery.size().toLong()
        } catch (e: Exception) {
            stats["reviews"] = 0L
        }

        try {
            val tripsQuery = db.collection("trips")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            stats["trips"] = tripsQuery.size().toLong()
        } catch (e: Exception) {
            stats["trips"] = 0L
        }

        stats["notifications"] = com.arriva.touristguideapp.data.notifications.NotificationRepository(context)
            .getNotifications()
            .size
            .toLong()

        stats
    }

    /**
     * Recent activity from the local action tracker (newest first), with Firestore fallback if empty.
     */
    suspend fun getRecentActivity(limit: Int = 10): List<ProfileActivityItem> = withContext(Dispatchers.IO) {
        val tracked = ProfileActivityTracker.getRecentEvents(context, limit)
        if (tracked.isNotEmpty()) {
            return@withContext tracked
        }

        val uid = getUid() ?: return@withContext emptyList()
        val items = mutableListOf<ProfileActivityItem>()

        try {
            val reviews = db.collection("reviews")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in reviews.documents) {
                val placeName = doc.getString("placeName")
                    ?: doc.getString("destinationName")
                    ?: "a destination"
                val timestamp = reviewTimestamp(doc)
                items.add(
                    ProfileActivityItem(
                        description = "Reviewed $placeName",
                        timestamp = timestamp,
                        type = ProfileActivityItem.Type.REVIEW
                    )
                )
            }
        } catch (e: Exception) {
            // Skip reviews on query failure
        }

        try {
            val favorites = db.collection("favorites")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in favorites.documents) {
                val name = doc.getString("destinationName") ?: "a destination"
                val savedAt = doc.getLong("savedAt") ?: 0L
                items.add(
                    ProfileActivityItem(
                        description = "Saved $name",
                        timestamp = savedAt,
                        type = ProfileActivityItem.Type.FAVORITE
                    )
                )
            }
        } catch (e: Exception) {
            // Skip favorites on query failure
        }

        try {
            val trips = db.collection("trips")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            for (doc in trips.documents) {
                val tripName = doc.getString("title")
                    ?: doc.getString("destinationName")
                    ?: doc.getString("location")
                    ?: "Trip"
                val timestamp = tripTimestamp(doc)
                items.add(
                    ProfileActivityItem(
                        description = "Created $tripName Trip",
                        timestamp = timestamp,
                        type = ProfileActivityItem.Type.TRIP
                    )
                )
            }
        } catch (e: Exception) {
            // Skip trips on query failure
        }

        val sosPrefs = SOSPreferences(context)
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        for (event in sosPrefs.sosEvents) {
            var timestamp = 0L
            val date = event.date
            val time = event.time
            if (!date.isNullOrBlank() && !time.isNullOrBlank()) {
                timestamp = dateTimeFormat.parse("$date $time")?.time ?: 0L
            } else if (!date.isNullOrBlank()) {
                timestamp = dateOnlyFormat.parse(date)?.time ?: 0L
            }
            items.add(
                ProfileActivityItem(
                    description = "Activated SOS",
                    timestamp = timestamp,
                    type = ProfileActivityItem.Type.SOS
                )
            )
        }

        items
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    private fun reviewTimestamp(doc: com.google.firebase.firestore.DocumentSnapshot): Long {
        val ts = doc.getTimestamp("createdAt")
        if (ts != null) return ts.toDate().time
        val date = doc.getDate("createdAt")
        if (date != null) return date.time
        val updated = doc.getTimestamp("updatedAt")
        if (updated != null) return updated.toDate().time
        return doc.getDate("updatedAt")?.time ?: 0L
    }

    private fun tripTimestamp(doc: com.google.firebase.firestore.DocumentSnapshot): Long {
        val created = doc.getTimestamp("createdAt")
        if (created != null) return created.toDate().time
        val date = doc.getDate("createdAt")
        if (date != null) return date.time
        val updated = doc.getTimestamp("updatedAt")
        if (updated != null) return updated.toDate().time
        return doc.getDate("updatedAt")?.time ?: 0L
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
        return com.arriva.touristguideapp.FavoritesManager.isFavorite(context, placeId)
    }

    /**
     * Toggles favorite status.
     */
    fun toggleFavorite(place: Place) {
        com.arriva.touristguideapp.FavoritesManager.toggleFavorite(context, place)
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
        try {
            val favoritesQuery = db.collection("favorites").whereEqualTo("userId", uid).get().await()
            for (doc in favoritesQuery) {
                doc.reference.delete()
            }
        } catch (e: Exception) {
            // Ignore if fails
        }

        // Delete trips
        try {
            val trips = db.collection("trips").whereEqualTo("userId", uid).get().await()
            for (doc in trips) {
                doc.reference.delete()
            }
        } catch (e: Exception) {
            // Ignore if trips deletion fails
        }

        // Delete user document
        db.collection("users").document(uid).delete().await()

        // Clear local caches
        favPrefs.edit().clear().apply()
        profileCachePrefs.edit().clear().apply()

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
