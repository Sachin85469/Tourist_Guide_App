package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

public class AdminDashboardActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private FloatingActionButton fabAddPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Toolbar toolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Admin Control Center");
        }

        tabLayout = findViewById(R.id.adminTabs);
        fabAddPlace = findViewById(R.id.fabAddPlace);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        fabAddPlace.show();
                        getSupportFragmentManager().beginTransaction()
                            .replace(R.id.adminContainer, new ManagePlacesFragment())
                            .commit();
                        break;
                    case 1:
                        fabAddPlace.hide();
                        getSupportFragmentManager().beginTransaction()
                            .replace(R.id.adminContainer, new ReviewModerationFragment())
                            .commit();
                        break;
                    case 2:
                        fabAddPlace.hide();
                        getSupportFragmentManager().beginTransaction()
                            .replace(R.id.adminContainer, new BulkUploadFragment())
                            .commit();
                        break;
                    case 3:
                        fabAddPlace.hide();
                        getSupportFragmentManager().beginTransaction()
                            .replace(R.id.adminContainer, new AdminAnalyticsFragment())
                            .commit();
                        break;
                    case 4:
                        fabAddPlace.hide();
                        getSupportFragmentManager().beginTransaction()
                            .replace(R.id.adminContainer, new AdminBroadcastFragment())
                            .commit();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabAddPlace.setOnClickListener(v -> {
            startActivity(new Intent(this, PlaceEditorActivity.class));
        });

        // Load default tab (Places)
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.adminContainer, new ManagePlacesFragment())
            .commit();
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
