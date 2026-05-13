package com.arriva.touristguideapp.data.notifications;

import java.io.Serializable;

public class NotificationModel implements Serializable {
    private String id;
    private String title;
    private String message;
    private String type;
    private long timestamp;
    private boolean isRead;

    public NotificationModel() {}

    public NotificationModel(String id, String title, String message, String type, long timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
