package com.example.starnav;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

public class ItemActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        String latitude = "55.7558";  // Широта (Москва)
        String longitude = "37.6173"; // Долгота
        String googleMapsUrl = "https://www.google.com/maps?q=" + latitude + "," + longitude;
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true); // Включаем JavaScript (обязательно для Google Maps)
        webView.loadUrl(googleMapsUrl);
    }
}
