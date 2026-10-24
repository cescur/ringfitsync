package cl.cescur.fitlog.data

import android.content.Context
import androidx.core.content.edit
import cl.cescur.fitlog.parser.DateOrder
import java.time.LocalTime

/** Ajustes de la app, guardados en SharedPreferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var dateOrder: DateOrder
        get() = if (prefs.getString("dateOrder", null) == "mdy") DateOrder.MDY else DateOrder.DMY
        set(value) = prefs.edit { putString("dateOrder", if (value == DateOrder.MDY) "mdy" else "dmy") }

    /** La pantalla del juego no dice a qué hora entrenaste: se usa esta. */
    var defaultStartTime: LocalTime
        get() = prefs.getString("defaultStartTime", null)?.let(LocalTime::parse) ?: LocalTime.of(19, 0)
        set(value) = prefs.edit { putString("defaultStartTime", value.toString()) }
}
