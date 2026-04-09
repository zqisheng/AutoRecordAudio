package com.haha.autorecordaudio;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class ClipActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView tvFileName, tvTrimInfo, tvCurrentTime;
    private Button btnPlay, btnSetStart, btnSetEnd, btnExportWav, btnBack;

    private String filePath;        // 原文件路径
    private int startMs = 0;
    private int endMs = 0;
    private Handler handler = new Handler();

    private static final int REQUEST_CODE_CREATE_FILE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clip);

        filePath = getIntent().getStringExtra("file_path");
        if (filePath == null) {
            Toast.makeText(this, "文件路径错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvFileName = findViewById(R.id.tv_file_name);
        tvTrimInfo = findViewById(R.id.tv_trim_info);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        seekBar = findViewById(R.id.seek_bar);
        btnPlay = findViewById(R.id.btn_play);
        btnSetStart = findViewById(R.id.btn_set_start);
        btnSetEnd = findViewById(R.id.btn_set_end);
        btnExportWav = findViewById(R.id.btn_export_wav);
        btnBack = findViewById(R.id.btn_back_clip);

        tvFileName.setText(new File(filePath).getName());

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            endMs = mediaPlayer.getDuration();
            seekBar.setMax(endMs);
            updateSeekBar();
        } catch (Exception e) {
            Log.e("ClipActivity", "播放器初始化失败", e);
            Toast.makeText(this, "无法播放该文件", Toast.LENGTH_SHORT).show();
        }

        btnPlay.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlay.setText("▶  播放");
            } else {
                mediaPlayer.start();
                btnPlay.setText("⏸  暂停");
            }
        });

        btnSetStart.setOnClickListener(v -> {
            startMs = mediaPlayer.getCurrentPosition();
            updateTrimInfo();
        });

        btnSetEnd.setOnClickListener(v -> {
            endMs = mediaPlayer.getCurrentPosition();
            if (endMs <= startMs) {
                Toast.makeText(this, "终点必须大于起点", Toast.LENGTH_SHORT).show();
                return;
            }
            updateTrimInfo();
        });

        btnExportWav.setOnClickListener(v -> showSaveFilePicker());

        btnBack.setOnClickListener(v -> finish());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) mediaPlayer.seekTo(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateSeekBar() {
        if (mediaPlayer != null) {
            int current = mediaPlayer.getCurrentPosition();
            int total = mediaPlayer.getDuration();
            tvCurrentTime.setText(String.format("%02d:%02d / %02d:%02d",
                    current / 60000, (current % 60000) / 1000,
                    total / 60000, (total % 60000) / 1000));
            seekBar.setProgress(current);
        }
        handler.postDelayed(this::updateSeekBar, 200);
    }

    private void updateTrimInfo() {
        tvTrimInfo.setText(String.format("起点: %d 秒    终点: %d 秒",
                startMs / 1000, endMs / 1000));
    }

    /**
     * 弹出系统保存文件对话框，让用户选择目录和文件名
     */
    private void showSaveFilePicker() {
        if (startMs >= endMs) {
            Toast.makeText(this, "请先正确设置起点和终点", Toast.LENGTH_SHORT).show();
            return;
        }

        File originalFile = new File(filePath);
        String defaultName = originalFile.getName().replace(".wav", "_clip.wav");

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/x-wav");           // WAV 文件类型
        intent.putExtra(Intent.EXTRA_TITLE, defaultName);   // 默认文件名

        startActivityForResult(intent, REQUEST_CODE_CREATE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CREATE_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                new Thread(() -> exportToSelectedUri(uri)).start();
            }
        }
    }

    /**
     * 把剪辑后的音频写入用户选择的路径
     */
    private void exportToSelectedUri(Uri targetUri) {
        try {
            trimWavToUri(targetUri);
            runOnUiThread(() ->
                    Toast.makeText(this, "✅ 导出成功！\n已保存到您选择的位置", Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            Log.e("ClipActivity", "导出失败", e);
            runOnUiThread(() ->
                    Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * 核心剪辑方法：把剪辑后的数据写入用户选择的 Uri
     */
    private void trimWavToUri(Uri targetUri) throws IOException {
        final int HEADER_SIZE = 44;
        File inputFile = new File(filePath);

        try (RandomAccessFile raf = new RandomAccessFile(inputFile, "r");
             OutputStream outputStream = getContentResolver().openOutputStream(targetUri)) {

            if (outputStream == null) throw new IOException("无法打开输出流");

            byte[] header = new byte[HEADER_SIZE];
            raf.readFully(header);

            int sampleRate = (header[24] & 0xff) | ((header[25] & 0xff) << 8) |
                    ((header[26] & 0xff) << 16) | ((header[27] & 0xff) << 24);
            int channels = header[22];
            int bytesPerSample = header[34] / 8;

            int bytesPerSecond = sampleRate * channels * bytesPerSample;

            long startBytes = HEADER_SIZE + (long) (startMs / 1000.0 * bytesPerSecond);
            long endBytes = HEADER_SIZE + (long) (endMs / 1000.0 * bytesPerSecond);
            long dataLength = endBytes - startBytes;

            // 写入新头
            long totalDataLen = dataLength;
            long totalFileLen = totalDataLen + 36;
            writeWavHeader(outputStream, totalDataLen, totalFileLen, sampleRate, channels, (long) bytesPerSecond * 2);

            // 复制音频数据
            raf.seek(startBytes);
            byte[] buffer = new byte[4096];
            long remaining = dataLength;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = raf.read(buffer, 0, toRead);
                if (read <= 0) break;
                outputStream.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    private void writeWavHeader(OutputStream out, long totalAudioLen, long totalDataLen,
                                long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        // RIFF
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff); header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff); header[7] = (byte) ((totalDataLen >> 24) & 0xff);

        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';

        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff); header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff); header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff); header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff); header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * channels); header[33] = 0;
        header[34] = 16; header[35] = 0;

        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff); header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff); header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) mediaPlayer.release();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}