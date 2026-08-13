package com.arriva.touristguideapp.data.trips

import com.arriva.touristguideapp.Place
import java.io.Serializable
import java.util.Date

data class Trip(
    var id: String = "",
    var userId: String = "",
    var title: String = "",
    var destinationName: String = "",
    var location: String = "",
    var places: List<Place> = emptyList(),
    var startDate: Date? = null,
    var endDate: Date? = null,
    var status: String = "planned", // planned, active, completed, cancelled
    var notes: String = "",
    var activities: List<String> = emptyList(),
    var budget: String = "",
    var createdAt: Date? = null,
    var updatedAt: Date? = null
) : Serializable
