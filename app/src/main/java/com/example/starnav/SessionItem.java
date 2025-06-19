package com.example.starnav;

public class SessionItem {
    public String date;
    public boolean isProcessed;
    public String status;
    public double latitude;
    public double longitude;

    public SessionItem(String date, boolean isProcessed, String status, double latitude, double longitude) {
        this.date = date;
        this.isProcessed = isProcessed;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}