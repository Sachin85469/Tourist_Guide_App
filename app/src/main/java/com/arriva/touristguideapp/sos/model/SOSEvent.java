package com.arriva.touristguideapp.sos.model;

public class SOSEvent {
    private final long timestamp;
    private final String triggerSource;
    private final boolean success;
    private final String locationStatus;

    public SOSEvent(long timestamp, String triggerSource, boolean success, String locationStatus) {
        this.timestamp = timestamp;
        this.triggerSource = triggerSource;
        this.success = success;
        this.locationStatus = locationStatus;
    }

    public long getTimestamp() { return timestamp; }
    public String getTriggerSource() { return triggerSource; }
    public boolean isSuccess() { return success; }
    public String getLocationStatus() { return locationStatus; }

    @Override
    public String toString() {
        return String.format("[%tF %tT] Source: %s, Success: %b, Location: %s",
                timestamp, timestamp, triggerSource, success, locationStatus);
    }
}
