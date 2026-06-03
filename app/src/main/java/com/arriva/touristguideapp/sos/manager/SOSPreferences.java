package com.arriva.touristguideapp.sos.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.arriva.touristguideapp.data.sos.SOSContact;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SOSPreferences {
    private static final String PREF_NAME = "sos_prefs";
    private static final String KEY_CONTACT_NUMBER = "contact_number";
    private static final String KEY_VOLUME_TRIGGER_ENABLED = "volume_trigger_enabled";
    private static final String KEY_FLOATING_BUTTON_ENABLED = "floating_button_enabled";
    private static final String KEY_CALL_AFTER_SMS = "call_after_sms";
    private static final String KEY_SOS_LOGS = "sos_logs";
    
    // New SOS Configuration keys
    private static final String KEY_CONTACTS_LIST = "emergency_contacts_list";
    private static final String KEY_SEND_SMS_AUTOMATICALLY = "send_sms_automatically";
    private static final String KEY_SHARE_LIVE_LOCATION = "share_live_location";
    private static final String KEY_REQUIRE_CONFIRMATION = "require_confirmation";

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

    // New SOS configuration settings
    public void setContacts(List<SOSContact> contacts) {
        String json = new Gson().toJson(contacts);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_CONTACTS_LIST, json);
        
        // Backward compatibility sync: update legacy phone number to the first contact's phone
        if (contacts != null && !contacts.isEmpty()) {
            editor.putString(KEY_CONTACT_NUMBER, contacts.get(0).getPhone());
        } else {
            editor.putString(KEY_CONTACT_NUMBER, "");
        }
        editor.apply();
    }

    public List<SOSContact> getContacts() {
        String json = prefs.getString(KEY_CONTACTS_LIST, null);
        if (json == null || json.isEmpty()) {
            List<SOSContact> list = new ArrayList<>();
            String legacyPhone = getContactNumber();
            if (legacyPhone != null && !legacyPhone.trim().isEmpty()) {
                SOSContact legacy = new SOSContact();
                legacy.setId("legacy_primary");
                legacy.setName("Primary Contact");
                legacy.setPhone(legacyPhone);
                legacy.setRelationship("Guardian");
                list.add(legacy);
            }
            return list;
        }
        Type type = new TypeToken<ArrayList<SOSContact>>(){}.getType();
        try {
            return new Gson().fromJson(json, type);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setSendSMSAutomatically(boolean enabled) {
        prefs.edit().putBoolean(KEY_SEND_SMS_AUTOMATICALLY, enabled).apply();
    }

    public boolean isSendSMSAutomatically() {
        return prefs.getBoolean(KEY_SEND_SMS_AUTOMATICALLY, true);
    }

    public void setShareLiveLocation(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHARE_LIVE_LOCATION, enabled).apply();
    }

    public boolean isShareLiveLocation() {
        return prefs.getBoolean(KEY_SHARE_LIVE_LOCATION, true);
    }

    public void setRequireConfirmation(boolean enabled) {
        prefs.edit().putBoolean(KEY_REQUIRE_CONFIRMATION, enabled).apply();
    }

    public boolean isRequireConfirmation() {
        return prefs.getBoolean(KEY_REQUIRE_CONFIRMATION, false);
    }
}
