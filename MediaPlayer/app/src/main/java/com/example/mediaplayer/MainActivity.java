package com.example.mediaplayer;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // -- UI References ----------------------------------------------------------
    private TextView tvFileName, tvStatus, tvTime;
    private View viewStatusDot;
    private SeekBar seekBar;
    private VideoView videoView;
    private LinearLayout musicModeLayout, equalizerLayout;

    // Control buttons
    private ImageButton btnPlay, btnPause, btnStop, btnRestart, btnRewind, btnForward;

    // Source buttons
    private Button btnOpenFile, btnStreamUrl;

    // Stream URL dialog
    private View dialogOverlay;
    private CardView streamDialog;
    private EditText etStreamUrl;
    private Button btnCancelStream, btnLoadStream;

    // -- Playback State ---------------------------------------------------------
    private MediaPlayer mediaPlayer;   // Used for audio only
    private Uri currentUri;
    private boolean isAudio = false;
    private boolean isPrepared = false;
    private boolean isUserSeeking = false;

    // -- Activity Result Launcher ----------------------------------------------
    private final ActivityResultLauncher<Intent> pickFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri == null) return;

                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}

                    String name = getFileNameFromUri(uri);
                    loadMedia(uri, name);
                }
            }
    );

    // -- SeekBar Handler --------------------------------------------------------
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private final Runnable seekRunnable = new Runnable() {
        @Override
        public void run() {
            updateSeekBar();
            if (isPrepared) {
                seekHandler.postDelayed(this, 1000);
            }
        }
    };

    // -- Constants --------------------------------------------------------------
    private static final int SEEK_STEP = 10000; // 10 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupSeekBar();
        setupButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseMedia();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseAll();
    }

    private void bindViews() {
        tvFileName      = findViewById(R.id.tvFileName);
        tvStatus        = findViewById(R.id.tvStatus);
        tvTime          = findViewById(R.id.tvTime);
        viewStatusDot   = findViewById(R.id.viewStatusDot);
        seekBar         = findViewById(R.id.seekBar);
        videoView       = findViewById(R.id.videoView);
        musicModeLayout = findViewById(R.id.musicModeLayout);
        equalizerLayout = findViewById(R.id.equalizerLayout);

        btnPlay         = findViewById(R.id.btnPlay);
        btnPause        = findViewById(R.id.btnPause);
        btnStop         = findViewById(R.id.btnStop);
        btnRestart      = findViewById(R.id.btnRestart);
        btnRewind       = findViewById(R.id.btnRewind);
        btnForward      = findViewById(R.id.btnForward);

        btnOpenFile     = findViewById(R.id.btnOpenFile);
        btnStreamUrl    = findViewById(R.id.btnStreamUrl);

        dialogOverlay   = findViewById(R.id.dialogOverlay);
        streamDialog    = findViewById(R.id.streamDialog);
        etStreamUrl     = findViewById(R.id.etStreamUrl);
        btnCancelStream = findViewById(R.id.btnCancelStream);
        btnLoadStream   = findViewById(R.id.btnLoadStream);
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    updateTimeText(progress, sb.getMax());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                if (isPrepared) {
                    int progress = sb.getProgress();
                    if (isAudio && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                    } else if (!isAudio && videoView != null) {
                        videoView.seekTo(progress);
                    }
                }
                isUserSeeking = false;
            }
        });
    }

    private void setupButtons() {
        btnOpenFile.setOnClickListener(v -> openFilePicker());
        btnStreamUrl.setOnClickListener(v -> showStreamDialog());

        btnPlay.setOnClickListener(v -> {
            if (!isPrepared && currentUri == null) {
                toast(getString(R.string.please_open_file));
                return;
            }
            if (isAudio && mediaPlayer != null) {
                mediaPlayer.start();
                setStatus(getString(R.string.playing));
            } else if (!isAudio && videoView != null) {
                videoView.start();
                setStatus(getString(R.string.playing));
            }
        });

        btnPause.setOnClickListener(v -> pauseMedia());

        btnStop.setOnClickListener(v -> {
            releaseAll();
            tvFileName.setText(R.string.no_media_selected);
            setStatus(getString(R.string.stopped));
            seekBar.setProgress(0);
            tvTime.setText(R.string.default_time);
            currentUri = null;
        });

        btnRestart.setOnClickListener(v -> {
            if (isPrepared) {
                if (isAudio && mediaPlayer != null) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                } else if (!isAudio && videoView != null) {
                    videoView.seekTo(0);
                    videoView.start();
                }
                setStatus(getString(R.string.playing));
            }
        });

        btnRewind.setOnClickListener(v -> {
            if (isPrepared) {
                int pos = isAudio ? (mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0)
                                  : videoView.getCurrentPosition();
                pos = Math.max(pos - SEEK_STEP, 0);
                if (isAudio && mediaPlayer != null) {
                    mediaPlayer.seekTo(pos);
                } else if (!isAudio) {
                    videoView.seekTo(pos);
                }
                seekBar.setProgress(pos);
                updateTimeText(pos, seekBar.getMax());
            }
        });

        btnForward.setOnClickListener(v -> {
            if (isPrepared) {
                int pos = isAudio ? (mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0)
                                  : videoView.getCurrentPosition();
                int duration = isAudio ? (mediaPlayer != null ? mediaPlayer.getDuration() : 0)
                                       : videoView.getDuration();
                if (duration > 0) {
                    pos = Math.min(pos + SEEK_STEP, duration);
                    if (isAudio && mediaPlayer != null) {
                        mediaPlayer.seekTo(pos);
                    } else if (!isAudio) {
                        videoView.seekTo(pos);
                    }
                    seekBar.setProgress(pos);
                    updateTimeText(pos, duration);
                }
            }
        });

        btnCancelStream.setOnClickListener(v -> hideStreamDialog());
        dialogOverlay.setOnClickListener(v -> hideStreamDialog());
        btnLoadStream.setOnClickListener(v -> {
            String url = etStreamUrl.getText().toString().trim();
            if (url.isEmpty()) { toast(getString(R.string.please_enter_url)); return; }
            hideStreamDialog();
            loadMedia(Uri.parse(url), getFileNameFromUrl(url));
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"video/*", "audio/*"});
        pickFileLauncher.launch(intent);
    }

    private void loadMedia(Uri uri, String name) {
        currentUri = uri;
        tvFileName.setText(name);
        releaseAll();

        String mime = getContentResolver().getType(uri);
        isAudio = mime != null ? mime.startsWith("audio") : isAudioUri(uri.toString());

        if (isAudio) {
            loadAudio(uri);
        } else {
            loadVideo(uri);
        }
    }

    private void loadVideo(Uri uri) {
        videoView.setVisibility(View.VISIBLE);
        musicModeLayout.setVisibility(View.GONE);
        equalizerLayout.setVisibility(View.VISIBLE);

        setStatus(getString(R.string.buffering));
        videoView.setOnPreparedListener(mp -> {
            isPrepared = true;
            videoView.start();
            setStatus(getString(R.string.playing));
            int duration = videoView.getDuration();
            if (duration > 0) {
                seekBar.setMax(duration);
            }
            startSeekBarUpdate();
        });
        videoView.setOnCompletionListener(mp -> setStatus(getString(R.string.completed)));
        videoView.setOnErrorListener((mp, what, extra) -> {
            setStatus(getString(R.string.error));
            toast(getString(R.string.error_playing_video, what));
            return true;
        });
        videoView.setVideoURI(uri);
    }

    private void loadAudio(Uri uri) {
        videoView.setVisibility(View.GONE);
        musicModeLayout.setVisibility(View.VISIBLE);
        equalizerLayout.setVisibility(View.GONE);

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                mediaPlayer.start();
                setStatus(getString(R.string.playing));
                int duration = mediaPlayer.getDuration();
                if (duration > 0) {
                    seekBar.setMax(duration);
                }
                startSeekBarUpdate();
            });
            mediaPlayer.setOnCompletionListener(mp -> setStatus(getString(R.string.completed)));
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                setStatus(getString(R.string.error));
                toast(getString(R.string.playback_error, what));
                return true;
            });
            mediaPlayer.prepareAsync();
            setStatus(getString(R.string.buffering));
        } catch (Exception e) {
            setStatus(getString(R.string.error));
            toast(getString(R.string.cannot_open_file, e.getMessage()));
        }
    }

    private void updateSeekBar() {
        if (!isPrepared || isUserSeeking) return;

        int current = 0;
        int total = 0;

        try {
            if (isAudio && mediaPlayer != null) {
                current = mediaPlayer.getCurrentPosition();
                total = mediaPlayer.getDuration();
            } else if (!isAudio && videoView != null) {
                current = videoView.getCurrentPosition();
                total = videoView.getDuration();
            }
        } catch (Exception e) {
            return;
        }

        if (total > 0) {
            if (seekBar.getMax() != total) {
                seekBar.setMax(total);
            }
            seekBar.setProgress(current);
            updateTimeText(current, total);
        }
    }

    private void updateTimeText(int current, int total) {
        if (total > 0) {
            tvTime.setText(String.format(Locale.getDefault(), "%s / %s", formatTime(current), formatTime(total)));
        } else {
            tvTime.setText(String.format(Locale.getDefault(), "%s / Live", formatTime(current)));
        }
    }

    private void startSeekBarUpdate() {
        seekHandler.removeCallbacks(seekRunnable);
        seekHandler.post(seekRunnable);
    }

    private void pauseMedia() {
        if (isAudio && mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            setStatus(getString(R.string.paused));
        } else if (!isAudio && videoView != null && videoView.isPlaying()) {
            videoView.pause();
            setStatus(getString(R.string.paused));
        }
    }

    private String formatTime(int ms) {
        int totalSec = Math.max(0, ms / 1000);
        return String.format(Locale.getDefault(), "%d:%02d", totalSec / 60, totalSec % 60);
    }

    private void setStatus(String status) {
        tvStatus.setText(status);
        if (viewStatusDot != null) {
            int dotDrawable = R.drawable.bg_green_dot;
            if (status.equals(getString(R.string.error))) {
                dotDrawable = R.drawable.bg_red_dot;
            } else if (status.equals(getString(R.string.paused)) || status.equals(getString(R.string.buffering))) {
                dotDrawable = R.drawable.bg_orange_dot;
            } else if (status.equals(getString(R.string.stopped)) || status.equals(getString(R.string.no_media))) {
                dotDrawable = R.drawable.bg_grey_dot;
            }
            viewStatusDot.setBackground(ContextCompat.getDrawable(this, dotDrawable));
        }
    }

    private void releaseAll() {
        seekHandler.removeCallbacks(seekRunnable);
        isPrepared = false;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }

    private void showStreamDialog() {
        dialogOverlay.setVisibility(View.VISIBLE);
        streamDialog.setVisibility(View.VISIBLE);
        etStreamUrl.requestFocus();
    }

    private void hideStreamDialog() {
        dialogOverlay.setVisibility(View.GONE);
        streamDialog.setVisibility(View.GONE);
    }

    private boolean isAudioUri(String uriStr) {
        String lower = uriStr.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3") || lower.endsWith(".aac") || lower.endsWith(".ogg") || lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".m4a");
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "Unknown";
    }

    private String getFileNameFromUrl(String url) {
        int slash = url.lastIndexOf('/');
        return slash >= 0 && slash < url.length() - 1 ? url.substring(slash + 1) : url;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
