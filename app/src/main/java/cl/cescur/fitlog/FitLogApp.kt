package cl.cescur.fitlog

import android.app.Application
import cl.cescur.fitlog.data.Settings
import cl.cescur.fitlog.data.WorkoutDb
import cl.cescur.fitlog.parser.OcrLine
import cl.cescur.fitlog.parser.ParsedWorkout

/** Resultado del último escaneo, que la pantalla de revisión recoge al abrirse. */
data class Scan(val imageUri: android.net.Uri, val lines: List<OcrLine>, val workouts: List<ParsedWorkout>)

class FitLogApp : Application() {
    val db by lazy { WorkoutDb(this) }
    val settings by lazy { Settings(this) }
    var pendingScan: Scan? = null
}
