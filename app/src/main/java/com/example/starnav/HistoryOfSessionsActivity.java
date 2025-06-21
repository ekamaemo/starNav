package com.example.starnav;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
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
import java.util.Objects;

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
        String email = prefs.getString("email", "noname@noname.com");

//        RecyclerView recyclerView = findViewById(R.id.sessionsRecyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionsRecyclerView = findViewById(R.id.sessionsRecyclerView);
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_history);
        dbHelper = new DataBaseHelper(this);
        // Запускаем фоновую задачу
        new FetchSessionsTask().execute(email);

//        List<SessionItem> items = null;
//        try {
//            items = getSessions(prefs.getString("email", "noname@noname.com"));
//        } catch (IOException | JSONException e) {
//            throw new RuntimeException(e);
//        }
//
//        recyclerView.setAdapter(
//                new SessionsAdapter(
//                        items,  // Передаем список items
//                        item -> {  // Лямбда-выражение для OnItemClickListener
//                            // Обработка клика
//                            Intent intent = new Intent(this, ItemActivity.class);
//                            startActivity(intent);
//                        }
//                )
//        );


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

    private String countStatus(String status1, String status2){
        if (status1.equals("success") && status2.equals("success")){
            return "success";
        } else if (status1.equals("failure") && status2.equals("failure")){
            return "failed";
        } else if ((status1.equals("success") || status1.equals("failure")) && ((status2.equals("success") || status2.equals("failure")))){
            return "partial_success";
        }
        return "processing";
    }

    private class FetchSessionsTask extends AsyncTask<String, Void, List<SessionItem>> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(HistoryOfSessionsActivity.this);
            progressDialog.setMessage("Загрузка данных...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected List<SessionItem> doInBackground(String... emails){
            String email = emails[0];
            List<SessionItem> items = new ArrayList<>();
            try {
                myClient = new AstrometryNetClient(HistoryOfSessionsActivity.this, email, API_KEY, "", "");
                String currDate = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                Cursor sessionsCursor = dbHelper.getUserSessions(email);

                if (sessionsCursor != null && sessionsCursor.moveToFirst()) {
                    Log.d("cursoropen", "1");
                    do {
                        @SuppressLint("Range") String status = sessionsCursor.getString(sessionsCursor.getColumnIndex("status"));
                        @SuppressLint("Range") String subid_polar = sessionsCursor.getString(sessionsCursor.getColumnIndex("subid_polar"));
                        @SuppressLint("Range") String subid_zenith = sessionsCursor.getString(sessionsCursor.getColumnIndex("subid_zenith"));
                        @SuppressLint("Range") String date = sessionsCursor.getString(sessionsCursor.getColumnIndex("created_atUTC"));
                        @SuppressLint("Range") int height = sessionsCursor.getInt(sessionsCursor.getColumnIndex("height"));
                        Log.d("session1", status);
                        double latitude = 0;
                        double longitude = 0;
                        if (status.equals("processing")) {

                            String session = myClient.getSessionKey();
                            String jobid_polar = AstrometryNetClient.getJobId(subid_polar, session);
                            String jobid_zenith = AstrometryNetClient.getJobId(subid_zenith, session);

                            String status_polar = myClient.isJobCompleted(jobid_polar);
                            String status_zenith = myClient.isJobCompleted(jobid_zenith);
                            if (Objects.equals(status_polar, "success") && (Objects.equals(status_zenith, "success"))) {
                                // Считаем широту
                                double polarisY = myClient.getPolarisYFromFits(HistoryOfSessionsActivity.this, jobid_polar);
                                if (polarisY == -1) {
                                    Log.println(Log.ASSERT, "POLARIS", "Полярная звезда не найдена на изображении");
                                } else {
                                    Log.println(Log.ASSERT, "POLARIS", "Координата Полярной звезды: " + polarisY);
                                    // Делаем рассчет широты
                                    JSONObject polarInfo = AstrometryNetClient.getJobInfo(jobid_polar, session);
                                    if ("success".equals(polarInfo.getString("status"))) {
                                        JSONObject calib = polarInfo.getJSONObject("calibration");
                                        double scaleDegPerPixel = calib.getDouble("pixscale") / 3600;
                                        double fovHeight = scaleDegPerPixel * height;
                                        latitude = ObserverPosition.getLatitude(polarisY, fovHeight, height);
                                    }
                                }
                                // Считаем долготу
                                JSONObject zenithInfo = myClient.getJobInfo(jobid_polar, session);
                                if ("success".equals(zenithInfo.getString("status"))) {
                                    JSONObject calib = zenithInfo.getJSONObject("calibration");
                                    double raCenter = calib.getDouble("ra");
                                    double raCenterH = raCenter / 15;
                                    longitude = ObserverPosition.calculateLongitude(raCenterH, date);
                                }
                                status = "success";
                            } else {
                                status = countStatus(status_polar, status_zenith);
                            }
                            dbHelper.updateSessions(email, subid_zenith, subid_polar, latitude, longitude);
                        }
                        else if (status.equals("success") || status.equals("partial_success")) {
                            // Извлекаем координаты из БД для успешных сессий
                            latitude = sessionsCursor.getDouble(sessionsCursor.getColumnIndex("latitude"));
                            longitude = sessionsCursor.getDouble(sessionsCursor.getColumnIndex("longitude"));
                            Log.d("DB_Coords", "From DB - Lat: " + latitude + ", Lon: " + longitude);
                        }

                        // Добавляем элемент в список
                        items.add(new SessionItem(date, isProcessed(currDate, date), status, latitude, longitude));
                        Log.d("SessionInfo", " " + date + status + latitude + longitude);
                    } while (sessionsCursor.moveToNext());

                    sessionsCursor.close();
                }

                return items;
            } catch (JSONException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(List<SessionItem> items) {
            progressDialog.dismiss();

            if (items != null) {
                adapter = new SessionsAdapter(HistoryOfSessionsActivity.this, items, item -> {
                    // Обработка клика (если нужна дополнительная логика)
                    Intent intent = new Intent(HistoryOfSessionsActivity.this, ItemActivity.class);
                    intent.putExtra("latitude", item.getLatitude());
                    intent.putExtra("longitude", item.getLongitude());
                    startActivity(intent);
                });
                sessionsRecyclerView.setAdapter(adapter);
            } else {
                Toast.makeText(HistoryOfSessionsActivity.this,
                        "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }
}


