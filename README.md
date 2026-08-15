# ZaralynUnbind

通过**直接调用 `cancel_bindings` API** 触发家长管理设备解绑。

## 背景

**重要发现**：在家长管理 6.2.8 中，`ACTIONG_RESET` 广播不再触发解绑——它被重映射到 `MSG 0x26`（UploadControlExtendResponse，控制扩展上传），而非真正的 `MSG_RESET(0x25)`（UploadReSetStatusResponse → cancel_bindings）。

真实解绑只能由以下方式触发：
1. **云端下发 `reset.status==1`**（全量拉取时判断）
2. **直接调用 `cancel_bindings` API**（本工具采用的方式）

## 原理

本工具直接向 `https://parentadmin.readboy.com/v1/machine/cancel_bindings` 发送 POST 请求，使用已知的签名算法（密钥硬编码在 APK 中）：

```java
signature = MD5(秒时间戳 + APPSECRET + MD5(APP_ID2))
APPSECRET = "de917e0e6b4962061d66d24f6cfdb5bf0d1b9b39"
APP_ID2   = "parent-manage"
```

## 构建

```bash
gradle assembleDebug
```

## 安装

1. 从 [Releases](https://github.com/OTFiles/ZaralynUnbind/releases) 下载 APK
2. 安装到设备
3. 打开应用，点击「解绑设备」
4. 确认操作

## 权限

- `INTERNET` — 发送 HTTP 请求
- `READ_PHONE_STATE` — 获取设备序列号（用于 API 签名）
  - 如果未授予，会尝试 root (`getprop ro.serialno`) 获取

## 免责声明

本文仅用于安全研究和教育目的。请确认你拥有设备的所有权。未经授权使用本文中的技术可能违反法律，作者不对任何滥用行为负责。