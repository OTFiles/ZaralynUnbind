package com.readboy.unbind

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: MaterialTextView
    private lateinit var unbindButton: MaterialButton
    private lateinit var infoText: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        unbindButton = findViewById(R.id.unbind_button)
        infoText = findViewById(R.id.info_text)

        unbindButton.setOnClickListener {
            showUnbindConfirmDialog()
        }
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
        try {
            val intent = Intent("com.readboy.parentmanager.ACTIONG_RESET").apply {
                `package` = "com.readboy.parentmanager"
            }

            // 尝试 startService（标准方式）
            try {
                startService(intent)
                statusText.text = getString(R.string.status_unbind_sent)
                statusText.setTextColor(getColor(R.color.status_success))
                unbindButton.isEnabled = false
                unbindButton.text = getString(R.string.button_done)
                infoText.text = getString(R.string.info_hint)
            } catch (e: Exception) {
                // 如果 startService 失败，尝试 sendBroadcast（备选）
                val broadcastIntent = Intent("com.readboy.parentmanager.ACTIONG_RESET")
                sendBroadcast(broadcastIntent)
                statusText.text = getString(R.string.status_broadcast_sent)
                statusText.setTextColor(getColor(R.color.status_success))
                unbindButton.isEnabled = false
                unbindButton.text = getString(R.string.button_done)
                infoText.text = getString(R.string.info_hint)
            }
        } catch (e: Exception) {
            statusText.text = getString(R.string.status_failed, e.message ?: "未知错误")
            statusText.setTextColor(getColor(R.color.status_error))
            Toast.makeText(this, R.string.toast_failed, Toast.LENGTH_LONG).show()
        }
    }
}