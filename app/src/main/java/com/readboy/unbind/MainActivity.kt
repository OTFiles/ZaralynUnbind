package com.readboy.unbind

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: MaterialTextView
    private lateinit var unbindButton: MaterialButton
    private lateinit var detailInfo: MaterialTextView
    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        unbindButton = findViewById(R.id.unbind_button)
        detailInfo = findViewById(R.id.detail_info)

        unbindButton.setOnClickListener {
            showUnbindConfirmDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun showUnbindConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title)
            .setMessage(R.string.dialog_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ -> doUnbind() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun doUnbind() {
        unbindButton.isEnabled = false
        unbindButton.text = "操作中..."
        statusText.text = "正在执行解绑..."

        // 方式1: 发送广播 (对旧版本可能有效)
        try {
            val intent = Intent("com.readboy.parentmanager.ACTIONG_RESET").apply {
                `package` = "com.readboy.parentmanager"
            }
            startService(intent)
        } catch (_: Exception) {}

        // 方式2: 直接调用 HTTP API (对 6.2.8 有效)
        scope.launch {
            val result = directHttpUnbind()
            handleResult(result)
        }
    }

    private suspend fun directHttpUnbind(): UnbindResult = withContext(Dispatchers.IO) {
        try {
            val serial = getDeviceSerial()
            if (serial == null || serial == "" || serial == "unknown") {
                return@withContext UnbindResult(
                    false, "无法获取设备序列号，请授予 READ_PHONE_STATE 权限或使用 root\n" +
                        "尝试的其他方式: 广播已发送，但新版家长管理(6.2.8)不再响应 ACTIONG_RESET 广播。"
                )
            }

            val timestampMs = System.currentTimeMillis()
            val seconds = timestampMs / 1000
            val appSecret = "de917e0e6b4962061d66d24f6cfdb5bf0d1b9b39"
            val appId2 = "parent-manage"

            // getSign2 算法: MD5(seconds + APPSECRET + MD5(APP_ID2))
            val md5AppId2 = md5(appId2)
            val signInput = "$seconds$appSecret$md5AppId2"
            val signature = md5(signInput)

            // GET 请求（POST 会返回 404！服务器只接受 GET）
            val params = "signature=$signature&imei=$serial&timestamp=$seconds&app_id=parent-manage"
            val url = URL("https://parentadmin.readboy.com/v1/machine/cancel_bindings?$params")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            // 设置 User-Agent 避免被某些服务器拦截
            conn.setRequestProperty("User-Agent", "ZaralynUnbind/1.0")

            val responseCode = conn.responseCode
            val responseBody = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            }
            conn.disconnect()

            return@withContext UnbindResult(true,
                "HTTP $responseCode\n响应: $responseBody\n\n" +
                "如果响应中 status=1，解绑成功。\n" +
                "如果 errno=7018(0x1B5A)，时间戳有误（可忽略，客户端可能已处理）。\n" +
                "如果 errno=7001，签名验证失败，可能是密钥过期或服务器已更新。\n" +
                "建议重启设备确认。"
            )
        } catch (e: Exception) {
            return@withContext UnbindResult(false,
                "HTTP 请求失败: ${e.message}\n\n" +
                "请检查网络连接和 HTTPS 证书。"
            )
        }
    }

    private fun getDeviceSerial(): String? {
        // API 26+ 需要 READ_PHONE_STATE 权限
        return try {
            if (Build.VERSION.SDK_INT >= 26) {
                Build.getSerial()
            } else {
                Build.SERIAL
            }
        } catch (e: SecurityException) {
            // 无权限，尝试通过系统属性读取（需要 root）
            try {
                val proc = Runtime.getRuntime().exec(arrayOf("getprop", "ro.serialno"))
                proc.inputStream.bufferedReader().readText().trim().ifEmpty { null }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun handleResult(result: UnbindResult) {
        if (result.success) {
            statusText.text = "解绑请求已发送"
            statusText.setTextColor(getColor(R.color.status_success))
            unbindButton.text = "已完成"
            detailInfo.text = result.message
        } else {
            statusText.text = "解绑失败"
            statusText.setTextColor(getColor(R.color.status_error))
            unbindButton.isEnabled = true
            unbindButton.text = "重试"
            detailInfo.text = result.message
            Toast.makeText(this, "解绑失败: ${result.message.take(50)}", Toast.LENGTH_LONG).show()
        }
    }

    data class UnbindResult(val success: Boolean, val message: String)
}