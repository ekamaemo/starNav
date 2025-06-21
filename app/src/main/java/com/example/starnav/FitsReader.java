package com.example.starnav;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import nom.tam.fits.Fits;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.TableHDU;
import nom.tam.util.Cursor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONArray;
import okhttp3.Response;

public class FitsReader {
    public static double getPolarisY(String jobId) throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Формируем URL запроса
        String url = "http://nova.astrometry.net/api/jobs/" + jobId + "/annotations/";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);

            // Проверяем наличие аннотаций
            if (!json.has("annotations")) {
                throw new IOException("No annotations in response");
            }

            JSONArray annotations = json.getJSONArray("annotations");

            // Ищем Polaris в аннотациях
            for (int i = 0; i < annotations.length(); i++) {
                JSONObject annotation = annotations.getJSONObject(i);
                if (annotation.has("names")) {
                    JSONArray names = annotation.getJSONArray("names");

                    for (int j = 0; j < names.length(); j++) {
                        String name = names.getString(j);

                        // Проверяем разные варианты названия Polaris
                        if (name.contains("Polaris")) {

                            // Возвращаем Y-координату
                            return annotation.getDouble("pixely");
                        }
                    }
                }
            }

            // Если Polaris не найден
            return -1;

        } catch (JSONException e) {
            throw new IOException("Failed to parse JSON response", e);
        }
    }
}