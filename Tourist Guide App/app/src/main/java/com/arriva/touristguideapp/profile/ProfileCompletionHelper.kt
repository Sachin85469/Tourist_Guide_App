package com.arriva.touristguideapp.profile

import com.arriva.touristguideapp.User

object ProfileCompletionHelper {

    const val TOTAL_FIELDS = 8

    fun completedFieldCount(user: User): Int {
        var count = 0
        if (hasPhoto(user)) count++
        if (hasName(user)) count++
        if (!user.email.isNullOrBlank()) count++
        if (!user.phoneNumber.isNullOrBlank()) count++
        if (!user.dateOfBirth.isNullOrBlank()) count++
        if (!user.city.isNullOrBlank()) count++
        if (!user.bio.isNullOrBlank()) count++
        if (hasTravelPreference(user)) count++
        return count
    }

    /** Percentage from completed fields: 0/8 = 0%, 4/8 = 50%, 8/8 = 100%. */
    fun calculateCompletion(user: User): Int {
        val completed = completedFieldCount(user)
        return ((completed * 100) / TOTAL_FIELDS).coerceIn(0, 100)
    }

    fun isProfileComplete(user: User): Boolean = completedFieldCount(user) >= TOTAL_FIELDS

    fun hasPhoto(user: User): Boolean =
        !user.profileImage.isNullOrBlank() || !user.profilePhoto.isNullOrBlank()

    fun hasName(user: User): Boolean =
        !user.fullName.isNullOrBlank() || !user.name.isNullOrBlank()

    private fun hasTravelPreference(user: User): Boolean =
        !user.favoriteTravelCategory.isNullOrBlank() ||
            (user.travelInterests != null && user.travelInterests.isNotEmpty())
}
