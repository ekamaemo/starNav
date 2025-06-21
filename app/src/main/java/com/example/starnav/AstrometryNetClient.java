package com.example.starnav;

import android.app.Activity;
import android.content.Context;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import java.io.File;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.CloseableHttpResponse;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.CloseableHttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClients;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.util.EntityUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AstrometryNetClient {
    private final String API_KEY;
    private static final String UPLOAD_URL = "http://nova.astrometry.net/api/upload";
    private static final String LOGIN_URL = "http://nova.astrometry.net/api/login";
    private final String[] imagePaths;
    private final String email;
    private final Context context;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public AstrometryNetClient(Context context, String email, String apiKey, String pathZenith, String pathPolarius) {
        this.context = context;
        this.imagePaths = new String[]{pathZenith, pathPolarius};
        this.API_KEY = apiKey;
        this.email = email;
    }

    public void processImages() {
        // Проверка файлов перед отправкой
        for (String path : imagePaths) {
            File file = new File(path);
            if (!file.exists() || !file.canRead()) {
                Log.e("Astrometry", "Cannot read file: " + path);
                return;
            }
        }
        new Thread(() -> {
            try {
                String sessionKey = getSessionKey();
                if (sessionKey.isEmpty()) {
                    Log.e("AstrometryNet", "Failed to get session key");
                    return;
                }
                Log.println(Log.ASSERT, "Началась новая сессия", sessionKey);
                DataBaseHelper databaseHelper = new DataBaseHelper(context);
                ZonedDateTime utcTime = ZonedDateTime.now(ZoneOffset.UTC);
                // Используйте ISO-8601 формат (рекомендуется)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String timeString = utcTime.format(formatter);

                String subid_polar = "";
                String subid_zenith = "";
                int heightPolImage = 0;
                int i = 0;
                for (String path : imagePaths) {
                    try {
                        String subId = uploadImage(path, sessionKey);
                        if (i == 1) {
                            subid_polar = subId;
                            heightPolImage = getImageHeight(path);

                        } else {
                            subid_zenith = subId;
                            i++;
                        }
                        Log.i("DataBase", "Image uploaded, subId: " + subId + " " + subid_zenith);
                    } catch (Exception e) {
                        Log.e("Database", "Error uploading image", e);
                    }
                }
                boolean res = databaseHelper.insertDataSessions(email, subid_polar, subid_zenith, timeString, heightPolImage);
                if (!res) {
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Не получилось отправить на решения изображения. Попробуйте позже", Toast.LENGTH_LONG).show());
                } else {
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Изображения отправлены. Ждите результат около 15 минут", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e("Database", "Error processing images", e);
            }
        }).start();
    }

    public String getSessionKey() throws IOException, JSONException {
        String sessionKey = "";

        // 1. Проверка API ключа
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalArgumentException("API key is not set");
        }
        // 2. Создаем JSON запрос
        JSONObject json = new JSONObject();
        ///ОСТОРОЖНООООООО НАДО ЗАМЕНИТЬ///
        json.put("apikey", "mwlmuhpsdvujnjrx");

        // Обертываем в "request-json" как в Python-коде
        JSONObject requestJson = new JSONObject();
        requestJson.put("request-json", json.toString());

        // Формируем FormBody как в Python-запросе
        RequestBody body = new FormBody.Builder()
                .add("request-json", json.toString())
                .build();

        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            Log.println(Log.ASSERT, "ooooooooooo", responseBody);
            JSONObject responseJson = new JSONObject(responseBody);
            // Проверяем статус и выводим session
            if ("success".equals(responseJson.getString("status"))) {
                sessionKey = responseJson.getString("session");
            } else {
                System.err.println("Error: " + responseJson.getString("errormessage"));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sessionKey;
    }

    private String uploadImage(String imagePath, String sessionKey) throws IOException, JSONException {
        File imageFile = new File(imagePath);

        // Проверка существования файла
        if (!imageFile.exists()) {
            throw new IOException("File not found: " + imagePath);
        }

        try {
            // 1. Создаем JSON параметры
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("session", sessionKey);
            jsonParams.put("publicly_visible", "y");
            jsonParams.put("allow_modifications", "d");
            jsonParams.put("allow_commercial_use", "d");

            // 2. Формируем multipart запрос
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("request-json",
                            null,
                            RequestBody.create(
                                    jsonParams.toString(),
                                    MediaType.parse("text/plain")))
                    .addFormDataPart("file",
                            imageFile.getName(),
                            RequestBody.create(
                                    imageFile,
                                    MediaType.parse("application/octet-stream")))
                    .build();

            // 3. Отправляем запрос
            Request request = new Request.Builder()
                    .url(UPLOAD_URL)
                    .post(requestBody)
                    .build();

            // 4. Обрабатываем ответ
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseBody = response.body().string();
                JSONObject responseJson = new JSONObject(responseBody);

                if ("success".equals(responseJson.getString("status"))) {
                    String subId = responseJson.optString("subid", "unknown");
                    Log.i("AstrometryUpload", "Success! Submission ID: " + subId);
                    return subId; // Возвращаем subid при успехе
                } else {
                    String errorMsg = responseJson.optString("errormessage", "Unknown error");
                    Log.e("AstrometryUpload", "Error: " + errorMsg);
                    throw new IOException("API Error: " + errorMsg);
                }
            }
        } catch (JSONException e) {
            Log.e("AstrometryUpload", "JSON parsing error", e);
            throw new IOException("Failed to parse API response", e);
        } catch (IOException e) {
            Log.e("AstrometryUpload", "Network error", e);
            throw e; // Пробрасываем исключение дальше
        }
    }

    public String isJobCompleted(String jobId) throws IOException {
        String url = "https://nova.astrometry.net/api/jobs/" + jobId;

        HttpGet request = new HttpGet(url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(request)) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject jobInfo = new JSONObject(json);
            return jobInfo.getString("status");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public double getPolarisYFromFits(Context context, String jobId) throws IOException {
        FitsReader fitsReader = new FitsReader();
        double polarisY = fitsReader.getPolarisY(jobId);
        return polarisY;
    }

    public static String getJobId(String submissionId, String sessionKey) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Формируем URL с параметрами
        HttpUrl url = HttpUrl.parse("http://nova.astrometry.net/api/submissions/" + submissionId).newBuilder()
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();

            // Проверяем, не XML ли это
            if (responseBody.trim().startsWith("<?xml")) {
                Log.d("subid", submissionId);
                throw new IOException("Server returned XML instead of JSON");
            }

            try {
                JSONObject submission = new JSONObject(responseBody);

                // Проверяем статус отправки
                if (submission.has("status")) {
                    String status = submission.getString("status");
                    if (!"success".equals(status)) {
                        throw new IOException("Submission status: " + status);
                    }
                }

                // Проверяем наличие ключа "jobs"
                if (!submission.has("jobs")) {
                    throw new IOException("No 'jobs' field in response");
                }

                JSONArray jobs = submission.getJSONArray("jobs");
                if (jobs.length() == 0) {
                    throw new IOException("No jobs available");
                }

                return String.valueOf(jobs.getInt(0));
            } catch (JSONException e) {
                // Логируем сырой ответ для отладки
                throw new IOException("Failed to parse JSON response", e);
            }
        }
    }
    public static JSONObject getJobInfo(String jobId, String session) throws IOException {
        HttpUrl url = HttpUrl.parse("http://nova.astrometry.net/api/jobs/" + jobId + "/info/").newBuilder()
                .addQueryParameter("session", session)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            return new JSONObject(responseBody);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public int getImageHeight(String imagePath) {
        // Создаем объект Options для чтения только метаданных
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // Только размеры, не загружаем само изображение

        // Читаем размеры файла
        BitmapFactory.decodeFile(imagePath, options);

        return options.outHeight; // Возвращаем высоту
    }
}