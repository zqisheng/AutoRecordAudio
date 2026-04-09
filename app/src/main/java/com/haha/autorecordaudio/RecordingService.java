package com.haha.autorecordaudio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RecordingService extends Service {

    private static final String CHANNEL_ID = "recording_channel";
    public static MediaProjection mediaProjection;  // 静态传递
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    private File outputFile;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("正在录制内部音频")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "内部音频录制", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        outputFile = (File) intent.getSerializableExtra("output_file");

        if (outputFile == null) {
            Log.e("RecordingService", "output_file 为空");
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = getNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, notification);
        }

        // 启动录音（如果 mediaProjection 还没来，就稍等）
        if (mediaProjection != null) {
            startRecording();
        } else {
            // 延迟重试一次（最多等 1 秒）
            new android.os.Handler().postDelayed(() -> {
                if (mediaProjection != null) {
                    startRecording();
                } else {
                    Log.e("RecordingService", "MediaProjection 仍为空，停止服务");
                    stopSelf();
                }
            }, 800);
        }

        return START_STICKY;
    }

    private void startRecording() {
        if (mediaProjection == null) {
            Log.e("RecordingService", "MediaProjection 为空，无法开始录音");
            return;
        }

        int sampleRate = 44100;
        int channels = 2;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build();

        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat) * 2;

        audioRecord = new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build();

        audioRecord.startRecording();
        isRecording = true;

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[1024 * 4];
            long totalAudioLen = 0;
            long byteRate = (long) sampleRate * channels * 2;

            try {
                // 先写入空的44字节 WAV 头
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    writeWavHeader(fos, 0, 0, sampleRate, channels, byteRate);
                }

                // 开始真正录音（追加数据）
                try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {  // true = append
                    while (isRecording) {
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            fos.write(buffer, 0, read);
                            totalAudioLen += read;
                        }
                    }
                }

                // 录音结束后，只修改头部（不覆盖数据！）
                long totalDataLen = totalAudioLen + 36;
                try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
                    raf.seek(0);  // 回到文件开头
                    writeWavHeaderToRaf(raf, totalAudioLen, totalDataLen, sampleRate, channels, byteRate);
                }

                Log.i("RecordingService", "录音完成！文件大小: " + outputFile.length() + " 字节，路径: " + outputFile.getAbsolutePath());

            } catch (IOException e) {
                Log.e("RecordingService", "录音失败", e);
            }
        });

        recordingThread.start();
    }

    /**
     * 写入初始空 WAV 头（44字节）
     */
    private void writeWavHeader(FileOutputStream out, long totalAudioLen, long totalDataLen,
                                long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = createWavHeader(totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
        out.write(header, 0, 44);
    }

    /**
     * 使用 RandomAccessFile 只修改头部（关键修复）
     */
    private void writeWavHeaderToRaf(RandomAccessFile raf, long totalAudioLen, long totalDataLen,
                                     long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = createWavHeader(totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
        raf.write(header, 0, 44);
    }

    /**
     * 统一的 WAV 头生成方法
     */
    private byte[] createWavHeader(long totalAudioLen, long totalDataLen, long longSampleRate,
                                   int channels, long byteRate) {
        byte[] header = new byte[44];

        // RIFF
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);

        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';

        // fmt
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * channels);
        header[33] = 0;
        header[34] = 16; header[35] = 0;

        // data
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;
    }

    @Override
    public void onDestroy() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}