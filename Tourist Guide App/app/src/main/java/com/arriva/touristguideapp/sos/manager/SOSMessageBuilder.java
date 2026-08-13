package com.arriva.touristguideapp.sos.manager;

import android.content.Context;
import android.telephony.SmsManager;

import com.arriva.touristguideapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class SOSMessageBuilder {

    public static final String PLACEHOLDER_LOCATION_LINK = "{LOCATION_LINK}";
    public static final String PLACEHOLDER_LATITUDE = "{LATITUDE}";
    public static final String PLACEHOLDER_LONGITUDE = "{LONGITUDE}";
    public static final String PLACEHOLDER_TIME = "{TIME}";
    public static final String PLACEHOLDER_DATE = "{DATE}";

    /** Max SMS parts allowed when sending (standard multipart limit). */
    public static final int MAX_SMS_PARTS = 10;

    /** Max characters for the stored template before placeholder expansion. */
    public static final int MAX_TEMPLATE_LENGTH = 1600;

    private SOSMessageBuilder() {
    }

    public static String getDefaultTemplate(Context context) {
        return context.getString(R.string.sos_default_message_template);
    }

    public static String build(
            Context context,
            String template,
            LocationHelper.LocationResult location,
            Date timestamp,
            boolean shareLocation
    ) {
        if (template == null || template.trim().isEmpty()) {
            template = getDefaultTemplate(context);
        }

        String locationLink = "Location unavailable";
        String latitude = "N/A";
        String longitude = "N/A";

        if (location != null) {
            if (location.mapsLink != null && !location.mapsLink.isEmpty()) {
                locationLink = location.mapsLink;
            }
            if (location.latitude != null) {
                latitude = String.valueOf(location.latitude);
            }
            if (location.longitude != null) {
                longitude = String.valueOf(location.longitude);
            }
        }

        if (!shareLocation) {
            locationLink = "Location sharing disabled";
            latitude = "N/A";
            longitude = "N/A";
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        Date when = timestamp != null ? timestamp : new Date();
        String time = timeFormat.format(when);
        String date = dateFormat.format(when);

        return template
                .replace(PLACEHOLDER_LOCATION_LINK, locationLink)
                .replace(PLACEHOLDER_LATITUDE, latitude)
                .replace(PLACEHOLDER_LONGITUDE, longitude)
                .replace(PLACEHOLDER_TIME, time)
                .replace(PLACEHOLDER_DATE, date);
    }

    public static boolean isTemplateEmpty(String template) {
        return template == null || template.trim().isEmpty();
    }

    public static boolean isTemplateWithinSmsLimit(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        SmsManager smsManager = SmsManager.getDefault();
        if (smsManager == null) {
            return message.length() <= MAX_TEMPLATE_LENGTH;
        }
        return smsManager.divideMessage(message).size() <= MAX_SMS_PARTS;
    }

    /**
     * Validates a template using worst-case placeholder lengths so expanded SMS stays within limits.
     */
    public static boolean isTemplateValidForSave(Context context, String template) {
        if (isTemplateEmpty(template)) {
            return false;
        }
        if (template.length() > MAX_TEMPLATE_LENGTH) {
            return false;
        }

        LocationHelper.LocationResult sample = new LocationHelper.LocationResult(
                "https://maps.google.com/?q=-90.000000,-180.000000",
                -90.0,
                -180.0
        );
        String expanded = build(context, template, sample, new Date(), true);
        return isTemplateWithinSmsLimit(expanded);
    }

    public static int getSmsPartCount(String message) {
        if (message == null || message.isEmpty()) {
            return 0;
        }
        SmsManager smsManager = SmsManager.getDefault();
        if (smsManager == null) {
            return 1;
        }
        return smsManager.divideMessage(message).size();
    }
}
