package com.example.photogalleryapp;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private List<Uri> imageUris = new ArrayList<>();
    private GalleryAdapter adapter;
    private TextView tvImageCount, tvPictures;
    private String bucketId;
    private String folderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        bucketId = getIntent().getStringExtra("bucket_id");
        folderName = getIntent().getStringExtra("folder_name");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvPictures = findViewById(R.id.tvPictures);
        if (folderName != null) {
            tvPictures.setText(folderName);
        }

        tvImageCount = findViewById(R.id.tvImageCount);
        RecyclerView rvGallery = findViewById(R.id.rvGallery);
        rvGallery.setLayoutManager(new GridLayoutManager(this, 3));
        
        adapter = new GalleryAdapter(imageUris, position -> {
            Intent intent = new Intent(GalleryActivity.this, ImageDetailsActivity.class);
            intent.putExtra("image_uri", imageUris.get(position).toString());
            startActivity(intent);
        });
        rvGallery.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImagesFromFolder();
    }

    private void loadImagesFromFolder() {
        imageUris.clear();
        String[] projection = new String[]{
                MediaStore.Images.Media._ID
        };
        
        String selection = null;
        String[] selectionArgs = null;
        
        if (bucketId != null) {
            selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
            selectionArgs = new String[]{bucketId};
        }

        String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC";

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                do {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    imageUris.add(contentUri);
                } while (cursor.moveToNext());
            }
        }

        adapter.notifyDataSetChanged();
        tvImageCount.setText(imageUris.size() + " images");
    }

    private static class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
        private final List<Uri> images;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(int position);
        }

        GalleryAdapter(List<Uri> images, OnItemClickListener listener) {
            this.images = images;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Glide.with(holder.imageView.getContext())
                    .load(images.get(position))
                    .centerCrop()
                    .into(holder.imageView);

            holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.ivGalleryImage);
            }
        }
    }
}
