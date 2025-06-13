package com.example.starnav;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DataBaseHelper extends SQLiteOpenHelper{

    private static List<String> JOBS;

    public DataBaseHelper(@Nullable Context context) {
        super(context, "starNavDatabase.db", null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase MyDatabase) {
        MyDatabase.execSQL("create Table users(email TEXT primary key, username TEXT, password TEXT)");
        MyDatabase.execSQL("create Table sessions(email TEXT, session_id TEXT, subid TEXT primary key, date_of_dispatch TEXT, polar_image_path TEXT, zenith_image_path TEXT, latitude DOUBLE, longitude DOUBLE, status TEXT)");
        MyDatabase.execSQL("create Table jobs(job_id TEXT primary key, session_id TEXT, type TEXT, status TEXT, result_data TEXT)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase MyDB, int i, int i1) {
        MyDB.execSQL("drop Table if exists users");
        MyDB.execSQL("DROP TABLE IF EXISTS sessions");
        MyDB.execSQL("drop Table if exists jobs");
        onCreate(MyDB);
    }
    public Boolean insertDataUsers(String email, String username, String password){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", email);
        contentValues.put("username", username);
        contentValues.put("password", password);
        long result = MyDatabase.insert("users", null, contentValues);
        Boolean b = insertDataSessions(email, "1234567", "909090", "2025-06-02 00:48:26.207801+00:00", "res/drawable/polar_image.png", "res/drawable/polar_image.png");

        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public Boolean insertDataSessions(String email, String session_id, String subid, String date, String polar_image_path, String zenith_image_path){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", email);
        contentValues.put("session_id", session_id);
        contentValues.put("subid", subid);
        contentValues.put("date_of_dispatch", date);
        contentValues.put("polar_image_path", polar_image_path);
        contentValues.put("zenith_image_path", zenith_image_path);
        contentValues.put("status", "submitted");
        long result = MyDatabase.insert("sessions", null, contentValues);
        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }


    public boolean insertDataJobs(String session_id, String job_id, String type, String status, String info){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("session_id", session_id);
        contentValues.put("job_id", job_id);
        contentValues.put("type", type);
        contentValues.put("status", status);
        contentValues.put("result_data", info);
        long result = MyDatabase.insert("jobs", null, contentValues);
        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public Boolean checkEmail(String email){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        Cursor cursor = MyDatabase.rawQuery("Select * from users where email = ?", new String[]{email});
        if(cursor.getCount() > 0) {
            return true;
        }else {
            return false;
        }
    }
    public Boolean checkEmailPassword(String email, String password){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        Cursor cursor = MyDatabase.rawQuery("Select * from users where email = ? and password = ?", new String[]{email, password});
        if (cursor.getCount() > 0) {
            return true;
        }else {
            return false;
        }
    }

    public Cursor getUserSessions(String email) {
        SQLiteDatabase MyDatabase = this.getReadableDatabase();
        return MyDatabase.rawQuery(
                "SELECT subid, date_of_dispatch FROM sessions WHERE email = ? ORDER BY date_of_dispatch DESC",
                new String[]{email}
        );
    }

    public boolean updateJobs(String jobId, String sessionId, String newStatus, String resultData) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("status", newStatus);          // Обновляем статус
        values.put("result_data", resultData);    // Обновляем данные

        // Условие WHERE: проверяем И job_id, И session_id
        String whereClause = "job_id = ? AND session_id = ?";
        String[] whereArgs = new String[]{jobId, sessionId};

        // Выполняем обновление
        int rowsAffected = db.update(
                "jobs",
                values,
                whereClause,
                whereArgs
        );

        return rowsAffected > 0;  // true, если обновили хотя бы 1 строку
    }

    public boolean updateSessions(String email, String session_id, double yPol) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            String timeOfDispatch = null;
            Cursor sessionCursor = db.query("sessions",
                    new String[]{"time_of_dispatch"},
                    "email = ? AND session_id = ?",
                    new String[]{email, session_id},
                    null, null, null);

            if (sessionCursor.moveToFirst()) {
                timeOfDispatch = sessionCursor.getString(0);
            }
            sessionCursor.close();

            if (timeOfDispatch == null) {
                return false; // Нет данных о времени
            }

            // 1. Получаем обе job для этой сессии
            Cursor cursor = db.query("jobs",
                    new String[]{"job_id", "type", "result_data"},
                    "session_id = ?",
                    new String[]{session_id},
                    null, null, null);

            String polarJobId = null;
            String zenithJobId = null;
            JSONObject zenithData = null;
            boolean polarSolved = false;
            boolean zenithSolved = false;

            // 2. Анализируем результаты
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String type = cursor.getString(cursor.getColumnIndex("type"));
                @SuppressLint("Range") String resultData = cursor.getString(cursor.getColumnIndex("result_data"));
                @SuppressLint("Range") String jobId = cursor.getString(cursor.getColumnIndex("job_id"));

                if ("polar".equals(type)) {
                    polarJobId = jobId;
                    polarSolved = !resultData.isEmpty();
                } else if ("zenith".equals(type)) {
                    zenithJobId = jobId;
                    if (!resultData.isEmpty()) {
                        try {
                            zenithData = new JSONObject(resultData);
                            zenithSolved = true;
                        } catch (JSONException e) {
                            Log.e("DB", "Error parsing zenith data", e);
                        }
                    }
                }
            }
            cursor.close();

            // 3. Проверяем условия для обновления
            ContentValues values = new ContentValues();
            boolean shouldUpdate = false;

            if (polarSolved && zenithSolved && zenithData != null) {
                try {
                    // Вычисляем координаты (примерная логика)
                    double ra = zenithData.getDouble("ra");

                    // Здесь должна быть ваша формула расчета координат
                    // Например:
                    double latitude = calculateLatitude(yPol);
                    double longitude = calculateLongitude(ra, timeOfDispatch);  // Нужно реализовать

                    values.put("latitude", latitude);
                    values.put("longitude", longitude);
                    values.put("status", "completed");
                    shouldUpdate = true;

                } catch (JSONException e) {
                    Log.e("DB", "Error extracting zenith data", e);
                }
            }

            // 4. Выполняем обновление, если нужно
            if (shouldUpdate) {
                int rowsAffected = db.update("sessions",
                        values,
                        "email = ? AND session_id = ?",
                        new String[]{email, session_id});

                db.setTransactionSuccessful();
                return rowsAffected > 0;
            }

            return false;

        } finally {
            db.endTransaction();
        }
    }

    // Заглушки для методов расчета (реализуйте их согласно вашей логике)
    private double calculateLatitude(double y) {

        return ObserverPosition.getLatitude(y);
    }

    private double calculateLongitude(double ra, String time) {
        // Реализуйте расчет долготы по данным зенита
        return ObserverPosition.calculateLongitude(ra, time);
    }

    public boolean jobExists(String jobId, String sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                "jobs",
                new String[]{"job_id"},
                "job_id = ? AND session_id = ?",
                new String[]{jobId, sessionId},
                null, null, null
        );

        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }
}
