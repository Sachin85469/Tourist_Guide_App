package com.arriva.touristguideapp.profile

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persists profile dashboard activity events per user (newest first).
 */
object ProfileActivityTracker {

    private const val PREF_NAME = "profile_activity_tracker"
    private const val MAX_EVENTS = 50
    private val gson = Gson()

    enum class Action {
        DESTINATION_SAVED,
        DESTINATION_REMOVED,
        TRIP_CREATED,
        TRIP_UPDATED,
        TRIP_DELETED,
        REVIEW_POSTED,
        REVIEW_EDITED,
        SOS_ACTIVATED
    }

    private data class StoredEvent(
        val action: String,
        val description: String,
        val timestamp: Long
    )

    @JvmStatic
    fun log(context: Context, action: Action, targetName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val description = formatDescription(action, targetName)
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = eventsKey(uid)
        val events = loadRaw(prefs, key).toMutableList()
        events.add(0, StoredEvent(action.name, description, System.currentTimeMillis()))
        val trimmed = if (events.size > MAX_EVENTS) events.subList(0, MAX_EVENTS) else events
        prefs.edit().putString(key, gson.toJson(trimmed)).apply()
    }

    @JvmStatic
    fun getRecentEvents(context: Context, limit: Int = 15): List<ProfileActivityItem> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return loadRaw(prefs, eventsKey(uid))
            .map { stored ->
                ProfileActivityItem(
                    description = stored.description,
                    timestamp = stored.timestamp,
                    type = mapType(stored.action)
                )
            }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }

    private fun eventsKey(uid: String) = "activity_events_$uid"

    private fun loadRaw(prefs: android.content.SharedPreferences, key: String): List<StoredEvent> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<StoredEvent>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun formatDescription(action: Action, name: String): String {
        val label = name.ifBlank { "destination" }
        return when (action) {
            Action.DESTINATION_SAVED -> "Saved $label"
            Action.DESTINATION_REMOVED -> "Removed $label"
            Action.TRIP_CREATED -> "Created $label Trip"
            Action.TRIP_UPDATED -> "Updated $label Trip"
            Action.TRIP_DELETED -> "Deleted $label Trip"
            Action.REVIEW_POSTED -> "Posted review for $label"
            Action.REVIEW_EDITED -> "Edited review for $label"
            Action.SOS_ACTIVATED -> "Activated SOS"
        }
    }

    private fun mapType(action: String): ProfileActivityItem.Type = when (action) {
        ProfileActivityTracker.Action.DESTINATION_SAVED.name,
        ProfileActivityTracker.Action.DESTINATION_REMOVED.name -> ProfileActivityItem.Type.FAVORITE
        ProfileActivityTracker.Action.TRIP_CREATED.name,
        ProfileActivityTracker.Action.TRIP_UPDATED.name,
        ProfileActivityTracker.Action.TRIP_DELETED.name -> ProfileActivityItem.Type.TRIP
        ProfileActivityTracker.Action.REVIEW_POSTED.name,
        ProfileActivityTracker.Action.REVIEW_EDITED.name -> ProfileActivityItem.Type.REVIEW
        ProfileActivityTracker.Action.SOS_ACTIVATED.name -> ProfileActivityItem.Type.SOS
        else -> ProfileActivityItem.Type.OTHER
    }
}
