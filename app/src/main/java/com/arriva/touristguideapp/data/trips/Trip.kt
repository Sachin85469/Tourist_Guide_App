package com.arriva.touristguideapp.data.trips

import com.arriva.touristguideapp.Place
import java.io.Serializable
import java.util.Date

data class Trip(
    var id: String = "",
    var userId: String = "",
    var title: String = "",
    var places: List<Place> = emptyList(),
    var startDate: Date? = null,
    var endDate: Date? = null,
    var status: String = "planned" // planned, active, completed
) : Serializable
