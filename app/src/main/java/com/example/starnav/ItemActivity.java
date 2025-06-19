package com.example.starnav;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ItemActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        // Получаем переданные координаты
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            double latitude = extras.getDouble("latitude", 0.0);
            double longitude = extras.getDouble("longitude", 0.0);

            // Проверяем валидность координат
            if (latitude == 0.0 && longitude == 0.0) {
                showDefaultLocation();
                Toast.makeText(this, "Координаты не найдены", Toast.LENGTH_SHORT).show();
            } else {
                showLocation(latitude, longitude);
            }
        } else {
            showDefaultLocation();
        }
    }

    private void showLocation(double latitude, double longitude) {
        String coordinates = String.format("%.6f,%.6f", latitude, longitude);
        String googleMapsUrl = "https://www.google.com/maps?q=" + coordinates;
        loadWebView(googleMapsUrl);
    }

    private void showDefaultLocation() {
        String googleMapsUrl = "https://www.google.com/maps?q=55.7558,37.6173"; // Москва по умолчанию
        loadWebView(googleMapsUrl);
    }

    private void loadWebView(String url) {
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true); // Для корректной работы карт
        webView.loadUrl(url);
    }
}