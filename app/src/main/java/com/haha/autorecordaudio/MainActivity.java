package com.haha.autorecordaudio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private TextView tvPermissionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStartRecord = findViewById(R.id.btn_start_record);
        Button btnMyRecordings = findViewById(R.id.btn_my_recordings);
        tvPermissionStatus = findViewById(R.id.tv_permission_status);
        TextView linkTextView = findViewById(R.id.tv_link);

        linkTextView.setOnClickListener(v -> {
            // 这里打开链接
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zqisheng/AutoRecordAudio"));
            startActivity(intent);
        });
        // 检查必要权限
        checkRequiredPermissions();

        // 开始录制按钮
        btnStartRecord.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                startActivity(new Intent(this, RecordActivity.class));
            } else {
                Toast.makeText(this, "请先授予所有必要权限", Toast.LENGTH_SHORT).show();
                checkRequiredPermissions();
            }
        });

        // 我的录音库按钮
        btnMyRecordings.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                startActivity(new Intent(this, LibraryActivity.class));
            } else {
                Toast.makeText(this, "请先授予所有必要权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 检查所有必要权限
     */
    private void checkRequiredPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            tvPermissionStatus.setText("✅ 所有权限已授予，可以正常使用");
            tvPermissionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvPermissionStatus.setText("⚠️ 需要以下权限：\n• 录音权限\n• 存储权限");
            tvPermissionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

            // 请求权限
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    private boolean hasAllPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                tvPermissionStatus.setText("✅ 所有权限已授予，可以正常使用");
                tvPermissionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                Toast.makeText(this, "权限已全部授予", Toast.LENGTH_SHORT).show();
            } else {
                tvPermissionStatus.setText("❌ 部分权限被拒绝，应用部分功能无法使用");
                tvPermissionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                Toast.makeText(this, "请授予录音和存储权限，否则无法使用", Toast.LENGTH_LONG).show();
            }
        }
    }
}