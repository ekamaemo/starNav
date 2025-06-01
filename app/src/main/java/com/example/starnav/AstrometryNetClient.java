package com.example.starnav;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.*;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.time.LocalDate;
import java.util.Calendar;
import android.app.Application;
import android.util.Log;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.CloseableHttpResponse;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpGet;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.CloseableHttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClients;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.util.EntityUtils;

import java.util.Date;

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
        new Thread(() -> {
            try {
                String sessionKey = getSessionKey();
                if (sessionKey == null) {
                    Log.e("AstrometryNet", "Failed to get session key");
                    return;
                }

                DataBaseHelper databaseHelper = new DataBaseHelper(context);

                for (String path : imagePaths) {
                    try {
                        String subId = uploadImage(path, sessionKey);
                        if (subId != null) {
                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                            databaseHelper.insertDataSessions(email, subId, timeStamp);
                            Log.i("AstrometryNet", "Image uploaded, subId: " + subId);
                        }
                    } catch (Exception e) {
                        Log.e("AstrometryNet", "Error uploading image", e);
                    }
                }
            } catch (Exception e) {
                Log.e("AstrometryNet", "Error processing images", e);
            }
        }).start();
    }

    private String getSessionKey() throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        json.put("apikey", API_KEY);

        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseString = response.body().string();
            return new JSONObject(responseString).optString("session", null);
        }
    }

    private String uploadImage(String imagePath, String sessionKey) throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        File imageFile = new File(imagePath);

        JSONObject requestJson = new JSONObject();
        requestJson.put("session", sessionKey);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("request-json", requestJson.toString())
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
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

    public File downloadFitsFile(Context context, String jobId) throws IOException {
        // URL для скачивания WCS FITS-файла
        String url = "https://nova.astrometry.net/image_rd_file/" + jobId;

        // Создаем временный файл
        File outputFile = new File(context.getExternalFilesDir(null), "wcs_" + jobId + ".fits");

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

        return outputFile;
    }
}