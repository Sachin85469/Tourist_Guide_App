package com.arriva.touristguideapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.arriva.touristguideapp.data.notifications.NotificationAdapter;
import com.arriva.touristguideapp.data.notifications.NotificationModel;
import com.arriva.touristguideapp.data.notifications.NotificationRepository;
import java.util.List;

public class NotificationHistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_history);

        Toolbar toolbar = findViewById(R.id.notificationToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notification History");
        }

        RecyclerView rvHistory = findViewById(R.id.rvNotificationHistory);
        TextView tvNoNotifications = findViewById(R.id.tvNoNotifications);

        NotificationRepository repository = new NotificationRepository(this);
        List<NotificationModel> history = repository.getHistory();

        if (history.isEmpty()) {
            tvNoNotifications.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            tvNoNotifications.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            rvHistory.setAdapter(new NotificationAdapter(history));
        }
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
