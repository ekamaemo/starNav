package com.example.starnav;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
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
        super(context, "starNavDatabase.db", null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase MyDatabase) {
        MyDatabase.execSQL("create Table users(email TEXT primary key, username TEXT, password TEXT)");
        MyDatabase.execSQL("create Table sessions(email TEXT,  subid_polar TEXT, subid_zenith TEXT primary key, status TEXT, created_atUTC TEXT, latitude DOUBLE, longitude DOUBLE)");
        // Добавляем тестовые данные
        insertTestSessions(MyDatabase);
    }

    private void insertTestSessions(SQLiteDatabase db) {
        // Тестовая сессия 1 (успешная)
        ContentValues cv1 = new ContentValues();
        cv1.put("email", "noname@noname");
        cv1.put("subid_polar", "sub_polar_123");
        cv1.put("subid_zenith", "sub_zenith_456");
        cv1.put("status", "success");
        cv1.put("created_atUTC", "2023-05-15T12:00:00Z");
        cv1.put("latitude", 55.7558);
        cv1.put("longitude", 37.6173);
        db.insert("sessions", null, cv1);

        // Тестовая сессия 2 (в процессе)
        ContentValues cv2 = new ContentValues();
        cv2.put("email", "noname@noname");
        cv2.put("subid_polar", "12611221");
        cv2.put("subid_zenith", "12611225");
        cv2.put("status", "processing");
        cv2.put("created_atUTC", "2023-05-16T13:30:00Z");
        db.insert("sessions", null, cv2);

        // Тестовая сессия 3 (ошибка)
        ContentValues cv3 = new ContentValues();
        cv3.put("email", "noname@noname");
        cv3.put("subid_polar", "sub_polar_345");
        cv3.put("subid_zenith", "sub_zenith_678");
        cv3.put("status", "failed");
        cv3.put("created_atUTC", "2023-05-17T14:45:00Z");
        db.insert("sessions", null, cv3);
    }

    @Override
    public void onUpgrade(SQLiteDatabase MyDB, int i, int i1) {
        MyDB.execSQL("drop Table if exists users");
        MyDB.execSQL("DROP TABLE IF EXISTS sessions");
        onCreate(MyDB);
    }
    public Boolean insertDataUsers(String email, String username, String password){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", email);
        contentValues.put("username", username);
        contentValues.put("password", password);
        long result = MyDatabase.insert("users", null, contentValues);

        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public Boolean insertDataSessions(String email, String subid_polar, String subid_zenith, String dateUTC){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", email);
        contentValues.put("subid_polar", subid_polar);
        contentValues.put("subid_zenith", subid_zenith);
        contentValues.put("created_atUTC", dateUTC);
        contentValues.put("status", "processing");
        long result = MyDatabase.insert("sessions", null, contentValues);
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

    public boolean updateSessions(String email, String subid_zenith, String subid_polaris, Double latitude, Double longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        String status = "success";
        ContentValues values = new ContentValues();
        if (latitude != 0){
            values.put("latitude", latitude);
        } else {
            status = "partial_success";
        }
        if (longitude != 0){
            values.put("longitude", longitude);
        }
        else {
            status = "failed";
        }
        values.put("status", status);
        String whereClause = "email = ? AND subid_zenith = ? AND subid_polaris = ?";
        String[] whereArgs = {email, subid_zenith, subid_polaris};
        try {
            int rowsAffected = db.update("sessions", values, whereClause, whereArgs);

            if (rowsAffected > 0) {
                Log.d("Database", "Успешно обновлено " + rowsAffected + " записей");
                return true;
            } else {
                Log.w("Database", "Не найдено записей для обновления");
                return false;
            }
        } catch (SQLException e) {
            Log.e("Database", "Ошибка при обновлении: " + e.getMessage());
            return false;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }
}
