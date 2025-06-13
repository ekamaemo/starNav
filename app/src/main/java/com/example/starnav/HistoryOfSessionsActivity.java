package com.example.starnav;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HistoryOfSessionsActivity extends AppCompatActivity{
    private RecyclerView sessionsRecyclerView;
    private BottomNavigationView bottomNav;
    private SessionsAdapter adapter;
    private DataBaseHelper dbHelper;
    private String API_KEY;
    private AstrometryNetClient myClient;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_of_sessions);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        API_KEY = prefs.getString("api_key", "no");
        String email = prefs.getString("email", "noname@noname");

        RecyclerView recyclerView = findViewById(R.id.sessionsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_history);
        dbHelper = new DataBaseHelper(this);

        myClient = new AstrometryNetClient(this, email, API_KEY, "", "");

        List<SessionItem> items = Arrays.asList(
                new SessionItem("http://example.com/photo1.jpg", "12.05.2023", true),
                new SessionItem("http://example.com/photo2.jpg", "13.05.2023", false)
        );
//        List<SessionItem> items = null;
//        try {
//            items = getSessions(prefs.getString("email", "noname@noname.com"));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        recyclerView.setAdapter(
                new SessionsAdapter(
                        items,  // Передаем список items
                        item -> {  // Лямбда-выражение для OnItemClickListener
                            // Обработка клика
                            Intent intent = new Intent(this, ItemActivity.class);
                            startActivity(intent);
                        }
                )
        );


        bottomNav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                case R.id.nav_camera:
                    String api_key = prefs.getString("api_key", "no");
                    if (api_key.equals("no")){
                        Toast.makeText(this, "Впишите API ключ в вашем профиле", Toast.LENGTH_LONG);
                    }
                    else {
                        startActivity(new Intent(this, AstrometrySessionActivity.class));
                    }
                    return true;
                case R.id.nav_profile:
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
            }
            return false;
        });
    }

    private List<SessionItem> getSessions(String email) throws IOException {
        List<SessionItem> items = new ArrayList<>();
        String currDate = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        Cursor sessionsCursor = dbHelper.getUserSessions(email);

        if (sessionsCursor != null && sessionsCursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String subid = sessionsCursor.getString(sessionsCursor.getColumnIndex("subid"));
                @SuppressLint("Range") String date = sessionsCursor.getString(sessionsCursor.getColumnIndex("date_of_dispatch"));

                String[] subids = subid.split(" ");
                String subidZenith = subids[0];
                String subidPolarius = subids[1];
//
//                // 1. Обработка зенита
//                List<String> zenithJobIds = myClient.getJobsIds(subidZenith);
//                if (!zenithJobIds.isEmpty()) {
//                    String zenithJobId = zenithJobIds.get(0);
//
//                    // Проверяем существование job в БД
//                    if (!dbHelper.jobExists(zenithJobId, subidZenith)) {
//                        // Вставляем новую запись
//                        dbHelper.insertDataJobs(
//                                subidZenith,
//                                zenithJobId,
//                                "zenith",
//                                "submitted", // начальный статус
//                                "" // пустые данные
//                        );
//                    }
//
//                    // Обновляем информацию
//                    myClient.getInfo(zenithJobId, subidZenith);
//                }
//
//                // 2. Обработка полярной звезды
//                List<String> polarJobIds = myClient.getJobsIds(subidPolarius);
//                double yPol = 0;
//                if (!polarJobIds.isEmpty()) {
//                    String polarJobId = polarJobIds.get(0);
//
//                    if (!dbHelper.jobExists(polarJobId, subidPolarius)) {
//                        dbHelper.insertDataJobs(
//                                subidPolarius,
//                                polarJobId,
//                                "polar",
//                                "submitted",
//                                ""
//                        );
//                    }
//                    yPol = myClient.downloadAndReadFitsFile(this, subidPolarius);
//
//                    myClient.getInfo(polarJobId, subidPolarius);
//                }
//                dbHelper.updateSessions(email, subid, yPol);
                // Добавляем элемент в список
                items.add(new SessionItem("", date, isProcessed(currDate, date)));
                Log.d("SessionInfo", "SubID: " + subid + ", DateUTC: " + date);

            } while (sessionsCursor.moveToNext());

            sessionsCursor.close();
        }
        return items;
    }

    private boolean isProcessed(String currDate, String otherDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");

            // Парсим даты
            Date date1 = format.parse(currDate);
            Date date2 = format.parse(otherDate);

            // Вычисляем разницу в миллисекундах
            long diffInMillis = Math.abs(date1.getTime() - date2.getTime());

            // 15 минут = 15 * 60 * 1000 миллисекунд
            return diffInMillis <= (15 * 60 * 1000);

        } catch (ParseException e) {
            e.printStackTrace();
            return false; // В случае ошибки парсинга считаем, что даты не удовлетворяют условию
        }
    }
}
