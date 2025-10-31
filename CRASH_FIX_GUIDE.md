# 授权Shizuku后闪退问题 - 诊断与修复指南

## 🔴 问题描述
用户报告：授权Shizuku后应用直接闪退

## 🔧 已实施的紧急修复

### 修复1：恢复Shizuku API使用
**问题根源**：之前为了修复编译错误，错误地将`Shizuku.newProcess()`改成了`Runtime.getRuntime().exec()`

**影响**：
- `Runtime.exec()`没有系统级权限，无法执行需要权限的操作
- 导致所有需要Shizuku权限的功能失效
- 可能在授权后尝试执行命令时抛出异常导致崩溃

**修复内容**：
1. ✅ `ScreenCapture.kt` - 恢复使用`Shizuku.newProcess()`执行截图
2. ✅ `ViewHierarchyAnalyzer.kt` - 恢复使用`Shizuku.newProcess()`执行UI控件树导出
3. ✅ `OperationExecutor.kt` - 恢复使用`Shizuku.newProcess()`执行所有Shell命令

**改进**：
- 添加了详细的异常捕获和日志记录
- 增强了错误提示信息
- 添加了进程创建失败的保护逻辑

### 修复代码示例

#### ScreenCapture.kt
```kotlin
// 修复前（错误）
val process = Runtime.getRuntime().exec("screencap -p")

// 修复后（正确）
val process = try {
    Shizuku.newProcess(arrayOf("screencap", "-p"), null, null)
} catch (e: Exception) {
    Timber.e(e, "无法创建Shizuku进程")
    return@withContext Result.failure(Exception("创建截图进程失败: ${e.message}"))
}
```

## 📊 崩溃分析步骤

### 步骤1：获取崩溃日志
请通过以下方式获取详细的崩溃日志：

**方法A：使用ADB（推荐）**
```bash
adb logcat -c  # 清空日志
adb logcat | grep -E "AndroidRuntime|AutoAI|Shizuku"
```

**方法B：Android Studio**
1. 连接设备
2. 打开Logcat窗口
3. 筛选器设置为包名：`com.autoai.android`
4. 重现崩溃
5. 复制完整的异常堆栈

**方法C：系统日志查看器**
- 安装"MatLog"或"LogCat Reader"等应用
- 授予日志读取权限
- 重现崩溃并导出日志

### 步骤2：检查关键信息
崩溃日志中应包含：
```
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: com.autoai.android, PID: xxxxx
    java.lang.RuntimeException: ...
    at com.autoai.android...
```

### 步骤3：常见崩溃原因

#### 原因1：Shizuku权限问题
**特征**：
```
SecurityException: Permission denied
或
RemoteException: Binder died
```

**解决方案**：
1. 确保Shizuku正在运行
2. 重新授予权限
3. 重启应用

#### 原因2：API不兼容
**特征**：
```
NoSuchMethodError: No virtual method newProcess
或
MethodNotFoundException
```

**解决方案**：
- 更新Shizuku到最新版本（v13.5+）
- 检查Gradle依赖版本是否匹配

#### 原因3：内存不足
**特征**：
```
OutOfMemoryError
```

**解决方案**：
- 清理后台应用
- 重启设备

#### 原因4：初始化异常
**特征**：
```
在MainActivity.onCreate或ShizukuManager.initialize中抛出异常
```

**解决方案**：
- 检查Timber日志库是否正常初始化
- 验证Hilt依赖注入是否成功

## 🛡️ 预防性修复

### 1. 增强异常处理
已在所有Shizuku调用处添加try-catch：

```kotlin
val process = try {
    Shizuku.newProcess(command, null, null)
} catch (e: Exception) {
    Timber.e(e, "无法创建Shizuku进程")
    // 返回友好错误而非崩溃
    return ShellResult(false, "", "进程创建失败: ${e.message}")
}
```

### 2. 添加状态检查
在执行任何Shizuku操作前检查：

```kotlin
if (!shizukuManager.isShizukuAvailable()) {
    return Result.failure(Exception("Shizuku 不可用"))
}
```

### 3. 超时保护
所有进程操作都添加了超时机制：

```kotlin
val hasFinished = process.waitFor(10, TimeUnit.SECONDS)
if (!hasFinished) {
    process.destroy()
    return Result.failure(Exception("操作超时"))
}
```

## 🔍 调试清单

请按顺序检查以下项目：

- [ ] **Shizuku版本**
  - 版本 >= 13.5
  - 正在运行中
  - 显示"已授权"状态

- [ ] **应用权限**
  - Shizuku权限已授予
  - 存储权限（用于UIAutomator dump）
  - 网络权限（用于API调用）

- [ ] **设备环境**
  - Android版本 >= 8.0
  - 可用内存 > 200MB
  - 未安装冲突的自动化工具

- [ ] **应用状态**
  - 完全卸载旧版本
  - 清除应用数据
  - 重新安装并授权

## 🚀 测试验证步骤

### 1. 基础测试
```
1. 启动Shizuku服务
2. 打开AutoAI应用
3. 等待Shizuku状态检测完成
4. 点击"授予权限"按钮
5. 确认权限授予成功
6. 应用应进入主界面（不崩溃）
```

### 2. 功能测试
```
1. 在聊天界面输入："打开设置"
2. 观察是否能正常识别并执行
3. 检查日志是否有Shizuku相关错误
```

### 3. 压力测试
```
1. 快速连续授予和撤销权限
2. 应用应能正确处理状态变化
3. 不应出现崩溃
```

## 📝 日志收集模板

如果问题依然存在，请提供以下信息：

```
【设备信息】
- 品牌/型号：
- Android版本：
- RAM大小：

【Shizuku信息】
- 版本号：
- 运行状态：
- 授权状态：

【应用信息】
- 版本：
- 安装方式：（APK直接安装/GitHub Actions构建）
- 首次安装还是升级：

【崩溃日志】
（粘贴完整的logcat输出）
```

## 🔄 后续优化计划

1. **崩溃报告集成**
   - 集成Firebase Crashlytics或Sentry
   - 自动收集崩溃信息

2. **启动时健康检查**
   - 验证Shizuku可用性
   - 检查系统兼容性
   - 显示诊断信息

3. **降级策略**
   - 如果Shizuku不可用，提供手动模式
   - 允许部分功能在无Shizuku下工作

4. **用户引导**
   - 添加首次使用教程
   - 提供常见问题解答
   - 内置故障排查工具

## 📞 获取帮助

如果以上步骤都无法解决问题，请：

1. **提交Issue**：https://github.com/king0929zion/autoai-android/issues
   - 使用上面的日志收集模板
   - 附上完整的崩溃日志

2. **检查已知问题**
   - 查看项目Issues列表
   - 可能已有类似问题的解决方案

3. **联系开发者**
   - 提供详细的复现步骤
   - 附上截图和日志文件

---

**最后更新**：2025-10-31  
**修复版本**：commit cca3dd0  
**状态**：已推送到GitHub，等待编译验证
