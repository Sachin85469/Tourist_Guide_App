package com.arriva.touristguideapp.sos.model;

import java.io.Serializable;

public class SOSEvent implements Serializable {
    private String eventId;
    private String date;
    private String time;
    private String location;
    private String contactsNotified;

    public SOSEvent() {}

    public SOSEvent(String eventId, String date, String time, String location, String contactsNotified) {
        this.eventId = eventId;
        this.date = date;
        this.time = time;
        this.location = location;
        this.contactsNotified = contactsNotified;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getContactsNotified() { return contactsNotified; }
    public void setContactsNotified(String contactsNotified) { this.contactsNotified = contactsNotified; }

    @Override
    public String toString() {
        return String.format("[%s %s] Location: %s, Notified: %s", date, time, location, contactsNotified);
    }
}
