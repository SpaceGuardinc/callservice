package com.example.callservice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class BackgroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundService", "Сервис запущен")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(applicationContext, BackgroundService::class.java)
        startService(restartIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BackgroundService", "Сервис уничтожен")
    }
}
