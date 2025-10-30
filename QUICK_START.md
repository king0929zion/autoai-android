# ⚡ 快速开始 - 30秒推送到GitHub

本指南帮助你在30秒内将项目推送到GitHub并开始自动编译。

---

## 🚀 超快速推送（3步）

### 步骤 1: 创建 GitHub 仓库
1. 打开 https://github.com/new
2. 仓库名: `autoai-android`
3. 可见性: **Public**（推荐，免费Actions）
4. **不要**勾选任何选项
5. 点击 **Create repository**

### 步骤 2: 运行推送脚本
双击项目根目录的 `push_to_github.bat` 文件

或在命令行执行：
```bash
cd F:\autoaiAndroid
push_to_github.bat
```

### 步骤 3: 按提示操作
1. 输入 GitHub 用户名和邮箱（首次）
2. 输入仓库地址（格式：`https://github.com/你的用户名/autoai-android.git`）
3. 输入提交信息（可直接回车使用默认）
4. 等待推送完成

---

## ✅ 推送成功后

### 立即查看
1. **代码仓库**: https://github.com/你的用户名/autoai-android
2. **自动编译**: 点击 `Actions` 标签

### 等待 3-5 分钟
- GitHub Actions 自动编译项目
- 编译完成后自动上传 APK

### 下载 APK
1. 进入 Actions → 点击最新的成功构建
2. 滚动到底部 `Artifacts` 区域
3. 下载：
   - `app-debug.apk` - Debug 版本
   - `app-release-unsigned.apk` - Release 版本

---

## 🔄 后续更新

修改代码后推送：
```bash
# 方法 1: 使用脚本
push_to_github.bat

# 方法 2: 手动推送
git add .
git commit -m "你的修改说明"
git push
```

每次推送都会自动触发编译！

---

## ❓ 遇到问题？

### 推送失败
**错误**: `Authentication failed`

**解决**:
1. GitHub 已停用密码认证
2. 需要使用 Personal Access Token (PAT)

**获取 Token**:
1. 访问 https://github.com/settings/tokens
2. 点击 `Generate new token (classic)`
3. 勾选 `repo` 权限
4. 生成并复制 Token
5. 推送时用 Token 替代密码

**或使用 SSH**:
```bash
# 生成 SSH 密钥
ssh-keygen -t ed25519 -C "你的邮箱"

# 添加到 GitHub
# 1. 复制公钥: cat ~/.ssh/id_ed25519.pub
# 2. 访问 https://github.com/settings/ssh/new
# 3. 粘贴公钥并保存

# 修改远程地址为 SSH
git remote set-url origin git@github.com:你的用户名/autoai-android.git
```

### 编译失败
1. 查看 Actions 中的错误日志
2. 通常是代码问题，修复后重新推送
3. 查看 [故障排查](GITHUB_DEPLOY_GUIDE.md#故障排查)

### 找不到 APK
- 确认构建状态为绿色 ✅
- 只有成功的构建才会上传 APK
- 检查是否被广告拦截器阻止下载

---

## 📖 更多信息

详细说明请查看：
- **完整部署指南**: [GITHUB_DEPLOY_GUIDE.md](GITHUB_DEPLOY_GUIDE.md)
- **修复报告**: [FIX_AND_OPTIMIZATION_REPORT.md](FIX_AND_OPTIMIZATION_REPORT.md)
- **项目文档**: [README.md](README.md)

---

## 🎉 完成！

现在你可以：
- ✅ 无需本地编译
- ✅ 自动构建 APK
- ✅ 随时下载最新版本
- ✅ 追踪构建历史
- ✅ 与他人协作开发

**祝使用愉快！** 🚀
