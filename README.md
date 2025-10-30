# AI 自主控机系统 (Auto AI Android)

[![Android CI](https://github.com/你的用户名/autoai-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/你的用户名/autoai-android/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)

基于 **Shizuku + Qwen3-VL** 的安卓端全自动 AI 控机系统。

## 🎯 项目简介

用户只需用自然语言描述需求，AI 就会自动规划任务步骤并执行复杂的手机操作。

**举例**:
- 你说: "帮我给张三发个微信，说今天晚上 8 点见"
- AI 自动: 打开微信 → 找到张三 → 进入聊天 → 输入消息 → 发送

## ✨ 核心特性

- **🤖 智能自主**: 自然语言交互,自动任务规划,动态调整策略
- **👁️ 多模态感知**: 视觉理解 + 控件树分析,准确识别屏幕状态
- **🛡️ 安全可控**: 三级权限体系,支付保护,隐私保护
- **💪 可靠执行**: 异常处理,智能重试,透明进度
- **🎨 精美界面**: Material Design 3,流畅动画,深浅色主题 ⭐ NEW
- **📊 任务历史**: 完整记录,统计分析,智能过滤 ⭐ NEW
- **⚡ 快捷操作**: 常用任务一键执行,提升效率 ⭐ NEW

## 📦 技术栈

- **开发**: Kotlin + Jetpack Compose + Hilt + Navigation
- **UI**: Material Design 3 + 自定义主题系统
- **权限**: Shizuku (系统级操作,无需 root)
- **AI**: Qwen2-VL-7B-Instruct (硅基流动 API)
- **网络**: Retrofit2 + OkHttp3
- **数据**: DataStore + Room (规划中)
- **最低版本**: Android 8.0 (API 26)

## 🚀 快速开始

### 0. 获取 APK

**方式一：GitHub Actions 自动编译（推荐）**
1. 查看 [GitHub 部署指南](GITHUB_DEPLOY_GUIDE.md)
2. 推送代码到 GitHub
3. 在 Actions 中下载编译好的 APK
4. 无需本地编译，节省磁盘空间

**方式二：本地编译**

### 1. 环境准备

**必需工具**:
- Android Studio Hedgehog (2023.1.1)+
- JDK 17
- Android 8.0+ 测试设备

### 2. Shizuku 配置

**Android 11+ (推荐)**:
1. 安装 [Shizuku](https://github.com/RikkaApps/Shizuku/releases)
2. 开启 "无线调试"
3. 在 Shizuku 中配对激活

**其他版本**:
```bash
adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
```

### 3. API 配置

1. 注册 [硅基流动](https://cloud.siliconflow.cn/) 获取 API Key
2. 在应用中配置:
   - 模型: `Qwen/Qwen2.5-VL-7B-Instruct`
   - Temperature: `0.3`

### 4. 运行

```bash
# 同步依赖
./gradlew clean build

# 运行应用
adb devices
# 点击 Android Studio Run 按钮
```

## 📱 功能亮点

### 🎯 聊天界面
- **快捷任务栏**: 5个常用任务一键执行
- **智能对话**: 自然语言描述任务需求
- **实时反馈**: 显示执行进度和状态
- **精美气泡**: 渐变色设计 + 流畅动画

### 📊 历史记录
- **任务统计**: 总数/成功/失败一目了然
- **智能过滤**: 快速筛选任务类型
- **详细信息**: 查看每个任务的执行步骤

### ⚙️ 高级设置
- **API配置**: 自定义API Key和模型
- **参数调节**: Temperature、Max Tokens精细控制
- **主题切换**: 深色/浅色模式自由选择

### 💡 帮助中心
- **快速开始**: 4步上手指南
- **常见问题**: 详细的FAQ解答
- **功能介绍**: 全面了解应用能力

## 📱 使用示例

**快捷任务**:
```
点击快捷按钮 → "打开微信" → AI 自动执行 → 完成
```

**自定义任务**:
```
输入: "给张三发微信,说今晚8点见"
→ AI 解析任务 → 打开微信 → 找到联系人 → 发送消息 → 完成
```

**复杂任务**:
```
"在淘宝搜索机械键盘,按销量排序"
→ AI 生成计划 → 逐步执行 → 实时反馈 → 完成
```

**安全保护**:
```
检测到支付操作 → 自动暂停 → 用户确认 → 手动完成
```

## 📊 项目进度

**当前阶段**: Phase 1 - MVP 核心功能  
**完成度**: 约 55% (+25% ⬆️)

- ✅ 项目架构设计
- ✅ 数据模型和权限层
- ✅ 完整的UI界面系统 ⭐ NEW
- ✅ Material Design 3主题 ⭐ NEW
- ✅ 聊天/设置/历史/帮助页面 ⭐ NEW
- ⏳ 屏幕感知层开发中
- ⏳ AI 决策层集成中
- 📋 数据持久化待开始

详见 [PROGRESS.md](PROGRESS.md) | [UI优化](UI_OPTIMIZATION.md)

## 📚 文档导航

### 核心文档
- **[README.md](README.md)** - 本文件,项目入口
- **[DESIGN.md](DESIGN.md)** - 完整功能设计文档
- **[DEVELOPMENT.md](DEVELOPMENT.md)** - 开发指南和架构说明

### 部署指南 ⭐ NEW
- **[GITHUB_DEPLOY_GUIDE.md](GITHUB_DEPLOY_GUIDE.md)** - GitHub 部署和自动编译指南
- **[FIX_AND_OPTIMIZATION_REPORT.md](FIX_AND_OPTIMIZATION_REPORT.md)** - 修复和优化报告

### 使用指南
- **[BUILD_AND_RUN.md](BUILD_AND_RUN.md)** - 构建和运行指南
- **[BETA_GUIDE.md](BETA_GUIDE.md)** - Beta版使用指南
- **[TESTING_DEBUG.md](TESTING_DEBUG.md)** - 测试和调试指南

### 项目记录
- **[PROGRESS.md](PROGRESS.md)** - 项目进度和更新日志
- **[UI_OPTIMIZATION.md](UI_OPTIMIZATION.md)** - UI优化详解
- **[CHANGELOG.md](CHANGELOG.md)** - 版本变更记录

## ❓ 常见问题

**Q: 为什么需要 Shizuku?**  
A: 提供系统级权限,可模拟真实人类操作,无需 root

**Q: 支付操作安全吗?**  
A: 系统自动检测支付场景并暂停,AI 永远不会自动完成支付

**Q: API 费用如何?**  
A: 硅基流动 API 约 ¥0.002/千 tokens,日常使用每月约 10 元

**Q: 支持哪些应用?**  
A: 理论上支持所有 Android 应用,标准 UI 组件效果最好

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🙏 致谢

- [Shizuku](https://github.com/RikkaApps/Shizuku) - 系统级权限框架
- [硅基流动](https://cloud.siliconflow.cn/) - AI API 服务
- [Qwen Team](https://github.com/QwenLM) - 视觉语言模型

---

⭐ 如果这个项目对你有帮助,欢迎 Star 支持!
