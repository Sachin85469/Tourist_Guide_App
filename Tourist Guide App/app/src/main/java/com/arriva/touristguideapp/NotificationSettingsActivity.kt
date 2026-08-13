package com.arriva.touristguideapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.arriva.touristguideapp.data.notifications.NotificationRepository
import kotlinx.coroutines.launch

class NotificationSettingsActivity : BaseActivity() {

    private lateinit var repository: NotificationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        val toolbar = findViewById<Toolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.notification_settings_title)
        }

        repository = NotificationRepository(this)

        setupSwitch(R.id.switchNearby, NotificationRepository.TYPE_NEARBY)
        setupSwitch(R.id.switchTrending, NotificationRepository.TYPE_TRENDING)
        setupSwitch(R.id.switchReviews, NotificationRepository.TYPE_REVIEWS)
        setupSwitch(R.id.switchFavorites, NotificationRepository.TYPE_FAVORITES)
        setupSwitch(R.id.switchAdmin, NotificationRepository.TYPE_ADMIN)
        setupSwitch(R.id.switchLocalReminders, NotificationRepository.TYPE_OFFLINE)
    }

    private fun setupSwitch(resId: Int, type: String) {
        val sw = findViewById<SwitchCompat>(resId)
        sw.isChecked = repository.isEnabled(type)
        sw.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                try {
                    repository.setPreference(type, isChecked)
                } catch (e: Exception) {
                    Toast.makeText(this@NotificationSettingsActivity, getString(R.string.failed_to_save, e.message), Toast.LENGTH_SHORT).show()
                    sw.isChecked = !isChecked // Revert on failure
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
