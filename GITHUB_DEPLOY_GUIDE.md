# 📦 GitHub 部署和自动编译指南

本指南将帮助你将 AutoAI Android 项目推送到 GitHub，并使用 GitHub Actions 自动编译 APK。

---

## 🎯 优势

使用 GitHub Actions 云端编译的好处：
- ✅ **无需本地空间** - 编译在 GitHub 服务器上进行
- ✅ **自动化构建** - 每次推送代码自动触发编译
- ✅ **免费额度** - GitHub Actions 对公开仓库完全免费
- ✅ **构建历史** - 保存所有编译产物和日志
- ✅ **多版本支持** - 同时编译 Debug 和 Release 版本

---

## 📋 前置准备

### 1. 安装 Git
如果还没有安装 Git，请先安装：
- Windows: https://git-scm.com/download/win
- 安装后重启命令行工具

### 2. 配置 Git
```bash
git config --global user.name "你的用户名"
git config --global user.email "你的邮箱@example.com"
```

### 3. 创建 GitHub 账号
- 访问 https://github.com 注册账号（如果还没有）

---

## 🚀 推送到 GitHub

### 步骤 1: 初始化本地仓库

在项目目录下打开命令行，执行：

```bash
# 进入项目目录
cd F:\autoaiAndroid

# 初始化 Git 仓库
git init

# 添加所有文件到暂存区
git add .

# 创建首次提交
git commit -m "Initial commit: AutoAI Android v0.1.0-alpha"
```

### 步骤 2: 创建 GitHub 仓库

1. 登录 GitHub
2. 点击右上角 `+` → `New repository`
3. 填写仓库信息：
   - **Repository name**: `autoai-android`
   - **Description**: `AI 自主控机系统 - 基于 Shizuku + Qwen3-VL`
   - **Visibility**:
     - `Public` - 公开仓库（推荐，可用免费 Actions）
     - `Private` - 私有仓库（有限 Actions 额度）
   - **不要**勾选 "Add a README file"
   - **不要**勾选 "Add .gitignore"
   - **不要**勾选 "Choose a license"
4. 点击 `Create repository`

### 步骤 3: 关联远程仓库

GitHub 会显示推送命令，执行以下命令（替换为你的仓库地址）：

```bash
# 添加远程仓库（使用 HTTPS）
git remote add origin https://github.com/你的用户名/autoai-android.git

# 或者使用 SSH（需要先配置 SSH 密钥）
# git remote add origin git@github.com:你的用户名/autoai-android.git

# 重命名默认分支为 main
git branch -M main

# 推送代码到 GitHub
git push -u origin main
```

### 步骤 4: 验证推送

1. 刷新 GitHub 仓库页面
2. 应该能看到所有文件已上传
3. 等待约 1-2 分钟

---

## ⚙️ GitHub Actions 自动编译

### 查看构建状态

1. 在 GitHub 仓库页面，点击 `Actions` 标签
2. 应该能看到自动触发的构建任务 `Android CI`
3. 点击任务查看详细日志

### 构建流程

GitHub Actions 会自动执行：
1. ✅ 检出代码
2. ✅ 配置 JDK 17 环境
3. ✅ 缓存 Gradle 依赖
4. ✅ 执行编译 `./gradlew build`
5. ✅ 运行单元测试
6. ✅ 生成 Debug APK
7. ✅ 生成 Release APK（未签名）
8. ✅ 执行代码检查（Lint）
9. ✅ 上传编译产物

### 下载 APK

编译完成后：
1. 进入成功的 workflow run
2. 滚动到底部 `Artifacts` 区域
3. 下载：
   - `app-debug` - Debug 版本 APK
   - `app-release-unsigned` - Release 版本 APK（未签名）
   - `lint-results` - 代码检查报告

---

## 🔄 后续更新

每次修改代码后推送：

```bash
# 查看修改的文件
git status

# 添加修改的文件
git add .

# 提交更改
git commit -m "描述你的修改内容"

# 推送到 GitHub
git push
```

推送后会自动触发新的构建。

---

## 🎨 添加状态徽章

在 `README.md` 顶部添加构建状态徽章：

```markdown
[![Android CI](https://github.com/你的用户名/autoai-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/你的用户名/autoai-android/actions/workflows/android-ci.yml)
```

效果：显示绿色 ✅ 表示编译成功，红色 ❌ 表示失败。

---

## 🛠️ 高级配置

### 触发条件

当前配置会在以下情况触发编译：
- 推送到 `main`、`master` 或 `develop` 分支
- 创建针对这些分支的 Pull Request
- 手动触发（在 Actions 页面点击 "Run workflow"）

### 自定义配置

编辑 `.github/workflows/android-ci.yml` 文件可以：
- 修改触发分支
- 添加自动发布 Release
- 配置签名密钥（用于发布版本）
- 添加自动化测试
- 集成其他 CI/CD 工具

### 配置签名（可选）

如果要发布正式版本，需要配置签名：

1. **生成签名密钥**（在本地）:
```bash
keytool -genkey -v -keystore release.keystore -alias autoai -keyalg RSA -keysize 2048 -validity 10000
```

2. **将密钥转换为 Base64**:
```bash
# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | clip

# Linux/Mac
base64 release.keystore | pbcopy
```

3. **在 GitHub 添加 Secrets**:
   - 进入仓库 `Settings` → `Secrets and variables` → `Actions`
   - 添加以下 secrets:
     - `KEYSTORE_BASE64` - 上一步复制的 Base64 内容
     - `KEYSTORE_PASSWORD` - 密钥库密码
     - `KEY_ALIAS` - 密钥别名（如 autoai）
     - `KEY_PASSWORD` - 密钥密码

4. **修改 workflow 文件**（在 "Build Release APK" 步骤前添加）:
```yaml
    - name: Decode Keystore
      env:
        KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
      run: echo $KEYSTORE_BASE64 | base64 -d > release.keystore

    - name: Build Signed Release APK
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      run: |
        ./gradlew assembleRelease \
          -Pandroid.injected.signing.store.file=$(pwd)/release.keystore \
          -Pandroid.injected.signing.store.password=$KEYSTORE_PASSWORD \
          -Pandroid.injected.signing.key.alias=$KEY_ALIAS \
          -Pandroid.injected.signing.key.password=$KEY_PASSWORD
```

---

## 📊 监控构建

### 查看日志
- 点击 Actions 中的构建任务
- 展开每个步骤查看详细日志
- 红色 ❌ 表示失败，绿色 ✅ 表示成功

### 常见问题

#### 1. 编译失败
**原因**: 代码有错误或依赖问题
**解决**: 查看日志找到具体错误，修复后重新推送

#### 2. Gradle 依赖下载慢
**原因**: 网络问题
**解决**: 等待或在 `build.gradle.kts` 中添加国内镜像：
```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    google()
    mavenCentral()
}
```

#### 3. 构建超时
**原因**: GitHub Actions 有时间限制（6小时）
**解决**: 优化 Gradle 配置，启用缓存

---

## 🎯 最佳实践

### 分支策略
```
main/master - 稳定版本（自动编译和发布）
develop - 开发版本（自动编译测试）
feature/* - 功能分支（Pull Request 时编译）
```

### 提交规范
```bash
git commit -m "feat: 添加新功能"
git commit -m "fix: 修复某个bug"
git commit -m "docs: 更新文档"
git commit -m "style: 代码格式化"
git commit -m "refactor: 重构代码"
git commit -m "test: 添加测试"
git commit -m "chore: 更新依赖"
```

### 版本标签
```bash
# 创建版本标签
git tag -a v0.1.0 -m "Release v0.1.0-alpha"
git push origin v0.1.0

# GitHub Actions 可以配置为标签推送时自动发布 Release
```

---

## 📦 自动发布 Release（可选）

在 `.github/workflows/android-ci.yml` 末尾添加：

```yaml
  release:
    needs: build
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')

    steps:
    - name: Download APK artifacts
      uses: actions/download-artifact@v4

    - name: Create Release
      uses: softprops/action-gh-release@v1
      with:
        files: |
          app-debug/app-debug.apk
          app-release-unsigned/app-release-unsigned.apk
        body: |
          ## AutoAI Android ${{ github.ref_name }}

          ### 下载
          - **Debug 版本**: app-debug.apk
          - **Release 版本**: app-release-unsigned.apk

          ### 更新内容
          详见 [CHANGELOG.md](CHANGELOG.md)
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

现在推送标签会自动创建 GitHub Release：
```bash
git tag -a v0.1.0 -m "First alpha release"
git push origin v0.1.0
```

---

## 🔍 故障排查

### 推送被拒绝
```bash
# 如果远程有更新，先拉取
git pull origin main --rebase

# 然后再推送
git push origin main
```

### 忘记添加文件
```bash
# 查看未跟踪的文件
git status

# 添加遗漏的文件
git add 文件路径

# 修改最后一次提交
git commit --amend --no-edit

# 强制推送（谨慎使用）
git push -f origin main
```

### 敏感信息泄露
如果不小心推送了 API Key：
1. 立即在服务商处撤销该 Key
2. 使用 `git filter-branch` 或 `BFG Repo-Cleaner` 清理历史
3. 强制推送清理后的仓库

---

## 📞 获取帮助

- **GitHub Actions 文档**: https://docs.github.com/actions
- **Android CI 最佳实践**: https://developer.android.com/studio/build/building-cmdline
- **项目 Issues**: 在 GitHub 仓库提交问题

---

## ✅ 检查清单

部署前确认：
- [ ] Git 已安装并配置
- [ ] GitHub 账号已创建
- [ ] `.gitignore` 配置正确（已包含）
- [ ] GitHub Actions workflow 文件已创建
- [ ] 敏感信息（API Key）已移除
- [ ] 准备好仓库名称和描述

推送后确认：
- [ ] 代码已成功推送到 GitHub
- [ ] Actions 标签可见
- [ ] 首次构建已自动触发
- [ ] 构建成功（绿色 ✅）
- [ ] APK 文件可下载

---

## 🎉 完成！

现在你可以：
1. 在 GitHub 上协作开发
2. 每次推送自动编译
3. 下载编译好的 APK
4. 查看构建日志和测试报告
5. 通过 Pull Request 进行代码审查

**祝开发顺利！** 🚀

---

**最后更新**: 2025-10-30
**适用版本**: AutoAI Android v0.1.0-alpha
