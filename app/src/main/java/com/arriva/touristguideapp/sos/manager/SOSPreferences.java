package com.arriva.touristguideapp.sos.manager;

import android.content.Context;
import android.content.SharedPreferences;

public class SOSPreferences {
    private static final String PREF_NAME = "sos_prefs";
    private static final String KEY_CONTACT_NUMBER = "contact_number";
    private static final String KEY_VOLUME_TRIGGER_ENABLED = "volume_trigger_enabled";
    private static final String KEY_FLOATING_BUTTON_ENABLED = "floating_button_enabled";
    private static final String KEY_CALL_AFTER_SMS = "call_after_sms";
    private static final String KEY_SOS_LOGS = "sos_logs";

    private final SharedPreferences prefs;

    public SOSPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setContactNumber(String number) {
        prefs.edit().putString(KEY_CONTACT_NUMBER, number).apply();
    }

    public String getContactNumber() {
        return prefs.getString(KEY_CONTACT_NUMBER, "");
    }

    public void setVolumeTriggerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOLUME_TRIGGER_ENABLED, enabled).apply();
    }

    public boolean isVolumeTriggerEnabled() {
        return prefs.getBoolean(KEY_VOLUME_TRIGGER_ENABLED, true);
    }

    public void setFloatingButtonEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FLOATING_BUTTON_ENABLED, enabled).apply();
    }

    public boolean isFloatingButtonEnabled() {
        return prefs.getBoolean(KEY_FLOATING_BUTTON_ENABLED, true);
    }

    public void setCallAfterSMS(boolean enabled) {
        prefs.edit().putBoolean(KEY_CALL_AFTER_SMS, enabled).apply();
    }

    public boolean isCallAfterSMS() {
        return prefs.getBoolean(KEY_CALL_AFTER_SMS, true);
    }

    public void addLog(String log) {
        String existing = prefs.getString(KEY_SOS_LOGS, "");
        prefs.edit().putString(KEY_SOS_LOGS, log + "\n" + existing).apply();
    }

    public String getLogs() {
        return prefs.getString(KEY_SOS_LOGS, "");
    }
}
