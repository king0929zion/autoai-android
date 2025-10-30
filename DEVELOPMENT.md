# 开发指南

> 本文档包含架构设计、技术栈、开发计划和当前进度

## 📑 目录
- [架构设计](#架构设计)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [开发计划](#开发计划)
- [当前进度](#当前进度)
- [开发实践](#开发实践)

---

## 🏗️ 架构设计

### 系统分层架构

```
┌─────────────────────────────────────────────┐
│          用户界面层 (UI Layer)               │
│     ChatScreen | FloatingBall | TaskPanel    │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│       任务管理层 (Task Management)            │
│   TaskManager | TodoList | ExceptionHandler  │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│         AI 决策层 (AI Decision)              │
│  VLMClient | ActionParser | PromptBuilder   │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│         感知层 (Perception)                  │
│  ScreenCapture | ViewHierarchy | Fusion     │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│         执行层 (Execution)                   │
│  ExecutionEngine | SafetyChecker            │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│         权限层 (Shizuku)                     │
│  ShizukuManager | OperationExecutor         │
└─────────────────────────────────────────────┘
```

### 核心模块说明

#### 1. 权限层 (Permission Layer)
**包名**: `com.autoai.android.permission`

负责 Shizuku 集成和系统级操作封装

**关键类**:
- `ShizukuManager` - Shizuku 生命周期管理
- `OperationExecutor` - 操作执行封装（点击、滑动、输入等）

#### 2. 感知层 (Perception Layer)
**包名**: `com.autoai.android.perception`

负责屏幕信息采集和多模态融合

**关键类**:
- `ScreenCapture` - 截图捕获
- `ViewHierarchyAnalyzer` - 控件树解析
- `MultiModalFusion` - 信息融合

#### 3. 决策层 (Decision Layer)
**包名**: `com.autoai.android.decision`

负责 AI 推理和动作生成

**关键类**:
- `VLMClient` - VLM API 调用
- `ActionParser` - 动作指令解析
- `PromptBuilder` - 提示词构建

#### 4. 执行层 (Execution Layer)
**包名**: `com.autoai.android.execution`

负责动作执行和安全检查

**关键类**:
- `ExecutionEngine` - 执行引擎
- `SafetyChecker` - 安全检查器

#### 5. 任务管理层 (Task Management)
**包名**: `com.autoai.android.task`

负责任务生命周期和异常处理

**关键类**:
- `TaskManager` - 任务管理器
- `TodoListManager` - 清单管理
- `ExceptionHandler` - 异常处理

#### 6. 用户界面层 (UI Layer)
**包名**: `com.autoai.android.ui`

负责用户交互

**关键类**:
- `ChatScreen` - 聊天界面
- `FloatingBall` - 悬浮球
- `TaskDetailPanel` - 任务详情

---

## 📦 技术栈

### 开发环境
- **IDE**: Android Studio Hedgehog (2023.1.1)+
- **JDK**: 17
- **Kotlin**: 1.9.20
- **Gradle**: 8.2
- **最低 Android**: API 26 (Android 8.0)
- **目标 Android**: API 34 (Android 14)

### 核心依赖

#### 1. Shizuku - 系统级权限
```gradle
implementation 'dev.rikka.shizuku:api:13.1.5'
implementation 'dev.rikka.shizuku:provider:13.1.5'
```

#### 2. 网络请求
```gradle
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

#### 3. UI 框架
```gradle
implementation platform('androidx.compose:compose-bom:2024.01.00')
implementation 'androidx.compose.material3:material3'
implementation 'androidx.activity:activity-compose:1.8.2'
```

#### 4. 异步处理
```gradle
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

#### 5. 依赖注入
```gradle
implementation 'com.google.dagger:hilt-android:2.48.1'
kapt 'com.google.dagger:hilt-compiler:2.48.1'
```

#### 6. 其他工具
```gradle
implementation 'io.coil-kt:coil-compose:2.5.0'  // 图片处理
implementation 'androidx.datastore:datastore-preferences:1.0.0'  // 数据存储
implementation 'com.jakewharton.timber:timber:5.0.1'  // 日志
```

### AI API 配置

**硅基流动 (SiliconFlow)**
- 官网: https://cloud.siliconflow.cn/
- 支持模型: Qwen/Qwen2.5-VL-7B-Instruct, Qwen2.5-VL-72B-Instruct
- 定价: 约 ¥0.002/千 tokens

**API 请求格式**:
```json
{
  "model": "Qwen/Qwen2.5-VL-7B-Instruct",
  "messages": [
    {"role": "system", "content": "你是一个手机自动化助手..."},
    {"role": "user", "content": [
      {"type": "text", "text": "当前任务: 打开微信"},
      {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,..."}}
    ]}
  ],
  "temperature": 0.3,
  "max_tokens": 2000
}
```

### 性能目标
- **截图捕获**: < 200ms
- **控件树解析**: < 100ms
- **API 调用**: < 2s
- **单步执行**: < 5s（含 AI 推理）
- **内存占用**: < 200MB
- **APK 大小**: < 20MB

---

## 📂 项目结构

```
autoaiAndroid/
├── README.md                    # 项目介绍
├── DESIGN.md                    # 完整设计文档
├── DEVELOPMENT.md               # 本文件 - 开发指南
│
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    │
    └── src/main/java/com/autoai/android/
        ├── AutoAIApplication.kt
        │
        ├── data/model/               # 数据模型层
        │   ├── Action.kt
        │   ├── ScreenState.kt
        │   └── Task.kt
        │
        ├── permission/               # 权限层 ✅
        │   ├── ShizukuManager.kt
        │   └── OperationExecutor.kt
        │
        ├── perception/               # 感知层 ⏳
        │   ├── ScreenCapture.kt
        │   ├── ViewHierarchyAnalyzer.kt
        │   └── MultiModalFusion.kt
        │
        ├── decision/                 # 决策层
        │   ├── VLMClient.kt
        │   ├── ActionParser.kt
        │   └── PromptBuilder.kt
        │
        ├── execution/                # 执行层
        │   ├── ExecutionEngine.kt
        │   └── SafetyChecker.kt
        │
        ├── task/                     # 任务管理层
        │   ├── TaskManager.kt
        │   ├── TodoListManager.kt
        │   └── ExceptionHandler.kt
        │
        ├── ui/                       # UI 层
        │   ├── MainActivity.kt
        │   ├── theme/
        │   ├── chat/
        │   ├── floating/
        │   └── settings/
        │
        ├── di/                       # 依赖注入
        │   └── AppModule.kt
        │
        └── utils/                    # 工具类
```

---

## 📅 开发计划

### Phase 1: MVP 核心功能 (2-3周)

#### 1.1 Shizuku 集成 ✅ 已完成
- ✅ 权限检查和申请
- ✅ 基础操作封装

#### 1.2 屏幕感知 ⏳ 进行中 (3天)
- [ ] `ScreenCapture.kt` - 截图捕获和压缩
- [ ] `ViewHierarchyAnalyzer.kt` - 控件树解析
- [ ] `MultiModalFusion.kt` - 多模态融合

#### 1.3 AI 决策层 (4天)
- [ ] `VLMClient.kt` - API 客户端
- [ ] `ActionParser.kt` - 指令解析
- [ ] `PromptBuilder.kt` - 提示词构建

#### 1.4 执行引擎 (4天)
- [ ] `ExecutionEngine.kt` - 执行循环
- [ ] `SafetyChecker.kt` - 安全检查

**里程碑**: 能够执行简单指令（如"打开微信"）

### Phase 2: AI 决策增强 (2-3周)
- [ ] TaskManager - 任务管理
- [ ] TodoListManager - 清单动态更新
- [ ] ExceptionHandler - 异常处理和重试
- [ ] 提示词工程优化

**里程碑**: 执行 5 步以上复杂任务

### Phase 3: 用户界面 (2周)
- [ ] 聊天界面（Compose）
- [ ] 悬浮控制球
- [ ] 任务详情面板
- [ ] 设置界面

**里程碑**: 完整用户交互流程

### Phase 4: 高级功能 (3-4周)
- [ ] 三级权限和支付保护
- [ ] 任务模板系统
- [ ] 性能优化
- [ ] 日志和诊断工具

**里程碑**: 功能完整的 1.0 版本

---

## 📊 当前进度

**阶段**: Beta 版本  
**完成度**: 约 85%  
**最后更新**: Beta 版本核心功能完成

### ✅ 已完成

**1. 项目基础架构 (100%)**
- ✅ 完整项目文档（README, DESIGN, DEVELOPMENT, BETA_GUIDE）
- ✅ Gradle 构建配置和依赖管理
- ✅ AndroidManifest 和资源文件
- ✅ 项目结构和包划分

**2. 数据模型层 (100%)**
- ✅ `Action.kt` - 操作动作数据结构
- ✅ `ScreenState.kt` - 屏幕状态数据结构
- ✅ `Task.kt` - 任务管理数据结构

**3. 权限层 (100%)**
- ✅ `ShizukuManager.kt` - Shizuku 生命周期管理
- ✅ `OperationExecutor.kt` - 所有基础操作封装

**4. 感知层 (100%)**
- ✅ `ScreenCapture.kt` - 截图捕获和压缩
- ✅ `ViewHierarchyAnalyzer.kt` - 控件树解析
- ✅ `MultiModalFusion.kt` - 多模态信息融合

**5. 决策层 (100%)**
- ✅ `VLMClient.kt` - AI API 客户端
- ✅ `ActionParser.kt` - 动作解析器
- ✅ `PromptBuilder.kt` - 提示词构建器

**6. 执行层 (100%)**
- ✅ `ExecutionEngine.kt` - 执行引擎
- ✅ `SafetyChecker.kt` - 安全检查器

**7. 任务管理层 (100%)**
- ✅ `TaskManager.kt` - 任务管理器

**8. 用户界面 (85%)**
- ✅ `MainActivity.kt` - 主界面和导航
- ✅ `ChatScreen.kt` - 聊天界面
- ✅ `SettingsScreen.kt` - 设置界面
- ✅ `Theme.kt` - Material Design 3 主题

**9. 依赖注入 (100%)**
- ✅ Hilt 配置和 AppModule
- ✅ DataStore 集成

**10. 测试框架 (10%)**
- ✅ `ActionTest.kt` 和 `TaskTest.kt`

### 📋 待完善
- TodoList 可视化显示
- 悬浮球控制
- 更多单元测试
- 集成测试
- 性能优化

---

## 💻 开发实践

### 下一步开发任务

**优先级 P0 - 立即开始**:
1. 实现 `ScreenCapture.kt` - 截图捕获和压缩
2. 实现 `ViewHierarchyAnalyzer.kt` - 控件树解析
3. 集成 VLM API - `VLMClient.kt`

**实现提示**:
- 截图: 使用 Shizuku 执行 `screencap -p` 命令
- 控件树: 使用 `uiautomator dump` 导出 XML 并解析
- API: 配置 Retrofit + Gson,参考上面的 API 格式

### 调试技巧

**查看日志**:
```bash
adb logcat -s AutoAI:* Timber:*
```

**测试命令**:
```bash
# 截图测试
adb shell screencap -p /sdcard/test.png

# 导出控件树
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml
```

### 代码规范

- 遵循 [Kotlin 官方代码风格](https://kotlinlang.org/docs/coding-conventions.html)
- 类名: PascalCase (`TaskManager`)
- 函数名: camelCase (`executeAction`)
- 常量: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
- 所有公共 API 必须添加 KDoc 注释

### Git 提交规范
```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试
chore: 构建/工具
```

---

## 📚 学习资源

- [Shizuku 文档](https://shizuku.rikka.app/zh-hans/)
- [UiAutomator 指南](https://developer.android.com/training/testing/other-components/ui-automator)
- [Jetpack Compose 教程](https://developer.android.com/jetpack/compose/tutorial)
- [硅基流动 API 文档](https://docs.siliconflow.cn/)
- [Kotlin 协程指南](https://kotlinlang.org/docs/coroutines-guide.html)

---

## 🐛 常见问题

**Q: Gradle 同步失败**  
A: 检查网络连接,清理缓存 `./gradlew clean`

**Q: Shizuku 不可用**  
A: 确认 Shizuku 应用已安装并正在运行,在 Shizuku 中授权 AutoAI

**Q: 无法截图**  
A: 检查 Shizuku 权限,某些系统可能限制截图功能

---

## 📞 获取帮助

- 查看项目文档: `README.md`, `DESIGN.md`
- 查阅 Android Studio Logcat
- 参考已完成的模块: `ShizukuManager.kt`, `OperationExecutor.kt`

---

**最后更新**: 项目初始化完成
