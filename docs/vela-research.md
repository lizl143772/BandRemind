# 手表端 Vela 快应用：可行性调研

> 调研时间：2026-10-21
> 结论摘要：**技术上能做出「手表端快应用」本体，但「手机不在身边也能准点震动」的离线提醒，在当前公开的 Vela 能力下没有被证实的可行路径。**
> 卡点不在开发难度，在平台权限。

---

## 1. 现状与目标

当前投递链是纯手机侧闭环：

```
规则引擎 → AlarmManager.setAlarmClock → 高优先级通知 → 小米运动健康镜像 → 手表震动
```

链路本身已验证跑通（`dumpsys alarm` 显示 `RTC_WAKEUP ... window=0 exactAllowReason=policy_permission`，走 `Alarm clock` 通道；到点触发后通知计数 `numPostedByApp` 递增）。

唯一的能力缺口是：**手机不在身边 → 手机不排程 → 手表不响。**

---

## 2. 三条候选路线

| 路线 | 做法 | 离线 | 成本 | 结论 |
|---|---|---|---|---|
| **A. 通知镜像（当前）** | 手机端排程 + 本地通知 + 小米运动健康镜像 | ❌ | 低（已完成） | 保持 |
| **B. Vela 快应用** | 手机规则同步到手表，手表本地计时 + 震动 | 取决于后台能力 | 中（2-3 周） | 本文档重点 |
| **C. BLE 直连** | 自研直连手表（参考 Gadgetbridge / huami-token） | ✅ | 高 | 不建议 |

### 先纠正一个易混点

**Vela 快应用 ≠ 快应用联盟。**

- 快应用联盟（`quickapp.cn`）是手机厂商联盟的框架，有 `system.alarm` 等接口。
- 小米 Vela 是小米基于 NuttX 自研的 IoT OS 平台（已以 Apache 2.0 开源为 [OpenVela](https://github.com/open-vela)），有**独立的开发文档与接口体系**：[iot.mi.com/vela/quickapp](https://iot.mi.com/vela/quickapp/zh/guide/)。

两套东西的能力不能互相套用 —— 后文会看到，这正是判断的关键。

---

## 3. Vela 快应用能力盘点（已核实）

### ✅ 有利因素

| 事实 | 出处 |
|---|---|
| **REDMI Watch 5 / 6 在官方支持列表内**（Redmi Watch 4、S3、手环 8 Pro / 9 均不支持） | [event 接口支持明细](https://iot.mi.com/vela/quickapp/zh/features/system/event.html) |
| `vibrator.vibrate` 支持 Watch 5 / 6 | [vibrator 支持明细](https://iot.mi.com/vela/quickapp/zh/features/system/vibrator.html) |
| 手机→手表同步是**官方通道**：手机端 `xms-wearable-lib` SDK + 手表端 `@system.interconnect` | [BandOTP 双端项目](https://github.com/lin945/mi_band_mfa) |
| 工具链成熟：`aiot` CLI 产出 `.rpk`，AstroBox 侧载安装 | BandOTP README |
| 本地存储 `@system.storage` 可用 | [Vela 官方文档](https://iot.mi.com/vela/quickapp/zh/features/data/storage.html) |
| 蓝牙 `@system.bluetooth` 存在 | [Vela 官方文档](https://iot.mi.com/vela/quickapp/zh/features/system/bluetooth.html) |

BandOTP（`lin945/mi_band_mfa`）几乎是本项目的翻版 —— 手机端管配置、手表端离线计算，走的正是 `xms-wearable-lib` + `system.interconnect`。
**它证明了「规则同步到手表」这条路是通的。**

### Vela JS 应用完整接口清单

| 分类 | 接口 |
|---|---|
| 基本功能 | `app` / `device` / `router` / `configuration` |
| 网络访问 | `fetch` / **`interconnect`** / `request` / `uploadtask` |
| 数据文件 | `storage` / `file` |
| 系统能力 | `network` / **`vibrator`** / `brightness` / `record` / `geolocation` / `sensor` / `event` / `battery` / `volume` / `zip` / `bluetooth` |
| 安全 | `crypto` |
| 其他 | `audio` / `prompt` |

**注意列表里没有 `alarm`，也没有 `notification`。**

---

## 4. 四个硬约束

### 约束 1：快应用退到后台会暂停运行

快应用框架的后台运行**只允许申请 4 类接口**：

- 音频播放 `system.audio`
- 录音 `system.record`
- 上传下载 `system.request`
- 地理位置 `system.geolocation`

且需同时满足三个条件：manifest 声明该接口 → 调用 Resident 接口启动后台模式 → 该接口正在运行。
后台运行时还会占用通知栏常驻通知（用户可强制结束）。

**定时器不在这 4 类里。**

`system.alarm` 接口在快应用文档里明确标注「后台运行限制：禁止使用」。

> [后台运行文档](https://doc.quickapp.cn/framework/background-running.html)

### 约束 2：Vela 没有 `alarm` 接口

快应用联盟的 `system.alarm` 能力很强 —— 能写系统级闹钟、支持 `days` 重复周期（`[0,1,2,3,4]` = 周一到周五）、可设震动。**但它不在 Vela 的接口清单里。**

Vela 的 `event` 接口只提供三个系统公共事件：`BATTERY_CHANGED` / `CHARGING` / `DISCHARGING`，**没有任何定时唤醒事件**。

同时 Vela 没有 `notification` 接口 → **手表端快应用不能自己弹系统通知**，只能震动 + 自己的界面。

### 约束 3：震动能力被砍了一半

| 接口 | REDMI Watch 5 / 6 |
|---|---|
| `vibrate` | ✅ 支持（`long` / `short` 两档） |
| `start`（自定义时长 / 间隔 / 次数） | ❌ 不支持 |
| `stop` | ❌ 不支持 |
| `getSystemDefaultMode` | ❌ 不支持 |

做不出「三短一长」这类震动模式，只能"哔"或"嗡"一下。

### 约束 4（最硬）：快应用分区只有 ~8MB，且用户删不掉

Vela 设备的 OTA 包里有一个 `vela_quickapp` 分区，存放系统预装的快应用 rpk。

- 分区大小固定约 **8MB**，撑满就装不了新快应用（提示"存储空间不足"）。
- 预装快应用在用户层面**无法真正删除**（「卸载系统应用」只是隐藏），恢复出厂设置会自动装回。
- 小米往里塞了一批小游戏 → **系统更新后直接撑满**，导致 Watch 5 升级后再也装不了任何快应用。
- 已知解法是刷改固件删掉预装快应用（作者实测能装 20 个），但**给分区扩容直接把手表刷成砖** —— 作者为此换了块新表。
- 普通固件大约能装 10 个快应用。

> [彻底解决 REDMI Watch 5 快应用数量限制问题及其原理解读](https://luming.cool/posts/2026/01/redmi-watch-5-quickapp-limit-crack/)

---

## 5. 可行性判断

把「离线提醒」拆成两问：

**问：手表端快应用本身能做出来吗？**
→ **能。** 规则存储、计时、震动、UI，技术上没有障碍。

**问：它能在手机不在身边时准点响吗？**
→ **目前没有已证实的路径。**

因为要准点响，就必须在那一刻有代码在执行，而：

| 想走的路 | 结果 |
|---|---|
| 后台定时器 | ❌ 退到后台即暂停 |
| 系统闹钟（`alarm`） | ❌ Vela 没有这个接口 |
| 定时唤醒事件 | ❌ `event` 只订阅得到电量/充电状态 |
| 表盘常驻 + 定时器 | ⚠️ 可行但已不是快应用，是「做一款带提醒的表盘」 |

**所以 Vela 快应用能做到的是：手表端显示规则 + 收到手机推送时立即响应。做不到离线闹钟。**

### 形态 B2：表盘（唯一理论可行的离线形态）

Vela 表盘用 Lua 编写且**常驻运行**（参考小米手环 9 Pro 开发指南中的 `\watchface\fprj\app\lua\main.lua`），可以在表盘里跑定时器 + 震动。

但这意味着：
- 形态从「快应用」变成「一款带提醒功能的表盘」，交互、占位、升级路径全变
- 8MB 分区更紧张（表盘通常比快应用大）
- 用户得把表盘设成当前表盘才有提醒

---

## 6. 建议的下一步：一次最小验证

与其继续查文档，不如用最小代码把三个未知数打掉。

写一个最简 Vela 快应用（`app.ux` + `manifest.json`），在 Watch 5 上装（走 AstroBox 侧载），验三件事：

1. **退到后台后 JS 定时器还跑不跑？**
   `setInterval` 里调 `vibrator.vibrate`，退到后台观察是否还在振。
   **这是生死判据** —— 如果停，路线 B 只剩表盘形态，可以直接省掉后面所有工作。

2. **`interconnect` 能不能从手机侧收到消息并触发震动？**
   手机端用 `xms-wearable-lib` 发一条，验证整条同步链是否通。

3. **当前固件还装得下吗？**
   顺带量一下 `vela_quickapp` 分区剩余空间，确认用户未来 OTA 后会不会突然装不上。

第 1 项的结果直接决定要不要投入后面的 2-3 周。

---

## 7. 如果要做：工程量与风险

### 工程量

| 阶段 | 估计 |
|---|---|
| 工具链学习 + 模拟器 / 真机部署 | 1-2 天 |
| 手表端快应用（规则存储 + 计时 + 震动 + UI） | 2-3 天 |
| 手机端集成 `xms-wearable-lib`（设备连接 + 设备管理权限 + 同步） | 3-5 天 |
| 联调 + 真机验证 | 3-5 天 |
| **合计** | **约 2-3 周**（比 BLE 直连小一个量级） |

### 主要风险

| 风险 | 说明 |
|---|---|
| **分发受限** | 自签名 `.rpk` 只能让用户手动侧载（AstroBox），上不了应用商店，普通用户装不了 |
| **固件迭代风险** | 接口支持列表随机型变化（Redmi Watch 4 不支持 `event`、Watch 5 支持），说明平台仍在动 |
| **分区随时可能爆** | 用户一次 OTA 就可能装不上，需要在 App 里给出明确提示与降级指引 |
| **权限与体验成本** | 用户要装 AstroBox、授设备管理权限、装快应用，链路很长 |
| **电池** | 若最终走表盘形态，续航是硬账 |
| **功能上限** | 免打扰时段需要在手机侧把当天时点展开成具体时刻列表再同步（现有的 `NextTriggerCalculator` 正好能承担这个展开） |

---

## 8. 参考

- [Xiaomi Vela JS 应用开发文档](https://iot.mi.com/vela/quickapp/zh/guide/) —— 官方
- [Vela event 接口（含设备支持明细表）](https://iot.mi.com/vela/quickapp/zh/features/system/event.html)
- [Vela vibrator 接口（含设备支持明细表）](https://iot.mi.com/vela/quickapp/zh/features/system/vibrator.html)
- [Vela interconnect 设备通信](https://iot.mi.com/vela/quickapp/zh/features/network/interconnect.html)
- [快应用后台运行机制](https://doc.quickapp.cn/framework/background-running.html)
- [快应用 system.alarm 接口](https://doc.quickapp.cn/features/system/alarm.html)
- [BandOTP 手表端（Vela 快应用范例）](https://github.com/lin945/mi_band_mfa)
- [BandOTP 安卓端（xms-wearable-lib 同步范例）](https://github.com/lin945/mi_band_mfa_android)
- [Redmi Watch 5 快应用数量限制原理解读](https://luming.cool/posts/2026/01/redmi-watch-5-quickapp-limit-crack/)
- [Xiaomi Vela 模拟器（安卓上跑 Vela，含 Watch 5 模板）](https://github.com/Gsjsjzhznsz/XiaomiVelaSimulator)
- [OpenVela 开源仓库](https://github.com/open-vela)
- [AstroBox（快应用侧载工具）](https://www.bgrdh.com/sites/65263.html)
- [小米手环 9 Pro Vela 系统开发指南（表盘 Lua）](https://www.mugyu.cc/article/小米手环9pro-vela系统开发指南)
