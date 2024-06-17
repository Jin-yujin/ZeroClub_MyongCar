package com.example.mju_car.com.example.mju_car.dbo

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.mju_car.dbo.User

class UserDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // 데이터베이스 정보
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "UserDatabase"
        private const val TABLE_NAME = "User"
        private const val COLUMN_USER_ID = "UserID"
        private const val COLUMN_CURRENT_LOCATION = "CurrentLocation"
        private const val COLUMN_NICKNAME = "NickName"
        private const val COLUMN_PW = "PW"
        private const val COLUMN_NAME = "Name"
        private const val COLUMN_CLASS_NUM = "ClassNum"
        private const val COLUMN_PICTURE = "Picture"
        private const val COLUMN_EMAIL = "Email"
        private const val COLUMN_DATE_TIME = "DateTime"
    }

    // 데이터베이스 테이블 생성
    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_USER_ID TEXT NOT NULL," +
                "$COLUMN_CURRENT_LOCATION REAL," +
                "$COLUMN_NICKNAME TEXT NOT NULL DEFAULT 'Name'," +
                "$COLUMN_PW TEXT NOT NULL," +
                "$COLUMN_NAME TEXT NOT NULL," +
                "$COLUMN_CLASS_NUM INTEGER NOT NULL," +
                "$COLUMN_PICTURE TEXT," +
                "$COLUMN_EMAIL TEXT NOT NULL," +
                "$COLUMN_DATE_TIME TEXT NOT NULL," +
                "PRIMARY KEY($COLUMN_USER_ID, $COLUMN_CURRENT_LOCATION, $COLUMN_NICKNAME)" +
                ")"
        db?.execSQL(CREATE_TABLE)
    }

    // 데이터베이스 업그레이드
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // 사용자 추가
    fun addUser(user: User): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, user.userID)
            put(COLUMN_CURRENT_LOCATION, user.currentLocation)
            put(COLUMN_NICKNAME, user.nickName)
            put(COLUMN_PW, user.password)
            put(COLUMN_NAME, user.name)
            put(COLUMN_CLASS_NUM, user.classNum)
            put(COLUMN_PICTURE, user.picture)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_DATE_TIME, user.dateTime)
        }
        val id = db.insert(TABLE_NAME, null, values)
        db.close()
        return id
    }

    // 사용자 조회
    @SuppressLint("Range")
    fun getUser(userID: String): User? {
        val db = this.readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userID),
            null,
            null,
            null,
            null
        )
        val user = if (cursor.moveToFirst()) {
            User(
                cursor.getString(cursor.getColumnIndex(COLUMN_USER_ID)),
                cursor.getDouble(cursor.getColumnIndex(COLUMN_CURRENT_LOCATION)),
                cursor.getString(cursor.getColumnIndex(COLUMN_NICKNAME)),
                cursor.getString(cursor.getColumnIndex(COLUMN_PW)),
                cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                cursor.getInt(cursor.getColumnIndex(COLUMN_CLASS_NUM)),
                cursor.getString(cursor.getColumnIndex(COLUMN_PICTURE)),
                cursor.getString(cursor.getColumnIndex(COLUMN_EMAIL)),
                cursor.getString(cursor.getColumnIndex(COLUMN_DATE_TIME))
            )
        } else {
            null
        }
        cursor.close()
        db.close()
        return user
    }

    // 사용자 업데이트
    fun updateUser(user: User): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CURRENT_LOCATION, user.currentLocation)
            put(COLUMN_NICKNAME, user.nickName)
            put(COLUMN_PW, user.password)
            put(COLUMN_NAME, user.name)
            put(COLUMN_CLASS_NUM, user.classNum)
            put(COLUMN_PICTURE, user.picture)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_DATE_TIME, user.dateTime)
        }
        val rowsAffected = db.update(
            TABLE_NAME,
            values,
            "$COLUMN_USER_ID = ?",
            arrayOf(user.userID)
        )
        db.close()
        return rowsAffected
    }

    // 사용자 삭제
    fun deleteUser(userID: String): Int {
        val db = this.writableDatabase
        val rowsDeleted = db.delete(
            TABLE_NAME,
            "$COLUMN_USER_ID = ?",
            arrayOf(userID)
        )
        db.close()
        return rowsDeleted
    }
}