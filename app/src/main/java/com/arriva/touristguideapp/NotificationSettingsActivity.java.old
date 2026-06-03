package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Switch;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.arriva.touristguideapp.data.notifications.NotificationRepository;

public class NotificationSettingsActivity extends AppCompatActivity {

    private NotificationRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notification Settings");
        }

        repository = new NotificationRepository(this);

        setupSwitch(R.id.switchNearby, NotificationRepository.TYPE_NEARBY);
        setupSwitch(R.id.switchTrending, NotificationRepository.TYPE_TRENDING);
        setupSwitch(R.id.switchReviews, NotificationRepository.TYPE_REVIEWS);
        setupSwitch(R.id.switchFavorites, NotificationRepository.TYPE_FAVORITES);
        setupSwitch(R.id.switchAdmin, NotificationRepository.TYPE_ADMIN);
        setupSwitch(R.id.switchLocalReminders, "local_reminders");
    }

    private void setupSwitch(int resId, String type) {
        Switch sw = findViewById(resId);
        sw.setChecked(repository.isEnabled(type));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            repository.setPreference(type, isChecked);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
