package com.example.callservice

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ErrorActivity : AppCompatActivity() {

    private lateinit var pauseReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val phoneNumber = intent.getStringExtra("PHONE_NUMBER")
        Log.d("PHONE NUMBER", "$phoneNumber")
        val title = intent.getStringExtra("DIALOG_TITLE") ?: getString(R.string.dialog_title) // Используем переданный title
        val message = intent.getStringExtra("DIALOG_MESSAGE") ?: getString(R.string.dialog_message) // Используем переданный message

        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("$message $phoneNumber")
            .setPositiveButton("Позвонить") { dialog, which ->
                if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    val callIntent = Intent(Intent.ACTION_CALL)
                    callIntent.data = Uri.parse("tel:$phoneNumber")
                    startActivity(callIntent)
                    dialog.dismiss()
                    finish()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), CALL_PHONE_PERMISSION_REQUEST_CODE)
                }
            }
            .setCancelable(false)

        val dialog = builder.create()
        dialog.show()

        // Регистрация broadcast receiver для обработки остановки активности
        pauseReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "com.example.callservice.PAUSE_ERROR_ACTIVITY" -> {
                        dialog.dismiss()
                    }
                    "com.example.callservice.RESUME_ERROR_ACTIVITY" -> {
                        dialog.show()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction("com.example.callservice.PAUSE_ERROR_ACTIVITY")
            addAction("com.example.callservice.RESUME_ERROR_ACTIVITY")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(pauseReceiver, filter, RECEIVER_NOT_EXPORTED)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pauseReceiver)
    }

    companion object {
        private const val CALL_PHONE_PERMISSION_REQUEST_CODE = 123
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val phoneNumber = intent.getStringExtra("PHONE_NUMBER")
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:$phoneNumber")
                startActivity(callIntent)
            } else {
                // Обработка, если разрешение не предоставлено
            }
        }
    }
}
