package com.example.callservice

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "callService.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "uuids"
        private const val COLUMN_ID = "id"
        private const val COLUMN_UUID = "uuid"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_UUID TEXT)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Метод для добавления UUID в базу данных
    fun addUUID(uuid: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_UUID, uuid)
        val result = db.insert(TABLE_NAME, null, contentValues)
        return result != -1L
    }

    // Метод для получения сохраненного UUID из базы данных
    @SuppressLint("Range")
    fun getUUID(): String? {
        val db = this.readableDatabase
        val selectQuery = "SELECT $COLUMN_UUID FROM $TABLE_NAME LIMIT 1"
        val cursor: Cursor? = db.rawQuery(selectQuery, null)
        var uuid: String? = null
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                uuid = cursor.getString(cursor.getColumnIndex(COLUMN_UUID))
            }
            cursor.close()
        }
        return uuid
    }

    // Метод для удаления всех UUID из базы данных (опционально)
    fun clearUUIDs() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
    }
}
