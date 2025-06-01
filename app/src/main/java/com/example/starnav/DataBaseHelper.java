package com.example.starnav;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
        super(context, "starNavDatabase.db", null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase MyDatabase) {
        MyDatabase.execSQL("create Table users(email TEXT primary key, username TEXT, password TEXT)");
        MyDatabase.execSQL("create Table sessions(email TEXT, subid TEXT, date_of_dispatch TEXT, jobs_id TEXT)");
        MyDatabase.execSQL("create Table jobs(job_id TEXT primary key, info JSON)");
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

    public Boolean insertDataSessions(String email, String subid, String date){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", email);
        contentValues.put("subid", subid);
        contentValues.put("date_of_dispatch", date);
        long result = MyDatabase.insert("sessions", null, contentValues);
        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public boolean insertDataJobs(String jobID, JSONObject info){
        SQLiteDatabase MyDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("job_id", jobID);
        contentValues.put("info", info.toString());
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

    public List<String> getJobsIds(String SUBID) throws IOException {
        String url = "https://nova.astrometry.net/api/submissions/" + SUBID;
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()  // Используем GET вместо POST, так как мы получаем данные
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseData = response.body().string();
            JSONObject json = new JSONObject(responseData);

            // Извлекаем массив jobs из JSON
            JSONArray jobsArray = json.getJSONArray("jobs");

            // Создаем список для хранения ID работ
            List<String> jobIds = new ArrayList<>();

            // Проходим по всем элементам массива и извлекаем ID
            for (int i = 0; i < jobsArray.length(); i++) {
                jobIds.add(String.valueOf(jobsArray.getInt(i)));
            }

            return jobIds;
        } catch (Exception e) {
            throw new IOException("Failed to parse jobs IDs", e);
        }
    }

    public void getInfo(String jobID, String Email){
        boolean isProcessed = false;
        String url = "https://nova.astrometry.net/api/jobs/" + jobID + "/info/";
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()  // Используем GET вместо POST, так как мы получаем данные
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseData = response.body().string();
            JSONObject json = new JSONObject(responseData);

            String status = json.getString("status");
            if (status.equals("success")) {
                isProcessed = true;

            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
