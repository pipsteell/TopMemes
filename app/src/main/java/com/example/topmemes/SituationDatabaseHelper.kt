import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SituationDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "situations.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_SITUATIONS = "situations"
        const val COLUMN_ID = "id"
        const val COLUMN_TEXT = "text"
        const val COLUMN_TIMESTAMP = "timestamp"

        private const val CREATE_TABLE = """
            CREATE TABLE $TABLE_SITUATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SITUATIONS")
        onCreate(db)
    }

    fun addSituation(text: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEXT, text)
        }
        return db.insert(TABLE_SITUATIONS, null, values)
    }

    fun getAllSituations(): List<Situation> {
        val situations = mutableListOf<Situation>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SITUATIONS,
            arrayOf(COLUMN_ID, COLUMN_TEXT, COLUMN_TIMESTAMP),
            null, null, null, null,
            "$COLUMN_TIMESTAMP DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val text = it.getString(it.getColumnIndexOrThrow(COLUMN_TEXT))
                val timestamp = it.getString(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                situations.add(Situation(id, text, timestamp))
            }
        }
        return situations
    }

    fun updateSituation(id: Long, newText: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEXT, newText)
        }
        return db.update(
            TABLE_SITUATIONS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteSituation(id: Long): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_SITUATIONS,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }
}

data class Situation(
    val id: Long,
    val text: String,
    val timestamp: String
)