package com.example.starnav;

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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import android.util.Log;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.CloseableHttpResponse;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.CloseableHttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClients;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.util.EntityUtils;

import java.util.List;

public class AstrometryNetClient {
    private final String API_KEY;
    private static final String UPLOAD_URL = "https://nova.astrometry.net/api/upload";
    private static final String LOGIN_URL = "https://nova.astrometry.net/api/login";
    private final String[] imagePaths;
    private final String email;
    private final Context context;

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
                if (sessionKey == null) {
                    Log.e("AstrometryNet", "Failed to get session key");
                    return;
                }

                DataBaseHelper databaseHelper = new DataBaseHelper(context);
                ZonedDateTime utcTime = ZonedDateTime.now(java.time.ZoneOffset.UTC);
                // Используйте ISO-8601 формат (рекомендуется)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String timeString = utcTime.format(formatter);
                String subIds = "";
                for (String path : imagePaths) {
                    try {
                        String subId = uploadImage(path, sessionKey);
                        if (subId != null) {
                            subIds += " " + subId;
                            Log.i("DataBase", "Image uploaded, subId: " + subId);
                        }
                    } catch (Exception e) {
                        Log.e("Database", "Error uploading image", e);
                    }
                }
                databaseHelper.insertDataSessions(email, sessionKey, subIds, timeString, imagePaths[1], imagePaths[0]);
            } catch (Exception e) {
                Log.e("Database", "Error processing images", e);
            }
        }).start();
    }

    private String getSessionKey() throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();

        // 1. Проверка API ключа
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalArgumentException("API key is not set");
        }

        // 2. Создаем JSON запрос
        JSONObject json = new JSONObject();
        json.put("apikey", API_KEY);

        // 3. Настраиваем HTTP-запрос
        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        // 4. Выполняем запрос
        try (Response response = client.newCall(request).execute()) {
            // Проверяем статус ответа
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                throw new IOException("HTTP error " + response.code() + ": " + errorBody);
            }
            Log.println(Log.ASSERT, "ooooo", String.valueOf(response));
            byte[] bytes = response.body().bytes();
            String rawResponse = new String(bytes, StandardCharsets.UTF_8);
            Log.d("RAW_RESPONSE", rawResponse);
            // Читаем ответ
            String responseBody = response.body().string();
            Log.d("Astrometry", "Raw login response: " + responseBody);

            // Парсим JSON
            JSONObject responseJson = new JSONObject(responseBody);
            Log.println(Log.ASSERT, "JSONOUTPUT", String.valueOf(responseJson));

            // Проверяем наличие ошибок
            if (responseJson.has("status") && "error".equals(responseJson.getString("status"))) {
                String errorMsg = responseJson.optString("errormessage", "Unknown error");
                throw new IOException("API error: " + errorMsg);
            }

            // Проверяем наличие session key
            if (!responseJson.has("session")) {
                throw new JSONException("Server response doesn't contain session key: " + responseBody);
            }

            return responseJson.getString("session");
        }
    }

    private String uploadImage(String imagePath, String sessionKey) throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        File imageFile = new File(imagePath);

        // Проверка существования файла
        if (!imageFile.exists()) {
            throw new IOException("File not found: " + imagePath);
        }

        JSONObject requestJson = new JSONObject();
        requestJson.put("session", sessionKey);
        requestJson.put("publicly_visible", "n");  // Добавляем рекомендуемые параметры

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("request-json", requestJson.toString())
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .addHeader("Accept", "application/json")  // Добавляем заголовок
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Upload failed: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            return json.getJSONObject("submission").getString("subid");
        }
    }

    public boolean isJobCompleted(String jobId) throws IOException {
        String url = "https://nova.astrometry.net/api/jobs/" + jobId;

        HttpGet request = new HttpGet(url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(request)) {
            String json = EntityUtils.toString(response.getEntity());
            JSONObject jobInfo = new JSONObject(json);
            return "success".equals(jobInfo.getString("status"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public double downloadAndReadFitsFile(Context context, String jobId) throws IOException {
        // URL для скачивания WCS FITS-файла
        String url = "https://nova.astrometry.net/image_rd_file/" + jobId;

        // Создаем временный файл
        File outputFile = new File(context.getExternalFilesDir(null), "image_rd_" + jobId + ".fits");

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
             InputStream is = response.getEntity().getContent();
             FileOutputStream fos = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        FitsApiClient client = new FitsApiClient("https://ekamaemo.pythonanywhere.com/");
        client.findPolaris(outputFile, new FitsApiClient.PolarisCallback() {
            @Override
            public double onSuccess(double polarisY) {
                // Обработка успешного результата
                return polarisY;
            }

            @Override
            public int onError(String message) {
                // Обработка ошибки
                return -1;
            }
        });
        return -1;
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

    public void getInfo(String jobID, String sessionId) {
        String url = "https://nova.astrometry.net/api/jobs/" + jobID + "/info/";
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("session", sessionId) // Важно: добавляем session key в заголовки!
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseData = response.body().string();
            JSONObject json = new JSONObject(responseData);

            // 1. Получаем статус
            String status = json.getString("status");

            // 2. Извлекаем нужные данные для info (пример для WCS-параметров)
            JSONObject infoJson = new JSONObject();

            if (json.has("calibration")) {
                JSONObject calibration = json.getJSONObject("calibration");
                infoJson.put("ra", calibration.optDouble("ra"));
                infoJson.put("dec", calibration.optDouble("dec"));
                infoJson.put("pixscale", calibration.optDouble("pixscale"));
            }

            if (json.has("tags")) {
                infoJson.put("tags", json.getJSONArray("tags"));
            }

            // 3. Конвертируем JSON в строку для сохранения в БД
            String info = infoJson.toString();

            // 4. Обновляем базу данных
            DataBaseHelper databaseHelper = new DataBaseHelper(context);
            databaseHelper.updateJobs(jobID, sessionId, status, info);

        } catch (Exception e) {
            Log.e("Astrometry", "Error fetching job info", e);
            // Обновляем статус на "failed" в случае ошибки
            DataBaseHelper databaseHelper = new DataBaseHelper(context);
            databaseHelper.updateJobs(jobID, sessionId, "failed", "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}