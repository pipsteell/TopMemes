package com.example.topmemes.helpers

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.topmemes.data.ImageItem
import java.io.ByteArrayOutputStream

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "memes.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_MEMES = "memes"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_IMAGE = "image"

        private const val CREATE_TABLE = """
            CREATE TABLE $TABLE_MEMES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_IMAGE BLOB NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEMES")
        onCreate(db)
    }

    fun addMeme(title: String, bitmap: Bitmap): Long {
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_IMAGE, bitmapToBytes(bitmap))
        }
        return writableDatabase.insert(TABLE_MEMES, null, values)
    }

    fun getAllMemes(): List<ImageItem> {
        val memes = mutableListOf<ImageItem>()
        val cursor = readableDatabase.query(
            TABLE_MEMES,
            arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_IMAGE),
            null, null, null, null,
            "$COLUMN_ID DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID))
                val title = it.getString(it.getColumnIndexOrThrow(COLUMN_TITLE))
                val imageBytes = it.getBlob(it.getColumnIndexOrThrow(COLUMN_IMAGE))
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                memes.add(ImageItem(id, title, bitmap))
            }
        }
        return memes
    }

    fun deleteMemes(ids: List<Int>): Int {
        if (ids.isEmpty()) return 0
        return writableDatabase.delete(
            TABLE_MEMES,
            "$COLUMN_ID IN (${ids.joinToString(", ") { "?" }})",
            ids.map { it.toString() }.toTypedArray()
        )
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        return ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
        }.toByteArray()
    }
}