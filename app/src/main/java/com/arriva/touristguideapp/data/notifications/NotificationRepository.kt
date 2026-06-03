package com.arriva.touristguideapp.data.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class NotificationRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    private val gson = com.google.gson.Gson()

    companion object {
        const val TYPE_NEARBY = "nearby"
        const val TYPE_TRENDING = "trending"
        const val TYPE_REVIEWS = "reviews"
        const val TYPE_FAVORITES = "favorites"
        const val TYPE_ADMIN = "admin"
        const val TYPE_OFFLINE = "offline_reminders"
    }

    /**
     * Registers the FCM token for the current user.
     */
    fun registerToken(token: String) {
        prefs.edit().putString("fcm_token", token).apply()

        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener { Log.d("NotificationRepo", "FCM_TOKEN_REGISTERED") }
                .addOnFailureListener { e -> Log.e("NotificationRepo", "FAILED_TO_REGISTER_TOKEN", e) }
        }
    }

    /**
     * Saves a notification to local history.
     */
    fun saveToHistory(notification: NotificationModel) {
        val history = getHistory().toMutableList()
        history.add(0, notification)
        val finalHistory = if (history.size > 50) history.subList(0, 50) else history
        prefs.edit().putString("notification_history", gson.toJson(finalHistory)).apply()
    }

    /**
     * Retrieves the notification history.
     */
    fun getHistory(): List<NotificationModel> {
        val json = prefs.getString("notification_history", null) ?: return emptyList()
        val type = object : com.google.gson.reflect.TypeToken<List<NotificationModel>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Updates notification preference in Firestore and subscribes/unsubscribes to FCM topic.
     */
    suspend fun setPreference(type: String, enabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        
        // 1. Save to SharedPreferences for local caching
        prefs.edit().putBoolean("pref_$type", enabled).apply()

        // 2. Save to Firestore for cross-device persistence
        db.collection("users").document(uid).update("notifications.$type", enabled).await()

        // 3. Subscribe/Unsubscribe to FCM Topic
        if (enabled) {
            FirebaseMessaging.getInstance().subscribeToTopic(type).await()
            Log.d("NotificationRepo", "Subscribed to $type")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(type).await()
            Log.d("NotificationRepo", "Unsubscribed from $type")
        }
    }

    fun isEnabled(type: String): Boolean {
        return prefs.getBoolean("pref_$type", true)
    }

    /**
     * Logs when a notification is opened.
     */
    fun logNotificationOpened(type: String?) {
        Log.d("NotificationRepo", "NOTIFICATION_OPENED: $type")
        // In a real app, you might send this to analytics as well
    }

    /**
     * Loads preferences from Firestore and updates local state.
     */
    suspend fun syncPreferences() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val doc = db.collection("users").document(uid).get().await()
            val notifications = doc.get("notifications") as? Map<String, Boolean>
            notifications?.forEach { (type, enabled) ->
                prefs.edit().putBoolean("pref_$type", enabled).apply()
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Failed to sync preferences", e)
        }
    }
}
