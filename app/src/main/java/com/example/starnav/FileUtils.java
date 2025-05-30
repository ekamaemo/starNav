package com.example.starnav;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    /**
     * Конвертирует Uri в файл и возвращает абсолютный путь
     */
    public static String getFilePathFromUri(Context context, Uri uri) throws IOException {
        File file = uriToFile(context, uri);
        return file.getAbsolutePath();
    }

    private static File uriToFile(Context context, Uri uri) throws IOException {
        File tempFile = File.createTempFile(
                "astrometry_temp_",
                ".jpg",
                context.getCacheDir()
        );

        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }
}