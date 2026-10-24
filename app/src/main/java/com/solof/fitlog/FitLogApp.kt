package com.solof.fitlog

import android.app.Application
import com.solof.fitlog.data.Settings
import com.solof.fitlog.data.WorkoutDb
import com.solof.fitlog.parser.OcrLine
import com.solof.fitlog.parser.ParsedWorkout

/** Resultado del último escaneo, que la pantalla de revisión recoge al abrirse. */
data class Scan(val imageUri: android.net.Uri, val lines: List<OcrLine>, val workouts: List<ParsedWorkout>)

class FitLogApp : Application() {
    val db by lazy { WorkoutDb(this) }
    val settings by lazy { Settings(this) }
    var pendingScan: Scan? = null
}
