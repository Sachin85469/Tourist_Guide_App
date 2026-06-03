package com.arriva.touristguideapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arriva.touristguideapp.data.notifications.NotificationAdapter
import com.arriva.touristguideapp.data.notifications.NotificationModel
import com.arriva.touristguideapp.data.notifications.NotificationRepository

class NotificationHistoryActivity : BaseActivity() {

    private lateinit var repository: NotificationRepository
    private lateinit var adapter: NotificationAdapter
    private lateinit var rvHistory: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private val history = mutableListOf<NotificationModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_history)

        val toolbar = findViewById<Toolbar>(R.id.notificationToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.notification_history_title)
        }

        rvHistory = findViewById(R.id.rvNotificationHistory)
        llEmptyState = findViewById(R.id.llEmptyState)

        repository = NotificationRepository(this)
        
        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(
            notifications = history,
            onItemClick = { showNotificationDetailsDialog(it) },
            onDeleteClick = { deleteNotification(it) }
        )
        rvHistory.adapter = adapter

        // Swipe-to-delete implementation
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val notification = history[position]
                
                // Delete from repo
                repository.deleteNotification(notification.id)
                
                // Remove from list and update adapter
                history.removeAt(position)
                adapter.notifyItemRemoved(position)
                
                toggleEmptyState(history.isEmpty())
                Toast.makeText(this@NotificationHistoryActivity, R.string.notification_deleted, Toast.LENGTH_SHORT).show()
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(rvHistory)
    }

    private fun loadNotifications() {
        val list = repository.getNotifications()
        history.clear()
        history.addAll(list)
        adapter.notifyDataSetChanged()
        toggleEmptyState(history.isEmpty())
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            llEmptyState.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
        }
    }

    private fun showNotificationDetailsDialog(notification: NotificationModel) {
        // Mark as read in repository
        repository.markAsRead(notification.id)

        // Update local status and refresh
        notification.isRead = true
        adapter.notifyDataSetChanged()

        // Handle deep linking based on notification type
        handleNotificationClick(notification)
    }

    private fun handleNotificationClick(notification: NotificationModel) {
        when (notification.type) {
            NotificationModel.TYPE_TRIP -> {
                // Navigate to Trip Details
                val tripId = notification.dataId
                if (!tripId.isNullOrEmpty()) {
                    val intent = Intent(this, TripDetailsActivity::class.java)
                    intent.putExtra(TripDetailsActivity.EXTRA_TRIP_ID, tripId)
                    startActivity(intent)
                } else {
                    // If no specific trip ID, go to trip history
                    startActivity(Intent(this, TripHistoryActivity::class.java))
                }
            }
            NotificationModel.TYPE_FAVORITE -> {
                // Navigate to Destination Details
                val placeId = notification.dataId
                if (!placeId.isNullOrEmpty()) {
                    // Try to find the place in the catalog
                    com.arriva.touristguideapp.data.places.PlaceRepository().fetchPublishedPlaces { places, _, _ ->
                        val place = places.find { it.id == placeId }
                        if (place != null) {
                            val intent = Intent(this, PlaceDetailsActivity::class.java)
                            PlaceIntentExtras.putPlaceDetails(intent, place)
                            startActivity(intent)
                        } else {
                            // Fallback to favorites
                            startActivity(Intent(this, FavoritesActivity::class.java))
                        }
                    }
                } else {
                    startActivity(Intent(this, FavoritesActivity::class.java))
                }
            }
            NotificationModel.TYPE_REVIEW -> {
                // Navigate to My Reviews
                startActivity(Intent(this, MyReviewsActivity::class.java))
            }
            NotificationModel.TYPE_SOS -> {
                // Navigate to SOS History
                startActivity(Intent(this, com.arriva.touristguideapp.sos.ui.SOSHistoryActivity::class.java))
            }
            NotificationModel.TYPE_SYSTEM -> {
                // For system notifications (like profile updates), go to profile
                if (notification.title.contains("Profile", ignoreCase = true)) {
                    startActivity(Intent(this, ProfileActivity::class.java))
                } else {
                    // Show dialog for other system notifications
                    AlertDialog.Builder(this)
                        .setTitle(notification.title)
                        .setMessage(notification.message)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                    return
                }
            }
            else -> {
                // Default: show dialog
                AlertDialog.Builder(this)
                    .setTitle(notification.title)
                    .setMessage(notification.message)
                    .setPositiveButton(R.string.ok, null)
                    .show()
                return
            }
        }
        finish()
    }

    private fun deleteNotification(notification: NotificationModel) {
        val position = history.indexOfFirst { it.id == notification.id }
        if (position != -1) {
            repository.deleteNotification(notification.id)
            history.removeAt(position)
            adapter.notifyItemRemoved(position)
            toggleEmptyState(history.isEmpty())
            Toast.makeText(this, R.string.notification_deleted, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_notification_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_mark_all_read -> {
                repository.markAllAsRead()
                loadNotifications()
                Toast.makeText(this, R.string.all_marked_read, Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.menu_clear_all -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.clear_all_notifications)
                    .setMessage(R.string.clear_all_confirm)
                    .setPositiveButton(R.string.clear) { _, _ ->
                        repository.clearAll()
                        loadNotifications()
                        Toast.makeText(this, R.string.notifications_cleared, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
