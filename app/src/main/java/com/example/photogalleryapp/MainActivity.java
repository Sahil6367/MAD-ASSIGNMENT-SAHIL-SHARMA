package com.example.photogalleryapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Uri imageUri;

    private ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    // Decide which action to take based on what was requested
                } else {
                    Toast.makeText(this, "Permissions denied. Please grant permissions to use this feature.", Toast.LENGTH_SHORT).show();
                }
            });

    private ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
                if (isSuccess) {
                    Toast.makeText(this, "Photo saved successfully!", Toast.LENGTH_SHORT).show();
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(imageUri);
                    sendBroadcast(mediaScanIntent);
                } else {
                    if (imageUri != null) {
                        getContentResolver().delete(imageUri, null, null);
                    }
                    Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> folderPickerLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        // Persist permissions
                        getContentResolver().takePersistableUriPermission(treeUri, 
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        
                        // Navigate to GalleryActivity for the selected folder
                        Intent intent = new Intent(this, GalleryActivity.class);
                        intent.putExtra("folder_uri", treeUri.toString());
                        startActivity(intent);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialButton btnTakePhoto = findViewById(R.id.btnTakePhoto);
        MaterialButton btnChooseFolder = findViewById(R.id.btnChooseFolder);
        MaterialButton btnViewAllFolders = findViewById(R.id.btnViewAllFolders);

        btnTakePhoto.setOnClickListener(v -> checkPermissionsAndOpenCamera());

        btnChooseFolder.setOnClickListener(v -> {
            // This opens the system folder picker (SAF) shown in your second screenshot
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            folderPickerLauncher.launch(intent);
        });

        if (btnViewAllFolders != null) {
            btnViewAllFolders.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, FolderListActivity.class);
                startActivity(intent);
            });
        }
    }

    private void checkPermissionsAndOpenCamera() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        }

        if (hasPermissions(permissions)) {
            openCamera();
        } else {
            permissionLauncher.launch(permissions);
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoGalleryApp");
        }

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        
        if (imageUri != null) {
            takePictureLauncher.launch(imageUri);
        }
    }
}
