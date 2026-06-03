package com.arriva.touristguideapp

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

class NotificationHistoryActivity : AppCompatActivity() {

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
            setTitle("Notification History")
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
                Toast.makeText(this@NotificationHistoryActivity, "Notification deleted", Toast.LENGTH_SHORT).show()
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

        AlertDialog.Builder(this)
            .setTitle(notification.title)
            .setMessage(notification.message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun deleteNotification(notification: NotificationModel) {
        val position = history.indexOfFirst { it.id == notification.id }
        if (position != -1) {
            repository.deleteNotification(notification.id)
            history.removeAt(position)
            adapter.notifyItemRemoved(position)
            toggleEmptyState(history.isEmpty())
            Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "All marked as read", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.menu_clear_all -> {
                AlertDialog.Builder(this)
                    .setTitle("Clear All Notifications")
                    .setMessage("Are you sure you want to clear all notifications?")
                    .setPositiveButton("Clear") { _, _ ->
                        repository.clearAll()
                        loadNotifications()
                        Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
