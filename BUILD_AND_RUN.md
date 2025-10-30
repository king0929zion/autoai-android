# 构建和运行指南

## 📋 前置条件

### 必需软件
- ✅ Android Studio Hedgehog (2023.1.1) 或更高
- ✅ JDK 17
- ✅ Android SDK (API 26-34)
- ✅ Git

### 测试设备
- Android 8.0 或更高版本
- 建议使用真机测试
- 需要安装并激活 Shizuku

---

## 🚀 构建项目

### 1. 克隆或打开项目

```bash
# 如果是从 Git 克隆
git clone <repository-url>
cd autoaiAndroid

# 如果已有项目，直接打开
# Android Studio -> File -> Open -> 选择 autoaiAndroid 目录
```

### 2. 同步依赖

在 Android Studio 中：
- 点击顶部的 "Sync Project with Gradle Files" 图标
- 或者在终端运行：

```bash
./gradlew clean build
```

### 3. 检查配置

确认 `build.gradle.kts` 中的配置正确：
- `compileSdk = 34`
- `minSdk = 26`
- `targetSdk = 34`

### 4. 构建 APK

**Debug 版本**:
```bash
./gradlew assembleDebug
```

输出位置: `app/build/outputs/apk/debug/app-debug.apk`

**Release 版本**:
```bash
./gradlew assembleRelease
```

输出位置: `app/build/outputs/apk/release/app-release.apk`

---

## 📱 设备准备

### 1. 安装 Shizuku

**方式 A: 从 GitHub 下载**
```bash
# 下载最新版本
https://github.com/RikkaApps/Shizuku/releases
```

**方式 B: 使用 ADB 安装**
```bash
adb install Shizuku.apk
```

### 2. 激活 Shizuku

**Android 11+ (推荐无线调试方式)**:
1. 打开设置 → 开发者选项 → 无线调试
2. 打开 Shizuku 应用
3. 点击配对按钮
4. 输入配对码

**所有版本 (USB 调试方式)**:
```bash
# 1. 连接设备
adb devices

# 2. 启动 Shizuku
adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
```

### 3. 验证 Shizuku

打开 Shizuku 应用，应该看到：
- ✅ "Shizuku 正在运行"
- 显示版本号

---

## 🏃 运行应用

### 方式 1: 从 Android Studio 运行

1. 连接设备: `adb devices`
2. 在 Android Studio 中选择设备
3. 点击绿色的 Run 按钮（或按 Shift+F10）

### 方式 2: 命令行安装

```bash
# 构建并安装
./gradlew installDebug

# 或者直接安装已有的 APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 方式 3: 手动安装

1. 将 APK 文件传输到设备
2. 在设备上打开文件管理器
3. 点击 APK 文件安装

---

## ⚙️ 首次配置

### 1. 授予权限

应用首次启动时会请求：
- ✅ Shizuku 权限（在 Shizuku 应用中授权）
- ✅ 悬浮窗权限（可选）

### 2. 配置 API

1. 点击右上角设置图标
2. 填入配置信息：

```
API Key: sk-xxxxxxxxxxxxxx
Base URL: https://api.siliconflow.cn/
Model: Qwen/Qwen2-VL-7B-Instruct
```

3. 点击"保存设置"

### 3. 验证功能

返回聊天界面，输入测试任务：
```
"打开设置"
```

如果成功执行，说明配置正确。

---

## 🧪 运行测试

### 单元测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试
./gradlew test --tests ActionTest
./gradlew test --tests TaskTest
```

### Android 仪器测试

```bash
# 需要连接设备或启动模拟器
./gradlew connectedAndroidTest
```

### 查看测试报告

```bash
# 测试报告位置
app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 🔍 调试技巧

### 查看日志

**Android Studio Logcat**:
- 过滤器: `AutoAI`
- 级别: Debug

**ADB 命令行**:
```bash
# 查看所有日志
adb logcat -s AutoAI:* Timber:*

# 清空日志后查看
adb logcat -c && adb logcat -s AutoAI:*

# 保存日志到文件
adb logcat -s AutoAI:* > autoai_log.txt
```

### 常见问题排查

**Gradle 同步失败**:
```bash
# 清理构建
./gradlew clean

# 删除缓存
rm -rf ~/.gradle/caches/

# 重新构建
./gradlew build
```

**Gradle Wrapper 配置错误**:
如果遇到 `'org.gradle.api.file.FileCollection org.gradle.api.artifacts.Configuration.fileCollection'` 错误:
```bash
# 1. 检查 gradle/wrapper/gradle-wrapper.properties 文件是否存在
# 2. 确认 Gradle 版本与 AGP 版本兼容
#    - AGP 8.1.4 需要 Gradle 8.0+
#    - AGP 8.2.x 需要 Gradle 8.2+
# 3. 如果 gradle-wrapper.jar 缺失，重新生成:
gradle wrapper --gradle-version 8.0

# 或者从项目根目录运行（Windows）:
.\gradlew.bat --version
```

**Gradle 下载慢**:
如果 Gradle 下载超时，可以：
```bash
# 1. 增加超时时间（已在 gradle-wrapper.properties 中设置为 60 秒）
# 2. 手动下载 Gradle:
#    - 访问 https://services.gradle.org/distributions/
#    - 下载 gradle-8.0-bin.zip
#    - 解压到 ~/.gradle/wrapper/dists/gradle-8.0-bin/
# 3. 使用国内镜像（可选）

**Shizuku 不可用**:
```bash
# 检查 Shizuku 状态
adb shell ps | grep shizuku

# 重启 Shizuku
adb shell am force-stop moe.shizuku.privileged.api
adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
```

**应用崩溃**:
```bash
# 查看崩溃日志
adb logcat *:E

# 查看 ANR
adb pull /data/anr/traces.txt
```

---

## 📦 打包发布

### 1. 配置签名

创建 `keystore.properties`:
```properties
storeFile=release.keystore
storePassword=yourPassword
keyAlias=autoai
keyPassword=yourPassword
```

### 2. 构建 Release APK

```bash
./gradlew assembleRelease
```

### 3. 优化 APK

```bash
# 使用 R8 压缩和混淆
./gradlew assembleRelease --no-configuration-cache
```

输出: `app/build/outputs/apk/release/app-release.apk`

### 4. 验证 APK

```bash
# 安装测试
adb install app/build/outputs/apk/release/app-release.apk

# 检查签名
jarsigner -verify -verbose -certs app-release.apk
```

---

## 🌐 使用代理（可选）

如果需要使用代理访问 API：

### Gradle 代理

在 `gradle.properties` 添加：
```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

### 应用内代理

在代码中配置 OkHttp 代理（仅开发环境）。

---

## 📊 性能监控

### 内存监控

```bash
# 查看内存使用
adb shell dumpsys meminfo com.autoai.android
```

### CPU 监控

在 Android Studio 中：
- View → Tool Windows → Profiler
- 选择设备和应用
- 查看 CPU、内存、网络使用情况

---

## 🆘 获取帮助

### 文档
- `README.md` - 项目介绍
- `DESIGN.md` - 设计文档
- `DEVELOPMENT.md` - 开发指南
- `BETA_GUIDE.md` - 使用指南

### 日志位置
- Logcat: 实时日志
- `/sdcard/AutoAI/`: 截图和缓存

### 常用命令
```bash
# 卸载应用
adb uninstall com.autoai.android

# 清除数据
adb shell pm clear com.autoai.android

# 重启应用
adb shell am force-stop com.autoai.android
adb shell am start -n com.autoai.android/.ui.MainActivity
```

---

**祝构建顺利！** 🚀
