package com.example.photogalleryapp;

import android.app.Activity;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDetailsActivity extends AppCompatActivity {

    private Uri currentImageUri;
    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    onDeleteSuccess();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        String uriString = getIntent().getStringExtra("image_uri");
        if (uriString != null) {
            currentImageUri = Uri.parse(uriString);
            displayImageDetails(currentImageUri);
        }

        MaterialButton btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage() {
        if (currentImageUri == null) return;

        try {
            int rowsDeleted = getContentResolver().delete(currentImageUri, null, null);
            if (rowsDeleted > 0) {
                onDeleteSuccess();
            } else {
                Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException securityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RecoverableSecurityException recoverableSecurityException;
                if (securityException instanceof RecoverableSecurityException) {
                    recoverableSecurityException = (RecoverableSecurityException) securityException;
                } else {
                    throw new RuntimeException(securityException.getMessage(), securityException);
                }
                
                IntentSender intentSender = recoverableSecurityException.getUserAction().getActionIntent().getIntentSender();
                deleteLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
            } else {
                Toast.makeText(this, "Permission denied to delete image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error deleting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void onDeleteSuccess() {
        Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void displayImageDetails(Uri uri) {
        ImageView ivDetailImage = findViewById(R.id.ivDetailImage);
        TextView tvName = findViewById(R.id.tvImageName);
        TextView tvPath = findViewById(R.id.tvImagePath);
        TextView tvSize = findViewById(R.id.tvImageSize);
        TextView tvDate = findViewById(R.id.tvImageDate);

        Glide.with(this).load(uri).into(ivDetailImage);

        String[] projection = {
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN
        };

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                long dateTaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN));

                tvName.setText(name);
                tvPath.setText(path);
                tvSize.setText(formatFileSize(size));
                
                if (dateTaken > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                    tvDate.setText(sdf.format(new Date(dateTaken)));
                } else {
                    tvDate.setText("Unknown");
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
