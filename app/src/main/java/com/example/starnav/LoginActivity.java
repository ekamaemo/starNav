package com.example.starnav;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.starnav.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity{
    private ActivityLoginBinding binding;
    private DataBaseHelper databaseHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Button btnLogin = findViewById(R.id.btn_login);
        databaseHelper = new DataBaseHelper(this);

        // Кнопка входа
        btnLogin.setOnClickListener(v -> {
            String username = binding.username.getText().toString();
            String password = binding.password.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseHelper.checkEmailPassword(username, password)) {
                Toast.makeText(this, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show();

                // Сохраняем статус входа
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_logged_in", true)
                        .putString("username", username)
                        .apply();

                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Неверные учетные данные", Toast.LENGTH_SHORT).show();
            }
        });

        // Переход на регистрацию
        TextView register_btn = findViewById(R.id.signupText);
        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ваш код при нажатии
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        binding = null; // Очищаем binding
    }
}
