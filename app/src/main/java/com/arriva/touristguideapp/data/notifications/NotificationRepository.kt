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

    private fun getHistoryKey(): String {
        val uid = auth.currentUser?.uid ?: "guest"
        return "notification_history_$uid"
    }

    /**
     * Saves a notification to local history.
     */
    fun saveToHistory(notification: NotificationModel) {
        val history = getHistory().toMutableList()
        // Ensure the notification is associated with the current user
        notification.userId = auth.currentUser?.uid ?: ""
        
        // Remove duplicate if same ID exists (e.g. on update/replace)
        history.removeAll { it.id == notification.id }
        
        history.add(0, notification)
        val finalHistory = if (history.size > 100) history.subList(0, 100) else history
        prefs.edit().putString(getHistoryKey(), gson.toJson(finalHistory)).apply()
    }

    /**
     * Retrieves the notification history. Generates a default welcome system notification if empty.
     */
    fun getHistory(): List<NotificationModel> {
        val key = getHistoryKey()
        val json = prefs.getString(key, null)
        if (json == null) {
            // Generate a default system notification on first launch for the user
            val welcomeNotif = NotificationModel(
                java.util.UUID.randomUUID().toString(),
                "Welcome to Tourist Guide App!",
                "Start exploring Pune's top picks, read and write reviews, plan your perfect trip, or configure SOS contacts.",
                NotificationModel.TYPE_SYSTEM,
                System.currentTimeMillis()
            ).apply {
                userId = auth.currentUser?.uid ?: ""
                isRead = false
            }
            val initialList = listOf(welcomeNotif)
            prefs.edit().putString(key, gson.toJson(initialList)).apply()
            return initialList
        }
        val type = object : com.google.gson.reflect.TypeToken<List<NotificationModel>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getNotifications(): List<NotificationModel> {
        return getHistory()
    }

    fun addNotification(title: String, message: String, type: String): NotificationModel {
        val notif = NotificationModel(
            java.util.UUID.randomUUID().toString(),
            title,
            message,
            type,
            System.currentTimeMillis()
        ).apply {
            userId = auth.currentUser?.uid ?: ""
            isRead = false
        }
        saveToHistory(notif)
        return notif
    }

    fun deleteNotification(id: String) {
        val history = getHistory().toMutableList()
        if (history.removeAll { it.id == id }) {
            prefs.edit().putString(getHistoryKey(), gson.toJson(history)).apply()
        }
    }

    fun markAsRead(id: String) {
        val history = getHistory().toMutableList()
        val notif = history.find { it.id == id }
        if (notif != null && !notif.isRead) {
            notif.isRead = true
            prefs.edit().putString(getHistoryKey(), gson.toJson(history)).apply()
        }
    }

    fun markAllAsRead() {
        val history = getHistory().toMutableList()
        var updated = false
        for (notif in history) {
            if (!notif.isRead) {
                notif.isRead = true
                updated = true
            }
        }
        if (updated) {
            prefs.edit().putString(getHistoryKey(), gson.toJson(history)).apply()
        }
    }

    fun clearAll() {
        prefs.edit().remove(getHistoryKey()).apply()
    }

    fun getUnreadCount(): Int {
        return getHistory().count { !it.isRead }
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
