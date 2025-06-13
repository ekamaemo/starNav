package com.example.starnav;

import okhttp3.*;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;

public class FitsApiClient {
    private final OkHttpClient client = new OkHttpClient();
    private final String serverUrl;

    public FitsApiClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void findPolaris(File fitsFile, PolarisCallback callback) {
        // 1. Создаем тело запроса
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        fitsFile.getName(),
                        RequestBody.create(fitsFile, MediaType.get("application/fits"))
                )
                .build();

        // 2. Формируем запрос
        Request request = new Request.Builder()
                .url(serverUrl + "/api/find-polaris")
                .post(requestBody)
                .build();

        // 3. Отправляем асинхронно
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String json = response.body().string();
                    JSONObject jsonResponse = new JSONObject(json);

                    if (response.isSuccessful()) {
                        if (jsonResponse.has("polaris_y")) {
                            double y = jsonResponse.getDouble("polaris_y");
                            callback.onSuccess(y);
                        } else {
                            callback.onError("Polaris not found");
                        }
                    } else {
                        callback.onError(jsonResponse.getString("message"));
                    }
                } catch (Exception e) {
                    callback.onError("JSON parsing error");
                }
            }
        });
    }

    public interface PolarisCallback {
        double onSuccess(double polarisY);
        int onError(String message);
    }
}