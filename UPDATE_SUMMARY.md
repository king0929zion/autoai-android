# 项目更新摘要

## Shizuku 通道兼容性
- `app/src/main/java/com/autoai/android/utils/ShizukuShell.kt`：通过反射尝试多套 `Shizuku.newProcess` 签名，适配 Shizuku 13.5.4 及后续版本；当无法匹配时返回友好错误而不是直接崩溃；统一处理超时与输出日志。
- `app/src/main/java/com/autoai/android/permission/ShizukuActionExecutor.kt`：全量改写 Shizuku 执行器，统一英文提示、改进文本转义、完善前台包名解析，并在权限缺失时向用户提供明确操作建议。
- `app/src/main/java/com/autoai/android/permission/ControlMode.kt`、`OperationExecutor.kt`：输出改为英文描述，未就绪时返回具体指导；`AccessibilityActionExecutor.kt` 同步调整失败提示。

## 无障碍通道稳定性
- `app/src/main/java/com/autoai/android/accessibility/AccessibilityBridge.kt`：优化 `setText` 的回退逻辑，避免复用已回收节点导致崩溃；更新截图回调实现以匹配最新 API，移除不存在的 `ScreenshotResult.close()` 调用，保持仅释放 `hardwareBuffer`。

## 设置页 UI 重构
- `app/src/main/java/com/autoai/android/ui/settings/SettingsScreen.kt`：
  - 采用 Material3 `FilterChip` + `AssistChip` 代替旧版 SegmentedButton，重写控制模式状态展示，并将关键提示转为英文；
  - 重新组织 API 表单、温度滑块、令牌输入与深色主题开关，统一校验与错误提示；
  - `SettingsViewModel` 支持持久化温度、最大 token、深色模式等配置，所有校验和错误提示改为英文；
  - 补充 `KeyboardOptions` 正确导入并清理多余引用，兼容 Compose 编译器。

## 文案与资源
- `app/src/main/res/values/strings.xml`：移除中文乱码，新增设置页、任务状态、提示气泡等英文字符串，确保 CI 编译环境不会因编码问题失败。

## 其他改动
- 在上述过程中多次同步 GitHub Actions 反馈的编译错误（如重复注解、缺失导入、过期 API 调用），逐项修复并推送，确保远程仓库始终保持可构建状态。

> 如需了解具体实现细节，可参阅对应文件中的注释与日志输出。若后续仍有新需求，请基于当前文档追加条目，保持更新可追踪。
