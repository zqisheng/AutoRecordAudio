# 内部音频录制器 Internal Audio Recorder

**一个使用 AI 辅助开发的 Android 应用**，专注于录制手机内部音频（系统声音），并提供灵活的剪辑与导出功能。

本项目由 **Grok（xAI）** 辅助开发完成，旨在帮助用户轻松录制手机内播放的音频（如音乐、视频、游戏声音等）。

### 主要功能

- **内部音频录制**：使用 Android 官方 AudioPlaybackCapture API 录制其他 App 正在播放的声音（无需 Root）
- **自定义保存位置**：支持用户在录制前或录制后自由选择保存目录和文件名
- **音频剪辑**：播放录音、设置起点/终点进行精确剪辑
- **导出 WAV**：导出剪辑后的标准 WAV 文件
- **录音管理**：录音列表支持长按删除文件
- **权限友好提示**：完善的权限检查与用户引导

### 推荐搭配使用

**本项目与 [CloneTTS](https://github.com/sipeter/CloneTTS) 配合使用效果更佳！**

你可以：
1. 使用本应用录制喜欢的音频（歌曲、配音、对话等）
2. 将录制的音频导入 CloneTTS 项目进行声音克隆训练
3. 实现高质量的个性化 TTS（文本转语音）模型

---

### 使用说明

1. 安装应用后授予**录音权限**和**存储权限**
2. 点击「开始录制」→ 选择保存位置 → 同意屏幕录制权限
3. 播放任意音频（音乐、视频、游戏等）
4. 停止录制后可进行播放、剪辑，并导出为 WAV 文件
5. 在「我的录音库」中管理所有录音（支持长按删除）

### 注意事项

- 部分音乐 App 可能会主动禁止被录制（系统保护机制）
- 首次使用需授予必要权限
- 推荐在安静环境下测试以获得最佳效果

### 技术特点

- 基于 Android 10 (API 29) 开发
- 使用 MediaProjection + AudioPlaybackCapture 实现内部音频捕获
- 支持 SAF（Storage Access Framework）自定义保存路径
- 标准 WAV 文件头写入，确保文件兼容性
- 界面简洁友好，操作流畅

---

**本项目由 AI（Grok by xAI）辅助开发完成**

---

## English Version

# Internal Audio Recorder

**An Android application developed with AI assistance**, focused on recording internal system audio and providing flexible trimming and export features.

This project was built with the assistance of **Grok (xAI)** to help users easily capture audio playing on their device.

### Key Features

- **Internal Audio Recording**: Records audio from other apps using Android's official AudioPlaybackCapture API (No Root required)
- **Custom Save Location**: Choose any folder and filename before or after recording
- **Audio Trimming**: Play, set start/end points, and trim recordings precisely
- **WAV Export**: Export trimmed audio as standard WAV files
- **Library Management**: View and delete recordings with long-press support
- **Friendly Permission Handling**

### Best Used With

**Highly recommended to use together with [CloneTTS](https://github.com/sipeter/CloneTTS)**

**Workflow**:
1. Use this app to record high-quality audio (songs, voice acting, dialogues, etc.)
2. Import the recorded audio into CloneTTS for voice cloning
3. Train your own personalized TTS model

---

### How to Use

1. Grant **RECORD_AUDIO** and **Storage** permissions after installation
2. Tap "Start Recording" → Choose save location → Allow screen recording permission
3. Play any audio on your device
4. Stop recording, trim if needed, and export as WAV
5. Manage recordings in "My Recordings" (long-press to delete)

### Notes

- Some music streaming apps may block internal recording due to system protection
- Storage and microphone permissions are required on first launch
- Best results in quiet environments

### Technical Highlights

- Developed for Android 10 (API 29) and above
- Uses MediaProjection + AudioPlaybackCapture for system audio capture
- SAF support for custom save paths
- Proper WAV header implementation
- Clean and intuitive UI

---

**This project was developed with AI assistance (Grok by xAI)**

---

欢迎 Star 本项目！  
如果在使用过程中遇到问题或有功能建议，欢迎提出。

**Developed with ❤️ and AI**
