package com.arriva.touristguideapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.data.repository.ProfileRepository
import com.arriva.touristguideapp.profile.ProfileActivityItem
import com.arriva.touristguideapp.profile.ProfileCompletionHelper
import com.arriva.touristguideapp.profile.ProfileRecentActivityAdapter
import com.arriva.touristguideapp.sos.ui.SOSSettingsActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var repository: ProfileRepository
    private lateinit var notificationRepository: com.arriva.touristguideapp.data.notifications.NotificationRepository

    private lateinit var ivProfileImage: ImageView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileUsername: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var cardCompleteProfileBanner: MaterialCardView
    private lateinit var tvBannerSubtitle: TextView
    private lateinit var recentActivityAdapter: ProfileRecentActivityAdapter
    private lateinit var tvSeeMoreActivity: TextView

    private var dashboardJob: Job? = null
    private var lastStats: Map<String, Long> = emptyMap()
    private var recentActivityItems: List<ProfileActivityItem> = emptyList()
    private var isRecentActivityExpanded = false
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    private companion object {
        const val DEFAULT_ACTIVITY_LIMIT = 10
        const val EXPANDED_ACTIVITY_LIMIT = 50
    }

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadUserData()
            refreshDashboard()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        repository = ProfileRepository(this)
        notificationRepository = com.arriva.touristguideapp.data.notifications.NotificationRepository(this)

        initViews()
        applyAnimations()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        refreshDashboard()
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val row = findViewById<View>(R.id.btnNotificationSettings)
        val badge = row.findViewById<TextView>(R.id.tvRowBadge)
        val unreadCount = notificationRepository.getUnreadCount()
        if (unreadCount > 0) {
            badge.text = unreadCount.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }

    private fun initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileUsername = findViewById(R.id.tvProfileUsername)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        cardCompleteProfileBanner = findViewById(R.id.cardCompleteProfileBanner)
        tvBannerSubtitle = findViewById(R.id.tvBannerSubtitle)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val rvRecentActivity = findViewById<RecyclerView>(R.id.rvRecentActivity)
        recentActivityAdapter = ProfileRecentActivityAdapter()
        rvRecentActivity.layoutManager = LinearLayoutManager(this)
        rvRecentActivity.adapter = recentActivityAdapter
        tvSeeMoreActivity = findViewById(R.id.tvSeeMoreActivity)
        tvSeeMoreActivity.setOnClickListener {
            isRecentActivityExpanded = true
            updateRecentActivityList()
        }

        findViewById<MaterialButton>(R.id.btnCompleteNow).setOnClickListener {
            openEditProfile()
        }

        setupStatNavigation()
        setupQuickAccessCards()
        setupQuickAccessRows()
        loadMemberInfo()

        ivProfileImage.setOnClickListener { openEditProfile() }
        findViewById<View>(R.id.cardProfileCompletion).setOnClickListener { openEditProfile() }
    }

    private fun openEditProfile() {
        editProfileLauncher.launch(Intent(this, EditProfileActivity::class.java))
    }

    private fun setupStatNavigation() {
        findViewById<View>(R.id.statTrips).setOnClickListener {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }
        findViewById<View>(R.id.statFavs).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
        findViewById<View>(R.id.statReviews).setOnClickListener {
            startActivity(Intent(this, MyReviewsActivity::class.java))
        }
        findViewById<View>(R.id.statNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationHistoryActivity::class.java))
        }
    }

    private fun setupQuickAccessCards() {
        bindQuickAccessCard(
            R.id.cardQuickTripHistory,
            R.drawable.ic_trip,
            getString(R.string.quick_access_trip_history)
        ) { startActivity(Intent(this, TripHistoryActivity::class.java)) }

        bindQuickAccessCard(
            R.id.cardQuickFavorites,
            R.drawable.ic_favorite,
            getString(R.string.quick_access_favorites)
        ) { startActivity(Intent(this, FavoritesActivity::class.java)) }

        bindQuickAccessCard(
            R.id.cardQuickReviews,
            R.drawable.ic_star,
            getString(R.string.quick_access_reviews)
        ) { startActivity(Intent(this, MyReviewsActivity::class.java)) }

        bindQuickAccessCard(
            R.id.cardQuickNotifications,
            R.drawable.ic_notification,
            getString(R.string.quick_access_notifications)
        ) { startActivity(Intent(this, NotificationHistoryActivity::class.java)) }
    }

    private fun bindQuickAccessCard(cardId: Int, iconRes: Int, title: String, onClick: () -> Unit) {
        val card = findViewById<View>(cardId)
        card.findViewById<ImageView>(R.id.ivQuickAccessIcon).setImageResource(iconRes)
        card.findViewById<TextView>(R.id.tvQuickAccessTitle).text = title
        card.setOnClickListener { onClick() }
    }

    private fun updateQuickAccessCounts(stats: Map<String, Long>) {
        updateQuickAccessCount(R.id.cardQuickTripHistory, stats["trips"] ?: 0)
        updateQuickAccessCount(R.id.cardQuickFavorites, stats["favorites"] ?: 0)
        updateQuickAccessCount(R.id.cardQuickReviews, stats["reviews"] ?: 0)
        updateQuickAccessCount(R.id.cardQuickNotifications, stats["notifications"] ?: 0)
    }

    private fun updateQuickAccessCount(cardId: Int, count: Long) {
        findViewById<View>(cardId).findViewById<TextView>(R.id.tvQuickAccessCount).text = count.toString()
    }

    private fun setupQuickAccessRows() {
        setupRow(findViewById(R.id.btnEditProfile), R.drawable.ic_account, getString(R.string.edit_profile), getString(R.string.edit_profile_desc)) {
            openEditProfile()
        }

        setupRow(findViewById(R.id.btnEditTravelInterests), R.drawable.ic_edit, getString(R.string.edit_travel_interests), getString(R.string.edit_travel_interests_desc)) {
            startActivity(
                Intent(this, InterestSelectionActivity::class.java)
                    .putExtra(InterestSelectionActivity.EXTRA_PROFILE_EDIT_MODE, true)
            )
        }

        setupRow(findViewById(R.id.btnMyReviews), R.drawable.ic_star, getString(R.string.my_reviews), getString(R.string.my_reviews_desc)) {
            startActivity(Intent(this, MyReviewsActivity::class.java))
        }

        setupRow(findViewById(R.id.btnTripHistory), R.drawable.ic_trip, getString(R.string.trip_history), getString(R.string.trip_history_desc)) {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }

        setupRow(findViewById(R.id.btnSecurity), R.drawable.ic_security, getString(R.string.security), getString(R.string.security_desc)) {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        setupRow(findViewById(R.id.btnNotificationSettings), R.drawable.ic_notification, getString(R.string.notifications), getString(R.string.notifications_desc)) {
            startActivity(Intent(this, NotificationHistoryActivity::class.java))
        }

        setupRow(findViewById(R.id.btnLanguage), R.drawable.ic_language, getString(R.string.language), getString(R.string.language_desc)) {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        setupRow(findViewById(R.id.btnSOSSettings), R.drawable.ic_sos, getString(R.string.profile_sos_settings), getString(R.string.sos_settings_desc)) {
            startActivity(Intent(this, SOSSettingsActivity::class.java))
        }

        setupRow(findViewById(R.id.btnSOSHistory), R.drawable.ic_history, getString(R.string.profile_sos_history), getString(R.string.sos_history_desc)) {
            startActivity(Intent(this, com.arriva.touristguideapp.sos.ui.SOSHistoryActivity::class.java))
        }

        setupRow(findViewById(R.id.btnLogoutRow), R.drawable.ic_logout, getString(R.string.sign_out), getString(R.string.sign_out_desc)) {
            val uid = auth.currentUser?.uid
            val deviceId = BaseActivity.getDeviceId(this)

            lifecycleScope.launch {
                if (uid != null) {
                    try {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .collection("devices")
                            .document(deviceId)
                            .delete()
                            .await()
                    } catch (e: Exception) {
                        // Ignore error
                    }
                }

                auth.signOut()
                repository.cacheUserProfile(User())
                getSharedPreferences("user_profile_cache", Context.MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences("favorites", Context.MODE_PRIVATE).edit().clear().apply()

                val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setupRow(row: View, icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
        row.findViewById<ImageView>(R.id.settingIcon).setImageResource(icon)
        row.findViewById<TextView>(R.id.settingTitle).text = title
        row.findViewById<TextView>(R.id.settingSubtitle).text = subtitle
        row.setOnClickListener { onClick() }
    }

    private fun loadMemberInfo() {
        val firebaseUser = auth.currentUser ?: return
        val creationTime = firebaseUser.metadata?.creationTimestamp
        val lastSignInTime = firebaseUser.metadata?.lastSignInTimestamp

        findViewById<TextView>(R.id.tvMemberSince).text =
            if (creationTime != null && creationTime > 0) {
                dateTimeFormat.format(Date(creationTime))
            } else {
                getString(R.string.date_not_available)
            }

        findViewById<TextView>(R.id.tvLastLogin).text =
            if (lastSignInTime != null && lastSignInTime > 0) {
                dateTimeFormat.format(Date(lastSignInTime))
            } else {
                getString(R.string.date_not_available)
            }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return

        val cached = repository.getCachedUserProfile()
        if (cached != null) {
            bindProfileHeader(cached, user.email)
            updateProfileCompletionUI(cached)
        } else {
            bindProfileHeader(null, user.email)
            updateProfileCompletionUI(buildUserFromAuth(user))
        }

        lifecycleScope.launch {
            val profile = repository.getUserProfile()
            val resolved = profile ?: buildUserFromAuth(user)
            bindProfileHeader(resolved, resolved.email ?: user.email)
            updateProfileCompletionUI(resolved)
        }
    }

    private fun buildUserFromAuth(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
        return User().apply {
            uid = firebaseUser.uid
            email = firebaseUser.email
            name = firebaseUser.displayName
            fullName = firebaseUser.displayName
            profileImage = firebaseUser.photoUrl?.toString()
        }
    }

    private fun bindProfileHeader(profile: User?, fallbackEmail: String?) {
        val displayName = profile?.fullName?.takeIf { it.isNotBlank() }
            ?: profile?.name?.takeIf { it.isNotBlank() }
            ?: getString(R.string.complete_your_profile)

        val username = profile?.username?.takeIf { it.isNotBlank() }
        val email = profile?.email?.takeIf { it.isNotBlank() }
            ?: fallbackEmail?.takeIf { it.isNotBlank() }
            ?: ""

        tvProfileName.text = displayName
        tvProfileUsername.text = if (username != null) "@$username" else ""
        tvProfileUsername.visibility = if (username != null) View.VISIBLE else View.GONE
        tvProfileEmail.text = email.ifBlank { getString(R.string.complete_your_profile) }

        if (profile != null && ProfileCompletionHelper.hasPhoto(profile)) {
            Glide.with(this)
                .load(profile.profileImage ?: profile.profilePhoto)
                .circleCrop()
                .placeholder(R.drawable.ic_account)
                .into(ivProfileImage)
        } else {
            val avatarName = if (displayName == getString(R.string.complete_your_profile)) "?" else displayName
            ivProfileImage.setImageDrawable(ProfileUtils.generateLetterAvatar(this, avatarName))
        }
    }

    private fun updateProfileCompletionUI(user: User) {
        val completion = ProfileCompletionHelper.calculateCompletion(user)
        findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.pbProfileCompletion).progress = completion
        findViewById<TextView>(R.id.tvCompletionPercentage).text =
            getString(R.string.profile_completion_format, completion)

        val isComplete = ProfileCompletionHelper.isProfileComplete(user)
        cardCompleteProfileBanner.visibility = if (isComplete) View.GONE else View.VISIBLE
        tvBannerSubtitle.text = getString(R.string.profile_banner_subtitle, completion)
    }

    private fun refreshDashboard() {
        dashboardJob?.cancel()
        dashboardJob = lifecycleScope.launch {
            try {
                val stats = repository.fetchStats()
                lastStats = stats
                updateStat(
                    R.id.statTrips,
                    stats["trips"] ?: 0,
                    getString(R.string.trips),
                    getString(R.string.no_trips_created)
                )
                updateStat(
                    R.id.statFavs,
                    stats["favorites"] ?: 0,
                    getString(R.string.saved_places),
                    getString(R.string.no_saved_places)
                )
                updateStat(
                    R.id.statReviews,
                    stats["reviews"] ?: 0,
                    getString(R.string.reviews),
                    getString(R.string.no_reviews_yet)
                )
                updateStat(
                    R.id.statNotifications,
                    stats["notifications"] ?: 0,
                    getString(R.string.notifications),
                    getString(R.string.no_notifications_yet)
                )
                updateQuickAccessCounts(stats)

                recentActivityItems = repository.getRecentActivity(EXPANDED_ACTIVITY_LIMIT)
                updateRecentActivityList()
            } catch (e: Exception) {
                if (lastStats.isNotEmpty()) {
                    updateQuickAccessCounts(lastStats)
                }
            }
        }
    }

    private fun updateRecentActivityList() {
        val displayLimit = if (isRecentActivityExpanded) {
            EXPANDED_ACTIVITY_LIMIT
        } else {
            DEFAULT_ACTIVITY_LIMIT
        }
        val displayedItems = recentActivityItems.take(displayLimit)
        recentActivityAdapter.submitList(displayedItems)

        val emptyView = findViewById<TextView>(R.id.tvRecentActivityEmpty)
        val listView = findViewById<RecyclerView>(R.id.rvRecentActivity)
        val isEmpty = displayedItems.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        listView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        tvSeeMoreActivity.visibility =
            if (!isRecentActivityExpanded && recentActivityItems.size > DEFAULT_ACTIVITY_LIMIT) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun updateStat(rowId: Int, value: Long, label: String, emptyMessage: String) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.tvStatValue).text = value.toString()
        row.findViewById<TextView>(R.id.tvStatLabel).text = label
        val emptyView = row.findViewById<TextView>(R.id.tvStatEmpty)
        if (value == 0L) {
            emptyView.text = emptyMessage
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    private fun applyAnimations() {
        findViewById<View>(R.id.cardProfileImage).apply {
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start()
        }
        tvProfileName.apply {
            translationY = 50f
            alpha = 0f
            animate().translationY(0f).alpha(1f).setDuration(600).setStartDelay(200).start()
        }
    }
}
