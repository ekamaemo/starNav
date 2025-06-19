package com.example.starnav;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Сразу переходим к тестированию, без проверок
        startActivity(new Intent(this, AstrometrySessionActivity.class));
        finish(); // Закрываем MainActivity чтобы нельзя было вернуться назад
    }
}