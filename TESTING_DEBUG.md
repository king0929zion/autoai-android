# 测试和调试指南

## 🧪 快速测试流程

### 第一步：验证基础环境

1. **检查 Shizuku 状态**
   ```bash
   # 查看 Shizuku 进程
   adb shell ps | grep shizuku
   
   # 如果没有运行，启动 Shizuku
   adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
   ```

2. **检查网络连接**
   ```bash
   # 测试网络
   adb shell ping -c 3 api.siliconflow.cn
   ```

3. **查看应用日志**
   ```bash
   # 实时日志
   adb logcat -s AutoAI:* Timber:* -v time
   
   # 保存日志到文件
   adb logcat -s AutoAI:* > autoai_debug.log
   ```

---

## 🎯 功能测试清单

### 基础功能测试

#### 1. 简单任务测试
```
测试用例：打开系统应用
输入："打开设置"
预期：成功打开设置应用
```

```
测试用例：返回操作
输入："返回"
预期：返回到上一个界面
```

#### 2. 应用导航测试
```
测试用例：应用内导航
输入："打开设置，找到 WLAN"
预期：进入 WLAN 设置页面
```

#### 3. 搜索功能测试
```
测试用例：应用内搜索
输入："在设置里搜索蓝牙"
预期：显示蓝牙相关设置
```

### 安全功能测试

#### 支付保护测试
```
测试用例：支付场景检测
1. 打开支付宝或微信
2. 进入支付页面
3. 输入任务："点击确认支付"
预期：AI 自动暂停并提示支付保护
```

---

## 🐛 常见问题排查

### 问题 1: 应用崩溃

**症状**：应用启动后立即崩溃

**排查步骤**：
```bash
# 1. 查看崩溃日志
adb logcat *:E

# 2. 查看崩溃堆栈
adb logcat AndroidRuntime:E

# 3. 检查内存
adb shell dumpsys meminfo com.autoai.android
```

**常见原因**：
- Shizuku 未运行
- 权限未授予
- 依赖库版本冲突

---

### 问题 2: API 调用失败

**症状**：任务执行失败，提示 API 错误

**排查步骤**：
```bash
# 1. 检查网络
adb shell ping -c 3 api.siliconflow.cn

# 2. 查看 API 调用日志
adb logcat -s AutoAI:D | grep -i api

# 3. 验证 API Key
# 在应用设置中检查 API Key 是否正确
```

**常见原因**：
- API Key 未配置或错误
- 网络不通
- API 配额用尽
- Base URL 配置错误

**解决方案**：
1. 重新输入正确的 API Key
2. 检查网络连接
3. 确认 API 配额
4. 使用默认 Base URL: `https://api.siliconflow.cn/`

---

### 问题 3: Shizuku 不可用

**症状**：应用显示 "Shizuku 不可用"

**排查步骤**：
```bash
# 1. 检查 Shizuku 是否安装
adb shell pm list packages | grep shizuku

# 2. 检查 Shizuku 是否运行
adb shell ps | grep shizuku

# 3. 查看 Shizuku 版本
adb shell dumpsys package moe.shizuku.privileged.api | grep versionName
```

**解决方案**：
```bash
# 重启 Shizuku
adb shell am force-stop moe.shizuku.privileged.api
adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh

# 或者在 Shizuku 应用中重新启动
```

---

### 问题 4: 截图失败

**症状**：任务执行到截图步骤失败

**排查步骤**：
```bash
# 1. 手动测试截图
adb shell screencap -p /sdcard/test.png
adb pull /sdcard/test.png

# 2. 检查权限
adb shell ls -l /sdcard/AutoAI/

# 3. 查看日志
adb logcat -s AutoAI:* | grep -i screen
```

**常见原因**：
- Shizuku 权限不足
- 存储空间不足
- 某些系统限制截图

---

### 问题 5: AI 决策不准确

**症状**：AI 执行了错误的操作

**分析方法**：
```bash
# 查看 AI 决策日志
adb logcat -s AutoAI:* | grep -E "AI|决策|响应"
```

**改进方法**：
1. 简化任务描述
2. 分解为多个简单步骤
3. 确保屏幕内容清晰
4. 检查提示词是否合适

---

## 📊 性能分析

### 查看执行时间
```bash
# 过滤性能日志
adb logcat -s Performance:*

# 分析慢操作
adb logcat -s AutoAI:* | grep -E "ms|秒"
```

### 内存分析
```bash
# 查看内存使用
adb shell dumpsys meminfo com.autoai.android

# 实时监控
watch -n 1 'adb shell dumpsys meminfo com.autoai.android | grep TOTAL'
```

### 网络分析
```bash
# 查看网络请求
adb logcat -s AutoAI:* OkHttp:*
```

---

## 🔍 调试技巧

### 1. 添加调试日志

在需要调试的地方添加：
```kotlin
Timber.tag("DEBUG").d("变量值: $variable")
```

### 2. 断点调试

在 Android Studio 中：
1. 设置断点（点击行号左侧）
2. 点击 Debug 按钮
3. 应用暂停在断点处
4. 查看变量值和调用栈

### 3. 网络抓包

使用 Charles 或 Fiddler：
```bash
# 设置代理
adb shell settings put global http_proxy 192.168.1.100:8888
```

### 4. 数据库检查

```bash
# 导出 DataStore
adb pull /data/data/com.autoai.android/files/datastore/settings.preferences_pb

# 查看应用数据
adb shell run-as com.autoai.android ls -la files/
```

---

## 📝 测试报告模板

### 问题报告格式

```markdown
## 问题描述
[简要描述问题]

## 复现步骤
1. [步骤1]
2. [步骤2]
3. [步骤3]

## 预期行为
[应该发生什么]

## 实际行为
[实际发生了什么]

## 环境信息
- 设备型号: [如 Xiaomi 12]
- Android 版本: [如 Android 13]
- 应用版本: [如 0.1.0-beta]
- Shizuku 版本: [如 13.1.5]

## 日志
```
[粘贴相关日志]
```

## 截图
[如果有截图]
```

---

## 🎯 测试检查表

### 发布前测试

- [ ] 基础功能
  - [ ] 简单任务（打开应用）
  - [ ] 中等任务（应用内导航）
  - [ ] 错误处理（无效指令）

- [ ] 安全功能
  - [ ] 支付保护
  - [ ] 权限检查
  - [ ] 敏感信息保护

- [ ] 用户界面
  - [ ] 聊天界面响应
  - [ ] 设置保存和加载
  - [ ] 错误提示显示

- [ ] 性能测试
  - [ ] 内存占用 < 200MB
  - [ ] 单步响应 < 5s
  - [ ] 无内存泄漏

- [ ] 兼容性
  - [ ] 不同设备测试
  - [ ] 不同 Android 版本
  - [ ] 不同 UI 系统

---

## 💡 优化建议

### 提高成功率
1. 使用简单明确的任务描述
2. 避免在动画过程中执行
3. 确保网络稳定
4. 定期清理缓存

### 降低成本
1. 避免重复执行失败任务
2. 使用简单任务代替复杂任务
3. 缓存常用操作
4. 监控 API 使用量

---

## 📞 获取帮助

### 日志收集
```bash
# 收集完整日志
adb logcat -d > full_log.txt

# 收集应用特定日志
adb logcat -d -s AutoAI:* Timber:* > app_log.txt

# 收集崩溃日志
adb logcat -d AndroidRuntime:E > crash_log.txt
```

### 系统信息收集
```bash
# 设备信息
adb shell getprop ro.build.version.release  # Android 版本
adb shell getprop ro.product.model          # 设备型号

# 应用信息
adb shell dumpsys package com.autoai.android | grep version
```

---

**祝调试顺利！** 🐛➡️✅
