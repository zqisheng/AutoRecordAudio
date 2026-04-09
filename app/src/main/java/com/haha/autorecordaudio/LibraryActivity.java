package com.haha.autorecordaudio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;

public class LibraryActivity extends AppCompatActivity {

    private ArrayList<String> fileNames = new ArrayList<>();
    private ArrayList<File> fileList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final int REQUEST_DELETE = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        ListView listView = findViewById(R.id.list_recordings);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        listView.setAdapter(adapter);

        loadRecordingFiles();

        // 短按 → 进入剪辑
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= fileList.size()) return;
            File selectedFile = fileList.get(position);
            Intent intent = new Intent(this, ClipActivity.class);
            intent.putExtra("file_path", selectedFile.getAbsolutePath());
            startActivity(intent);
        });

        // 长按 → 删除（使用 SAF 方式）
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position >= fileList.size()) return true;
            File fileToDelete = fileList.get(position);
            showDeleteDialog(fileToDelete);
            return true;
        });
    }

    private void loadRecordingFiles() {
        fileNames.clear();
        fileList.clear();

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = downloadDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));

        if (files != null && files.length > 0) {
            for (File f : files) {
                fileNames.add(f.getName());
                fileList.add(f);
            }
        } else {
            fileNames.add("（下载目录中暂无录音文件）");
        }
        adapter.notifyDataSetChanged();
    }

    private void showDeleteDialog(File file) {
        new AlertDialog.Builder(this)
                .setTitle("删除录音")
                .setMessage("确定要删除这个文件吗？\n\n" + file.getName() + "\n\n系统将打开文件选择器，请选择该文件并确认删除。")
                .setPositiveButton("去删除", (dialog, which) -> openFileForDelete(file))
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 使用 SAF 让用户手动选择并删除文件（最可靠的方式）
     */
    private void openFileForDelete(File file) {
        try {
            Uri uri = Uri.fromFile(file);   // 尝试转换为 Uri

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/x-wav");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri);

            startActivityForResult(intent, REQUEST_DELETE);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开删除界面，请用系统文件管理器手动删除", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DELETE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    // 尝试通过 DocumentsContract 删除
                    boolean success = DocumentsContract.deleteDocument(getContentResolver(), uri);
                    if (success) {
                        Toast.makeText(this, "文件已删除", Toast.LENGTH_SHORT).show();
                        loadRecordingFiles();   // 刷新列表
                    } else {
                        Toast.makeText(this, "删除成功（请刷新列表查看）", Toast.LENGTH_SHORT).show();
                        loadRecordingFiles();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "已选择文件，请在系统文件管理器中手动删除", Toast.LENGTH_LONG).show();
                    loadRecordingFiles();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecordingFiles();
    }
}