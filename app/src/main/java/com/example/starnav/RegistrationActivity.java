package com.example.starnav;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.starnav.databinding.ActivityRegistrationBinding;

public class RegistrationActivity extends AppCompatActivity {
    private ActivityRegistrationBinding binding;
    private DataBaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        databaseHelper = new DataBaseHelper(this);


        TextView loginBtn = findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        Button btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.length() == 0 || username.length() == 0 || password.length() == 0) {
                Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseHelper.checkEmail(email)) {
                Toast.makeText(this, "Пользователь с таким email уже существует", Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseHelper.insertDataUsers(email, username, password)) {
                Toast.makeText(this, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show();

                getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_logged_in", true)
                        .putString("email", email)
                        .putString("username", username)
                        .apply();

                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

}