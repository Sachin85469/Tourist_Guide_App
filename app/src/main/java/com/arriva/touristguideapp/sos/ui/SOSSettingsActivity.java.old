package com.arriva.touristguideapp.sos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.arriva.touristguideapp.R;
import com.arriva.touristguideapp.sos.manager.SOSPreferences;

public class SOSSettingsActivity extends AppCompatActivity {
    private SOSPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_settings);

        preferences = new SOSPreferences(this);

        EditText etContact = findViewById(R.id.et_emergency_contact);
        SwitchCompat swVolume = findViewById(R.id.sw_volume_trigger);
        SwitchCompat swFloating = findViewById(R.id.sw_floating_button);
        SwitchCompat swCall = findViewById(R.id.sw_call_after_sms);
        Button btnSave = findViewById(R.id.btn_save_settings);

        etContact.setText(preferences.getContactNumber());
        swVolume.setChecked(preferences.isVolumeTriggerEnabled());
        swFloating.setChecked(preferences.isFloatingButtonEnabled());
        swCall.setChecked(preferences.isCallAfterSMS());

        btnSave.setOnClickListener(v -> {
            preferences.setContactNumber(etContact.getText().toString());
            preferences.setVolumeTriggerEnabled(swVolume.isChecked());
            preferences.setFloatingButtonEnabled(swFloating.isChecked());
            preferences.setCallAfterSMS(swCall.isChecked());

            if (swFloating.isChecked()) {
                try {
                    Intent serviceIntent = new Intent(this, com.arriva.touristguideapp.sos.service.FloatingSOSService.class);
                    androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
                } catch (Exception e) {
                    com.arriva.touristguideapp.sos.utils.Logger.e("Failed to start floating service from settings", e);
                }
            } else {
                stopService(new Intent(this, com.arriva.touristguideapp.sos.service.FloatingSOSService.class));
            }

            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
        });
    }
}
