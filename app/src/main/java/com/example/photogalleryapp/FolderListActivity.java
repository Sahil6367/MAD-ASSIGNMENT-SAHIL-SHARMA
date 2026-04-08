package com.example.photogalleryapp;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FolderListActivity extends AppCompatActivity {

    private List<FolderModel> folderList = new ArrayList<>();
    private FolderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_list);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rvFolders = findViewById(R.id.rvFolders);
        rvFolders.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new FolderAdapter(folderList, folder -> {
            Intent intent = new Intent(this, GalleryActivity.class);
            intent.putExtra("bucket_id", folder.bucketId);
            intent.putExtra("folder_name", folder.name);
            startActivity(intent);
        });
        rvFolders.setAdapter(adapter);

        loadFolders();
    }

    private void loadFolders() {
        folderList.clear();
        Map<String, FolderModel> folderMap = new HashMap<>();

        String[] projection = {
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATA
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
                projection, 
                null, 
                null, 
                MediaStore.Images.Media.DATE_MODIFIED + " DESC")) {
            
            if (cursor != null && cursor.moveToFirst()) {
                int bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
                int bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                do {
                    String bucketId = cursor.getString(bucketIdColumn);
                    String name = cursor.getString(bucketNameColumn);
                    String path = cursor.getString(dataColumn);

                    if (!folderMap.containsKey(bucketId)) {
                        folderMap.put(bucketId, new FolderModel(name, bucketId, path, 1));
                    } else {
                        folderMap.get(bucketId).count++;
                    }
                } while (cursor.moveToNext());
            }
        }
        folderList.addAll(folderMap.values());
        adapter.notifyDataSetChanged();
    }

    private static class FolderModel {
        String name;
        String bucketId;
        String firstImagePath;
        int count;

        FolderModel(String name, String bucketId, String firstImagePath, int count) {
            this.name = name;
            this.bucketId = bucketId;
            this.firstImagePath = firstImagePath;
            this.count = count;
        }
    }

    private static class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {
        private final List<FolderModel> folders;
        private final OnFolderClickListener listener;

        interface OnFolderClickListener {
            void onFolderClick(FolderModel folder);
        }

        FolderAdapter(List<FolderModel> folders, OnFolderClickListener listener) {
            this.folders = folders;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FolderModel folder = folders.get(position);
            holder.tvName.setText(folder.name);
            holder.tvCount.setText(folder.count + " images");
            
            Glide.with(holder.ivThumbnail.getContext())
                    .load(new File(folder.firstImagePath))
                    .centerCrop()
                    .placeholder(R.drawable.ic_folder)
                    .into(holder.ivThumbnail);

            holder.itemView.setOnClickListener(v -> listener.onFolderClick(folder));
        }

        @Override
        public int getItemCount() {
            return folders.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCount;
            ImageView ivThumbnail;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvFolderName);
                tvCount = itemView.findViewById(R.id.tvFolderCount);
                ivThumbnail = itemView.findViewById(R.id.ivFolderThumbnail);
            }
        }
    }
}
