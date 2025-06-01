package com.example.starnav;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        bottomNav = findViewById(R.id.bottomNavigation);

        bottomNav.setSelectedItemId(R.id.nav_profile);

        TextView link = findViewById(R.id.link);
        link.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tVUsername = findViewById(R.id.tvUsername);
        String username = prefs.getString("username", "no name");
        tVUsername.setText(username);

        TextView tVEmail = findViewById(R.id.tvEmail);
        String email = prefs.getString("email", "noname@noname.com");
        tVEmail.setText(email);

        TextView tvApiKey = findViewById(R.id.api_key);
        String apiKey = prefs.getString("api_key", "Добавить ключ");
        tvApiKey.setText(apiKey);
        tvApiKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
                builder.setTitle("Новый АPI ключ");

                final EditText input = new EditText(ProfileActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String api_key = input.getText().toString();
                        if (!api_key.isEmpty()) {
                            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("api_key", api_key).apply();
                            tvApiKey.setText(api_key);
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        // Навигация
        bottomNav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                case R.id.nav_camera:
                    String api_key = prefs.getString("api_key", "no");
                    if (api_key.equals("no")){
                        Toast.makeText(this, "Впишите API ключ в вашем профиле", Toast.LENGTH_SHORT);
                    }
                    else {
                        startActivity(new Intent(this, AstrometrySessionActivity.class));
                    }
                    return true;
                case R.id.nav_history:
                    startActivity(new Intent(this, HistoryOfSessionsActivity.class));
                    return true;
            }
            return false;
        });
    }
}
