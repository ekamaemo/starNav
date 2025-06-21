package com.example.starnav;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.Locale;  // Добавлен импорт для Locale

public class ItemActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        // Настройка Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Обработчик кнопки назад
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

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
        // Используем Locale.US для гарантии точки в качестве разделителя
        String coordinates = String.format(Locale.US, "%.6f,%.6f", latitude, longitude);
        String googleMapsUrl = "https://www.google.com/maps?q=" + coordinates;
        loadWebView(googleMapsUrl);
    }

    private void showDefaultLocation() {
        // Также используем Locale.US для координат по умолчанию
        String coordinates = String.format(Locale.US, "%.6f,%.6f", 55.7558, 37.6173);
        String googleMapsUrl = "https://www.google.com/maps?q=" + coordinates;
        loadWebView(googleMapsUrl);
    }

    private void loadWebView(String url) {
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.loadUrl(url);
    }
}