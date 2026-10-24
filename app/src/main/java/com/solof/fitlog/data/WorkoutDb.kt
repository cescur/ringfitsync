package com.solof.fitlog.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Historial local de entrenamientos. */
class WorkoutDb(context: Context) : SQLiteOpenHelper(context, "fitlog.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE workouts (
              id TEXT PRIMARY KEY NOT NULL,
              date TEXT NOT NULL,
              start_time TEXT NOT NULL,
              duration_sec INTEGER NOT NULL,
              kcal REAL,
              distance_m INTEGER,
              in_health_connect INTEGER NOT NULL DEFAULT 0,
              created_at TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX workouts_date ON workouts(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    private fun Cursor.toWorkout() = Workout(
        date = LocalDate.parse(getString(getColumnIndexOrThrow("date"))),
        startTime = LocalTime.parse(getString(getColumnIndexOrThrow("start_time"))),
        durationSec = getInt(getColumnIndexOrThrow("duration_sec")),
        kcal = getColumnIndexOrThrow("kcal").let { if (isNull(it)) null else getDouble(it) },
        distanceM = getColumnIndexOrThrow("distance_m").let { if (isNull(it)) null else getInt(it) },
        inHealthConnect = getInt(getColumnIndexOrThrow("in_health_connect")) == 1,
    )

    private fun query(selection: String?, args: Array<String>?): List<Workout> =
        readableDatabase.query("workouts", null, selection, args, null, null, "date DESC, start_time DESC").use { c ->
            buildList { while (c.moveToNext()) add(c.toWorkout()) }
        }

    fun list(): List<Workout> = query(null, null)

    fun onDates(dates: Collection<LocalDate>): List<Workout> {
        if (dates.isEmpty()) return emptyList()
        return query("date IN (${dates.joinToString(",") { "?" }})", dates.map { it.toString() }.toTypedArray())
    }

    fun save(w: Workout) {
        val values = ContentValues().apply {
            put("id", w.id)
            put("date", w.date.toString())
            put("start_time", w.startTime.toString())
            put("duration_sec", w.durationSec)
            put("kcal", w.kcal)
            put("distance_m", w.distanceM)
            put("in_health_connect", if (w.inHealthConnect) 1 else 0)
            put("created_at", Instant.now().toString())
        }
        writableDatabase.insertWithOnConflict("workouts", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun delete(id: String) {
        writableDatabase.delete("workouts", "id = ?", arrayOf(id))
    }
}
