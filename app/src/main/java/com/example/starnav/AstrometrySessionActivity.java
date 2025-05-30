package com.example.starnav;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AstrometrySessionActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;

    private ImageView viewImagePolarius;
    private ImageView viewImageZenith;
    private String currentPhotoPath;
    private String currentPhotoType; // "zenith" или "polarius"
    private Uri zenithImageUri, polarisImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_session);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 100);
            }
        }


        viewImagePolarius = findViewById(R.id.polarStarImageView);
        viewImageZenith = findViewById(R.id.zenithImageView);

        ImageButton btnAddPhotoZenith = findViewById(R.id.addZenithPhotoButton);
        ImageButton btnAddPhotoPolarius = findViewById(R.id.addPolarStarButton);

        btnAddPhotoZenith.setOnClickListener(v -> selectImage("zenith"));
        btnAddPhotoPolarius.setOnClickListener(v -> selectImage("polarius"));

        Button solveBtn = findViewById(R.id.solveButton);
        solveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    solveImages();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void selectImage(String type) {
        currentPhotoType = type;
        final CharSequence[] options = {"Сделать фото", "Выбрать из галереи", "Отмена"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить фото");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Сделать фото")) {
                dispatchTakePictureIntent();
            } else if (options[item].equals("Выбрать из галереи")) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("Camera Error", "Ошибка создания файла", ex);
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "ASTRO_" + currentPhotoType + "_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void solveImages() throws IOException {
        // 1. Проверка заполнения полей
        if (zenithImageUri == null || polarisImageUri == null) {
            Toast.makeText(this, "Пожалуйста, выберите оба изображения", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Создаем клиента с путями к изображениям
        AstrometryNetClient myClient = new AstrometryNetClient(
                FileUtils.getFilePathFromUri(this, zenithImageUri),
                FileUtils.getFilePathFromUri(this, polarisImageUri)
        );

        // 3. Показываем прогресс
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Обработка фото с камеры
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                if (bitmap != null) {
                    if ("zenith".equals(currentPhotoType)) {
                        viewImageZenith.setImageBitmap(bitmap);
                    } else {
                        viewImagePolarius.setImageBitmap(bitmap);
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_GALLERY) {
                // Обработка выбора из галереи
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                try (Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);
                        Bitmap bitmap = BitmapFactory.decodeFile(picturePath);

                        if ("zenith".equals(currentPhotoType)) {
                            viewImageZenith.setImageBitmap(bitmap);
                        } else {
                            viewImagePolarius.setImageBitmap(bitmap);
                        }
                    }
                }
            }
        }
    }
}