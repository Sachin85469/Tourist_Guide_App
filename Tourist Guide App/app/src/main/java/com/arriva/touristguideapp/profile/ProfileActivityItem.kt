package com.arriva.touristguideapp.profile

data class ProfileActivityItem(
    val description: String,
    val timestamp: Long,
    val type: Type = Type.OTHER
) {
    enum class Type {
        REVIEW,
        FAVORITE,
        TRIP,
        SOS,
        OTHER
    }
}
