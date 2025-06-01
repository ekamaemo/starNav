package com.example.starnav;

public class SessionItem {
    public String imageUrl;
    public String date;
    public boolean isProcessed;

    public SessionItem(String imageUrl, String date, boolean isProcessed) {
        this.imageUrl = imageUrl;
        this.date = date;
        this.isProcessed = isProcessed;
    }
}