package com.arriva.touristguideapp.data.notifications;

import java.io.Serializable;

public class NotificationModel implements Serializable {
    public static final String TYPE_TRIP = "trip";
    public static final String TYPE_REVIEW = "review";
    public static final String TYPE_FAVORITE = "favorite";
    public static final String TYPE_SOS = "sos";
    public static final String TYPE_SYSTEM = "system";

    private String id;
    private String userId;
    private String title;
    private String message;
    private String type;
    private String dataId; // For deep linking (tripId, placeId, etc.)
    private long timestamp;
    private boolean isRead;

    public NotificationModel() {}

    public NotificationModel(String id, String title, String message, String type, long timestamp) {
        this(id, title, message, type, null, timestamp);
    }

    public NotificationModel(String id, String title, String message, String type, String dataId, long timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.dataId = dataId;
        this.timestamp = timestamp;
        this.isRead = false;
        this.userId = "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDataId() { return dataId; }
    public void setDataId(String dataId) { this.dataId = dataId; }

    public String getNotificationId() { return id; }
    public void setNotificationId(String notificationId) { this.id = notificationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getCreatedAt() { return timestamp; }
    public void setCreatedAt(long createdAt) { this.timestamp = createdAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
