package com.haha.autorecordaudio;

import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private Button btnRecord, btnStop;
    private TextView tvTimer;
    private File currentFile;
    private boolean isRecording = false;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        btnRecord = findViewById(R.id.btn_record);
        btnStop = findViewById(R.id.btn_stop);
        tvTimer = findViewById(R.id.tv_timer);

        btnRecord.setOnClickListener(v -> startCapturePermission());
        btnStop.setOnClickListener(v -> stopRecording());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void startCapturePermission() {
        Intent intent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {

                // 生成文件名并保存到系统下载目录
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                // 确保下载目录存在
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                currentFile = new File(downloadDir, "InternalAudio_" + timeStamp + ".wav");

                // 第一步：先启动前台服务
                Intent serviceIntent = new Intent(this, RecordingService.class);
                serviceIntent.putExtra("output_file", currentFile);
                startForegroundService(serviceIntent);

                // 第二步：延迟一点时间，让服务启动后再传递 MediaProjection
                new android.os.Handler().postDelayed(() -> {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);

                    if (mediaProjection != null) {
                        RecordingService.mediaProjection = mediaProjection;

                        isRecording = true;
                        startTime = System.currentTimeMillis();
                        btnRecord.setEnabled(false);
                        btnStop.setEnabled(true);

                        // 启动计时器
                        new android.os.Handler().postDelayed(this::updateTimer, 1000);

                        Toast.makeText(this, "开始录制内部音频...\n文件将保存到下载目录", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "MediaProjection 获取失败", Toast.LENGTH_SHORT).show();
                    }
                }, 600);   // 延迟600毫秒，确保服务已启动

            } else {
                Toast.makeText(this, "用户取消了录制权限", Toast.LENGTH_SHORT).show();
                btnRecord.setEnabled(true);
            }
        }
    }

    private void startRecording() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        currentFile = new File(getExternalFilesDir(null), "internal_" + timeStamp + ".wav");

        // 创建 Intent 并传递必要数据
        Intent serviceIntent = new Intent(this, RecordingService.class);
        serviceIntent.putExtra("output_file", currentFile);           // File 可以直接传递

        // MediaProjection 不能直接 putExtra，我们用 startForegroundService + bind 的方式处理
        // 但为了简单，这里采用最常用且稳定的方式：
        if (mediaProjection != null) {
            // 把 MediaProjection 绑定到服务（推荐方式）
            Intent intent = new Intent(this, RecordingService.class);
            intent.putExtra("output_file", currentFile);

            // 使用 startForegroundService 并在服务中通过静态变量临时传递（简单可靠）
            RecordingService.mediaProjection = mediaProjection;   // ← 关键修改

            startService(intent);

            isRecording = true;
            startTime = System.currentTimeMillis();
            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);

            new android.os.Handler().postDelayed(this::updateTimer, 1000);
            Toast.makeText(this, "开始录制内部音频...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "MediaProjection 未初始化", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTimer() {
        if (!isRecording) return;
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        tvTimer.setText(String.format("%02d:%02d", elapsed / 60, elapsed % 60));
        new android.os.Handler().postDelayed(this::updateTimer, 1000);
    }

    private void stopRecording() {
        stopService(new Intent(this, RecordingService.class));
        isRecording = false;
        btnRecord.setEnabled(true);
        btnStop.setEnabled(false);
        Toast.makeText(this, "录音已保存: " + currentFile.getName(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        RecordingService.mediaProjection = null;   // 清理
        super.onDestroy();
    }

}