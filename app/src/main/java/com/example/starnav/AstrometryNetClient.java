package com.example.starnav;

import okhttp3.*;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class AstrometryNetClient {
    private static final String API_KEY = "mwlmuhpsdvujnjrx";
    private static final String UPLOAD_URL = "http://nova.astrometry.net/api/upload";
    private static final String LOGIN_URL = "http://nova.astrometry.net/api/login";
    private static String[] imagePaths;


    public AstrometryNetClient(String pathZenith, String pathPolarius){
        imagePaths = new String[]{pathZenith, pathPolarius};
    }


    public void main() {
        try {
            String sessionKey = getSessionKey();
            if (sessionKey == null) {
                System.out.println("Не удалось получить сессионный ключ");
                return;
            }
            for (String path : imagePaths) {
                System.out.println("\nОтправка изображения: " + path);
                String response = uploadImage(path, sessionKey);
                processResponse(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getSessionKey() throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        json.put("apikey", API_KEY);

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseString = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseString);
            return jsonResponse.optString("session", null);
        }
    }

    private static String uploadImage(String imagePath, String sessionKey) throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient();
        File imageFile = new File(imagePath);

        JSONObject requestJson = new JSONObject();
        requestJson.put("session", sessionKey);
        requestJson.put("publicly_visible", "n");
        requestJson.put("allow_modifications", "n");
        requestJson.put("scale_units", "degwidth");
        requestJson.put("scale_type", "ul");
        requestJson.put("scale_upper", 180.0);

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
            return response.body().string();
        }
    }

    private static void processResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            System.out.println("Получен ответ от сервера:\n" + jsonResponse.toString(2));

            if (jsonResponse.optString("status", "").equals("success")) {
                System.out.println("Номер сессии (subid): " + jsonResponse.optInt("subid"));
                System.out.println("ID задания: " + jsonResponse.optInt("jobid"));
            } else {
                System.out.println("Ошибка: " + jsonResponse.optString("errormessage", "Неизвестная ошибка"));
            }
        } catch (Exception e) {
            System.out.println("Ошибка при обработке ответа: " + e.getMessage());
        }
    }
}