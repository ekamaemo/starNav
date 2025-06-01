package com.example.starnav;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AstrometrySessionActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;

    private BottomNavigationView bottomNav;

    private ImageView viewImagePolarius;
    private ImageView viewImageZenith;
    private String currentPhotoPath;
    private String currentPhotoType; // "zenith" или "polarius"
    private Uri zenithImageUri, polarisImageUri;
    public static String API_KEY;
    public static String email;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_session);
        bottomNav = findViewById(R.id.bottomNavigation);

        bottomNav.setSelectedItemId(R.id.nav_camera);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        API_KEY = prefs.getString("api_key", "no");
        email = prefs.getString("email", "noname@noname.com");

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
                    Log.println(Log.ASSERT, "false", "чтото не так");
                    throw new RuntimeException(e);
                }
            }
        });

        // Навигация
        bottomNav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                case R.id.nav_history:
                    startActivity(new Intent(this, HistoryOfSessionsActivity.class));
                    return true;
                case R.id.nav_profile:
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
            }
            return false;
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
                AstrometrySessionActivity.this,
                email,
                API_KEY,
                FileUtils.getFilePathFromUri(this, zenithImageUri),
                FileUtils.getFilePathFromUri(this, polarisImageUri)
        );
        Log.println(Log.ASSERT, "client", "Отправлено");
        myClient.processImages();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Обработка фото с камеры
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                File file = new File(currentPhotoPath);
                Uri photoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        file);

                if ("zenith".equals(currentPhotoType)) {
                    viewImageZenith.setImageBitmap(bitmap);
                    zenithImageUri = photoUri; // Используем созданный URI
                } else {
                    viewImagePolarius.setImageBitmap(bitmap);
                    polarisImageUri = photoUri; // Используем созданный URI
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
                            zenithImageUri = data.getData();
                        } else {
                            polarisImageUri = data.getData();
                            viewImagePolarius.setImageBitmap(bitmap);
                        }
                    }
                }
            }
        }
    }
}