package com.arriva.touristguideapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.arriva.touristguideapp.communication.CommunicationHubActivity;

/**
 * Legacy entry point — forwards to the multilingual {@link CommunicationHubActivity}.
 */
public class PhrasebookActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB = "initial_tab";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, CommunicationHubActivity.class);
        intent.putExtra(EXTRA_INITIAL_TAB, 0);
        startActivity(intent);
        finish();
    }
}
