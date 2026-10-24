package cl.cescur.fitlog.data

import cl.cescur.fitlog.parser.formatDuration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class PeriodStats(
    val key: String,
    val label: String,
    val sessions: Int,
    val totalSec: Int,
    val totalKcal: Double,
    val totalM: Int,
) {
    val avgSec: Int get() = totalSec / sessions
    val avgKcal: Double get() = totalKcal / sessions
    val avgM: Int get() = totalM / sessions
}

private val MONTHS_ES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Lunes de la semana de la fecha dada. */
fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

private fun summarize(groups: Map<String, List<Workout>>, label: (String) -> String): List<PeriodStats> =
    groups.entries
        .sortedByDescending { it.key }
        .map { (key, ws) ->
            PeriodStats(
                key = key,
                label = label(key),
                sessions = ws.size,
                totalSec = ws.sumOf { it.durationSec },
                totalKcal = ws.sumOf { it.kcal ?: 0.0 },
                totalM = ws.sumOf { it.distanceM ?: 0 },
            )
        }

fun weeklyStats(ws: List<Workout>): List<PeriodStats> =
    summarize(ws.groupBy { weekStart(it.date).toString() }) { key ->
        val s = LocalDate.parse(key)
        val e = s.plusDays(6)
        "${s.dayOfMonth} ${MONTHS_ES[s.monthValue - 1]} – ${e.dayOfMonth} ${MONTHS_ES[e.monthValue - 1]} ${e.year}"
    }

fun monthlyStats(ws: List<Workout>): List<PeriodStats> =
    summarize(ws.groupBy { YearMonth.from(it.date).toString() }) { key ->
        val ym = YearMonth.parse(key)
        "${MONTHS_ES[ym.monthValue - 1]} ${ym.year}"
    }

fun toCsv(ws: List<Workout>): String {
    val header = "fecha,hora_inicio,duracion,duracion_seg,kcal,distancia_km"
    val lines = ws.sortedBy { it.date }.map { w ->
        listOf(
            w.date.toString(),
            w.startTime.toString(),
            formatDuration(w.durationSec),
            w.durationSec.toString(),
            w.kcal?.let { formatPlain(it) } ?: "",
            w.distanceM?.let { String.format(Locale.ROOT, "%.2f", it / 1000.0) } ?: "",
        ).joinToString(",")
    }
    return (listOf(header) + lines).joinToString("\n") + "\n"
}

/** 45.0 → "45", 74.25 → "74.25". */
private fun formatPlain(v: Double): String =
    if (v == Math.floor(v)) v.toLong().toString() else v.toBigDecimal().stripTrailingZeros().toPlainString()
