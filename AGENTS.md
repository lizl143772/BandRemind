# AGENTS.md

腕上提醒 BandRemind —— Android 定时提醒 App。手机端用 AlarmManager 精确闹钟 + 高优先级通知，经小米运动健康「同步所有通知」镜像到 Redmi Watch 5/6 手表震动。

Kotlin + Jetpack Compose，无 Hilt/Dagger、无网络请求、无账号、无广告。

## Setup commands

- 环境：JDK 17（Corretto 17.0.12）、Android SDK（compileSdk 35 / build-tools 35.0.0）
- `local.properties` 指向本机 SDK：`sdk.dir=E\:\\AndroidStudio\\sdk`（不要提交，已在 .gitignore）
- **Gradle wrapper 下载会超时**，用已解压的 gradle 并用 `-p` 指定工程目录（不要 `cd`）：
  `& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.7-all\549ctztx5g5jixmk70l9ykwir\gradle-8.7\bin\gradle.bat" -p <repo> <task>`
- 构建 debug：`:app:assembleDebug`
- 构建正式包：`:app:assembleRelease`（需要 `keystore.properties`，缺失时产出未签名包）
- 单元测试：`:app:testDebugUnitTest`
- 装包：`adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Project layout

- `app/src/main/java/com/lizl/bandremind/` — 主源码
  - `data/` — `Reminder` 模型、JSON 编解码、DataStore 仓库（规则整体存单 key `reminder_rules_v1`）
  - `logic/` — 纯函数：`NextTriggerCalculator`（触发时刻、进度、免打扰活动片段）、`TimelinePolicy`（今日提醒折叠策略）
  - `schedule/` — `ReminderScheduler`，`setAlarmClock` 精确闹钟，无权限时降级 `setAndAllowWhileIdle`
  - `notify/` — 通知渠道与投递
  - `receiver/` — 闹钟触发、通知动作（完成/稍后）、开机与时区变更恢复
  - `ui/` — Compose 界面：Home / RuleEdit / Template / Settings + `ui/theme/`
- `app/src/test/` — JVM 单元测试，46 项
- `app/src/androidTest/` — 真机 UI 测试，6 项
- `app/src/debug/` — 仅 debug 源的调试广播接收器（release 不含，用 `aapt dump xmltree` 验证）
- `docs/` — README 引用的截图 + 长文调研

## Code style

- **注释与 UI 文案一律中文**
- 可测逻辑一律下沉到 `logic/` 做成纯函数，UI 只做渲染；折叠规则见 `TimelinePolicy`
- 免打扰用「活动片段」建模：每段独立对齐段起点，午休结束即刻恢复提醒
- 数据模型字段命名保持一致，新增字段同步改 `data/ReminderCodec.kt`

## Testing instructions

- 单元测试：`gradle -p <repo> :app:testDebugUnitTest` —— 纯逻辑改动必须补单测
- 真机 UI 测试：
  `adb shell am instrument -w -e class com.lizl.bandremind.BandRemindUiTest 'com.lizl.bandremind.debug.test/androidx.test.runner.AndroidJUnitRunner'`
- 调试数据（仅 debug 包）：`adb shell am broadcast -a com.lizl.bandremind.debug.SEED| CLEAR | FIRE | LIST -n 'com.lizl.bandremind.debug/com.lizl.bandremind.debug.DebugReceiver'`
- 全部测试通过再提 PR

## PR & commit conventions

- 分支从 `main` 切，不要直接推 main
- 提交信息用 conventional commits（`feat:` / `fix:` / `build:` / `docs:`）
- 用 `gh pr create` 开 PR

## Security

- **签名材料绝不入库**：`*.jks`、`keystore.properties` 已 gitignore；仓库是公开的，泄露私钥等于把签名权送人
- 后续版本必须用同一份签名，否则用户无法覆盖升级
- `local.properties` 含本机路径，已忽略
- 华为设备上 App 写不进 logcat，调试广播靠「界面是否变化」判定，不要依赖 logcat

## 已知踩坑（动手前先看）

- **国产 ROM 会拦进程内启动 Activity**（`ActivityTaskManager` result code=102），`ActivityScenario` 走不通。真机 UI 测试用 `UiAutomation.executeShellCommand` 以 shell 身份开页面，配 shell `input tap/swipe` 真实点击，再从无障碍树读文本
- **Compose 的 a11y 文本是 `SpannedString`**，比较必须 `text?.toString()`，不能直接 `==`
- **深链必须带显式组件**：`am start -a VIEW -d <uri> -n <pkg>/<activity>`，否则系统按多应用匹配会取消（result code=-91）
- **MIUI/HyperOS 上 shell 没有 `INJECT_EVENTS`**，`input tap` 报 SecurityException；华为 Mate 60 Pro 上可用
- 长列表断言用「可见条目数增加」而不是固定条数，按钮会滚出屏幕导致 flaky
- `NextTriggerCalculator.next()` 语义是**不含当前时刻**（`isAfter`）
- bash 里 `cd` 后再写文件、`Copy-Item`/`Move-Item`/`Remove-Item -Recurse` 会触发安全策略拦截 → 用 `gradle -p` 指定目录，删文件用单条 `rm -- "<path>"`
- 抓屏归档直接 `adb pull` 落到 `docs/`，不要用 `Copy-Item`
