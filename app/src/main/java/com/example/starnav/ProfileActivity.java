package com.example.starnav;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        TextView tVUsername = findViewById(R.id.tvUsername);
        String username = prefs.getString("username", "no name");
        tVUsername.setText(username);

        TextView tVEmail = findViewById(R.id.tvEmail);
        String email = prefs.getString("email", "noname@noname.com");
        tVEmail.setText(email);
    }
}
