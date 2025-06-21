package com.example.starnav;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private VideoView videoView;
    private ImageButton btnLogout;
    private BottomNavigationView bottomNav;
    private boolean isVideoPlaying = false;
    private static int k = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

        if (isFirstLaunch) {
            // Показываем WelcomeActivity только при первом запуске
            prefs.edit().putBoolean("is_first_launch", false).apply();
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            finish();
            return;
        }

        if (!isLoggedIn) {
            //show start activity
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Инициализация элементов
        videoView = findViewById(R.id.videoBackground);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNav = findViewById(R.id.bottomNavigation);

        // Настройка видео-фона
        setupVideoBackground();

        // Обработчик кнопки выхода
        btnLogout.setOnClickListener(v -> {
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_logged_in", false)
                    .apply();

            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Навигация
        bottomNav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_camera:
                    String api_key = prefs.getString("api_key", "no");
                    if (api_key.equals("no")){
                        Toast.makeText(this, "Впишите API ключ в вашем профиле", Toast.LENGTH_LONG);
                    }
                    else {
                        startActivity(new Intent(this, AstrometrySessionActivity.class));
                    }
                    return true;
                case R.id.nav_history:
                    startActivity(new Intent(this, HistoryOfSessionsActivity.class));
                    return true;
                case R.id.nav_profile:
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
            }
            return false;
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!videoView.isPlaying()) {
            videoView.start();
            isVideoPlaying = true;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isVideoPlaying) {
            videoView.stopPlayback();
        }
    }

    private void setupVideoBackground() {
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test);
        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            mp.setVolume(0, 0);

            // Рассчитываем соотношение сторон
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();
            float videoRatio = (float) videoWidth / videoHeight;

            // Получаем размеры экрана
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            float screenRatio = (float) metrics.widthPixels / metrics.heightPixels;

            // Настраиваем масштабирование
            ViewGroup.LayoutParams params = videoView.getLayoutParams();
            if (videoRatio > screenRatio) {
                // Видео шире экрана - обрезаем по бокам
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = (int) (metrics.widthPixels / videoRatio);
            } else {
                // Видео уже экрана - обрезаем сверху/снизу
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = (int) (metrics.heightPixels * videoRatio);
            }
            videoView.setLayoutParams(params);
        });
        videoView.start();
    }
}