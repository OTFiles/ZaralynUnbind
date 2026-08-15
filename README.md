# ZaralynUnbind

通过发送 `ACTIONG_RESET` 广播触发家长管理 SyncDataService 执行设备解绑。

## 原理

家长管理 App 的 `SyncDataService` 组件 `exported=true` 且无权限保护，注册了 `com.readboy.parentmanager.ACTIONG_RESET` action。任何应用均可向该组件发送 intent，触发设备解绑流程：

1. 发送 `ACTIONG_RESET` 广播/服务 intent → SyncDataService
2. SyncDataService 向 `https://parentadmin.readboy.com/v1/machine/cancel_bindings` 发送解绑请求
3. 服务端解除平板与家长账户的绑定
4. 本地数据全清（密码、管控列表、使用记录、商城控制等）

## 构建

```bash
gradle assembleDebug
```

## 安装

1. 从 [Releases](https://github.com/OTFiles/ZaralynUnbind/releases) 下载 APK
2. 安装到设备
3. 打开应用，点击「解绑设备」
4. 确认操作

## 注意

- 需要家长管理已安装（目标包名: `com.readboy.parentmanager`）
- 需要网络连接
- 解绑后建议重启设备

## 免责声明

本文仅用于安全研究和教育目的。请确认你拥有设备的所有权。未经授权使用本文中的技术可能违反法律，作者不对任何滥用行为负责。