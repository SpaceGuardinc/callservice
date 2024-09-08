package com.example.callservice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    private val REQUEST_CODE_READ_PHONE_STATE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверяем разрешение на чтение состояния телефона
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_CODE_READ_PHONE_STATE)
        } else {
            // Разрешение уже предоставлено
            setupOverlayPermission()
        }
    }

    private fun setupOverlayPermission() {
        overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Settings.canDrawOverlays(this)) {
                startMyService()
                moveTaskToBack(true)  // Уходит в фон
                finish()
            } else {
                finish()
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
        } else {
            startMyService()
            moveTaskToBack(true)  // Уходит в фон
            finish()
        }
    }

    private fun startMyService() {
        MyService.scheduleNextJob(this)
    }

    private fun sendPhoneNumberToApi(phoneNumber: String) {
        val client = OkHttpClient()
        val bearerToken = getString(R.string.bearer_token)
        val apiBaseUrl = getString(R.string.api_base_url)
        val registerEndpoint = getString(R.string.register_endpoint)
        // Замените на URL вашего API
        val registerUrl = "$apiBaseUrl$registerEndpoint"

        val jsonObject = JSONObject().apply {
            put("phone_number", phoneNumber)
        }

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonObject.toString()
        )

        val request = Request.Builder()
            .url(registerUrl)
            .post(requestBody)
            .addHeader("Authorization", bearerToken)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // Обработка ошибок
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    println("Response: $responseData")
                    // Обработка успешного ответа
                } else {
                    println("Response error: ${response.code}")
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_PHONE_STATE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение предоставлено
                setupOverlayPermission()
            } else {
                // Разрешение отклонено
                finish() // Завершите активность, если разрешение не предоставлено
            }
        }
    }
}
