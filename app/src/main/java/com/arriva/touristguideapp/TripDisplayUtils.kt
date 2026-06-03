package com.arriva.touristguideapp

import com.arriva.touristguideapp.data.trips.Trip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val tripDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
private val tripInputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
    isLenient = false
}

fun Trip.destinationLabel(): String {
    return destinationName.takeIf { it.isNotBlank() }
        ?: title.takeIf { it.isNotBlank() }
        ?: places.firstOrNull()?.city?.takeIf { it.isNotBlank() }
        ?: places.firstOrNull()?.name?.takeIf { it.isNotBlank() }
        ?: "Untitled Trip"
}

fun Trip.locationLabel(): String {
    return location.takeIf { it.isNotBlank() }
        ?: places.mapNotNull { it.city }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
            .takeIf { it.isNotBlank() }
        ?: "Location not set"
}

fun Trip.heroImageUrl(): String {
    return imageUrl.takeIf { it.isNotBlank() }
        ?: places.firstOrNull { !it.imageUrl.isNullOrBlank() }?.imageUrl
        ?: ""
}

fun Trip.dateRangeLabel(): String {
    val start = startDate?.let { tripDateFormat.format(it) }
    val end = endDate?.let { tripDateFormat.format(it) }
    return when {
        start != null && end != null -> "$start - $end"
        start != null -> "Starts $start"
        end != null -> "Ends $end"
        else -> "Travel dates not set"
    }
}

fun Trip.inputStartDateLabel(): String = startDate?.let { tripInputDateFormat.format(it) } ?: ""

fun Trip.inputEndDateLabel(): String = endDate?.let { tripInputDateFormat.format(it) } ?: ""

fun parseTripInputDate(value: String): Date? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    return tripInputDateFormat.parse(trimmed)
}

fun Trip.durationLabel(): String {
    val start = startDate ?: return "Duration not set"
    val end = endDate ?: return "Duration not set"
    val days = TimeUnit.MILLISECONDS.toDays(end.startOfDayMillis() - start.startOfDayMillis()) + 1
    if (days <= 0) return "Duration not set"
    return if (days == 1L) "1 day" else "$days days"
}

fun Trip.displayStatus(now: Date = Date()): String {
    return when (storedStatusKey()) {
        "cancelled" -> "Cancelled"
        "completed" -> "Completed"
        "active" -> "Ongoing"
        else -> statusFromDates(now)
    }
}

fun Trip.storedStatusKey(): String {
    return when (status.trim().lowercase(Locale.US)) {
        "cancelled" -> "cancelled"
        "completed" -> "completed"
        "ongoing", "active" -> "active"
        else -> "planned"
    }
}

fun statusLabelToStoredStatus(label: String): String {
    return when (label.trim().lowercase(Locale.US)) {
        "cancelled" -> "cancelled"
        "completed" -> "completed"
        "ongoing" -> "active"
        else -> "planned"
    }
}

fun deriveStoredStatus(startDate: Date?, endDate: Date?, selectedStatus: String): String {
    val selected = statusLabelToStoredStatus(selectedStatus)
    if (selected == "cancelled" || selected == "completed") return selected

    val today = Date().startOfDayMillis()
    val start = startDate?.startOfDayMillis()
    val end = endDate?.startOfDayMillis()

    return when {
        start != null && end != null && today > end -> "completed"
        start != null && end != null && today in start..end -> "active"
        start != null && today < start -> "planned"
        selected == "active" -> "active"
        else -> "planned"
    }
}

fun Trip.activitiesLabel(): String {
    if (activities.isEmpty()) return "No planned activities yet"
    return activities.joinToString("\n") { "- $it" }
}

fun Trip.savedPlacesLabel(): String {
    if (places.isEmpty()) return "No saved places"
    return places.joinToString("\n") { place ->
        val meta = listOfNotNull(
            place.city?.takeIf { it.isNotBlank() },
            place.category?.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
        if (meta.isBlank()) "- ${place.name}" else "- ${place.name} ($meta)"
    }
}

fun Trip.matchesTripQuery(query: String): Boolean {
    val normalized = query.trim().lowercase(Locale.getDefault())
    if (normalized.isEmpty()) return true

    val searchText = buildString {
        append(destinationLabel()).append(' ')
        append(title).append(' ')
        append(notes).append(' ')
        append(locationLabel()).append(' ')
        append(activities.joinToString(" ")).append(' ')
        places.forEach { place ->
            append(place.name).append(' ')
            append(place.city).append(' ')
            append(place.category).append(' ')
            append(place.description).append(' ')
        }
    }.lowercase(Locale.getDefault())

    return searchText.contains(normalized)
}

private fun Trip.statusFromDates(now: Date): String {
    val today = now.startOfDayMillis()
    val start = startDate?.startOfDayMillis()
    val end = endDate?.startOfDayMillis()

    return when {
        start != null && end != null && today > end -> "Completed"
        start != null && end != null && today in start..end -> "Ongoing"
        start != null && today < start -> "Upcoming"
        else -> "Upcoming"
    }
}

private fun Date.startOfDayMillis(): Long {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
