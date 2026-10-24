// Lector puro de la pantalla "Registro de ejercicio" de Ring Fit Adventure.
// Sin dependencias de Android: recibe las líneas del OCR y devuelve los días detectados.
// Basado en la lógica de WorkoutLogManager.swift de RingFitConnect (MIT).
package com.solof.fitlog.parser

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

enum class DateOrder { DMY, MDY }

data class OcrLine(
    val text: String,
    /** Coordenadas en píxeles de la imagen; null en tests con texto plano. */
    val top: Int? = null,
    val bottom: Int? = null,
    val left: Int? = null,
    val right: Int? = null,
)

data class ParsedWorkout(
    val date: LocalDate,
    val durationSec: Int? = null,
    val kcal: Double? = null,
    val distanceM: Int? = null,
    /** true si la pantalla no tenía fecha (p. ej. "Resultados de hoy") y se usó la de hoy. */
    val dateIsToday: Boolean = false,
) {
    val hasValues: Boolean get() = durationSec != null || kcal != null || distanceM != null
}

private const val METERS_PER_MILE = 1609.344
private const val NUM = """(\d+(?:[.,]\d+)?)"""

private val MONTHS = mapOf(
    "ene" to 1, "jan" to 1, "feb" to 2, "mar" to 3, "abr" to 4, "apr" to 4, "may" to 5, "jun" to 6,
    "jul" to 7, "ago" to 8, "aug" to 8, "sep" to 9, "set" to 9, "oct" to 10, "nov" to 11, "dic" to 12,
    "dec" to 12,
)
private val MONTH_NAMES = MONTHS.keys.joinToString("|")

// Fecha: 29/11, 11/29 o 29-11, seguida o no del día de la semana. No debe ser parte de un número ni de una hora.
private val DATE_RE = Regex("""(?<![\d:.,])(\d{1,2})\s?[/\-]\s?(\d{1,2})(?![\d:]|[.,]\d)""")
// Fecha con nombre de mes: "29 nov." o "nov. 29".
private val DATE_DAY_MONTH_RE =
    Regex("""(?<![\d:.,])(\d{1,2})\s?(?:de\s)?($MONTH_NAMES)[a-z]*\.?(?![a-z])""", RegexOption.IGNORE_CASE)
private val DATE_MONTH_DAY_RE =
    Regex("""(?<![a-z])($MONTH_NAMES)[a-z]*\.?\s?(\d{1,2})(?![\d:.,])""", RegexOption.IGNORE_CASE)

// Duración: 0:20:49 (h:mm:ss). También 20:49 (mm:ss) y "20 min 49 s".
private val DURATION_HMS_RE = Regex("""(?<![\d.,])(\d{1,2}):([0-5]\d):([0-5]\d)(?!\d)""")
private val DURATION_MS_RE = Regex("""(?<![\d:.,])([0-5]?\d):([0-5]\d)(?![\d:])""")
private val DURATION_WORDS_RE = Regex(
    """(?:(\d{1,2})\s?h\s?)?(\d{1,3})\s?min(?:\.|utos?)?(?:\s?(\d{1,2})\s?(?:s|seg|segundos?)\b)?""",
    RegexOption.IGNORE_CASE,
)

private val KCAL_RE = Regex("""$NUM\s?(kcal|cal)(?![a-z])""", RegexOption.IGNORE_CASE)
private val DIST_RE = Regex("""$NUM\s?(km|mi\.?|millas?)(?![a-z])""", RegexOption.IGNORE_CASE)

private val UNIT_CAL_RE = Regex("""\b(k?)ca[lI1|]\b""", RegexOption.IGNORE_CASE)
private val UNIT_KM_RE = Regex("""(\d|\s)krn\b""", RegexOption.IGNORE_CASE)
private val ZERO_RE = Regex("""(?<=\d[:.,/]?)[OoQD]|(?<![A-Za-zÀ-ÿ])[OoQD](?=[:.,/]?\d)""")
private val ONE_RE = Regex("""(?<=\d[:.,/]?)[Il|](?![A-Za-zÀ-ÿ])|(?<![A-Za-zÀ-ÿ])[Il|](?=[:.,/]?\d)""")
private val FIVE_RE = Regex("""(?<=\d)[Ss](?=\d)""")
private val SPACED_COLON_RE = Regex("""(\d)\s*:\s*(\d)""")
private val SPACED_DECIMAL_RE = Regex("""(\d)([.,])\s+(\d{2})(?!\d)""")
private val SPACES_RE = Regex("""[ \t]+""")

/**
 * Corrige errores típicos del OCR antes de buscar valores:
 * O/o por 0, I/l/| por 1 junto a dígitos, espacios dentro de números y unidades mal leídas.
 */
fun normalizeOcrText(input: String): String {
    var s = input
    // Unidades mal leídas: "kcaI", "Ca1", "krn".
    s = UNIT_CAL_RE.replace(s) { "${it.groupValues[1]}cal" }
    s = UNIT_KM_RE.replace(s, "$1km")
    // Letras parecidas a dígitos cuando están pegadas a un número o separador numérico.
    repeat(2) {
        s = ZERO_RE.replace(s, "0")
        s = ONE_RE.replace(s, "1")
        s = FIVE_RE.replace(s, "5")
    }
    // Espacios sueltos dentro de horas y decimales: "0 :20: 49", "74, 25".
    repeat(2) { s = SPACED_COLON_RE.replace(s, "$1:$2") }
    s = SPACED_DECIMAL_RE.replace(s, "$1$2$3")
    s = SPACES_RE.replace(s, " ")
    return s
}

private fun toNumber(raw: String): Double = raw.replace(',', '.').toDouble()

/** Decide qué número es el día y cuál el mes. Devuelve null si no es una fecha válida. */
fun resolveDayMonth(a: Int, b: Int, order: DateOrder): Pair<Int, Int>? {
    val (day, month) = when {
        a > 12 && b <= 12 -> a to b
        b > 12 && a <= 12 -> b to a
        order == DateOrder.DMY -> a to b
        else -> b to a
    }
    if (month !in 1..12 || day !in 1..31) return null
    return day to month
}

/** El año no sale en pantalla: el actual, o el anterior si la fecha quedaría en el futuro. */
fun resolveDate(month: Int, day: Int, today: LocalDate): LocalDate? {
    var year = today.year
    if (month * 100 + day > today.monthValue * 100 + today.dayOfMonth) year -= 1
    if (day > YearMonth.of(year, month).lengthOfMonth()) return null
    return LocalDate.of(year, month, day)
}

private sealed interface Token {
    val index: Int

    data class Date(override val index: Int, val month: Int, val day: Int) : Token
    data class Duration(override val index: Int, val seconds: Int) : Token
    data class Kcal(override val index: Int, val value: Double) : Token
    data class Distance(override val index: Int, val meters: Double) : Token
}

/** Busca fechas, duraciones, calorías y distancias en una fila de texto, en orden. */
private fun tokenize(text: String, order: DateOrder): List<Token> {
    val tokens = mutableListOf<Token>()
    val taken = mutableListOf<IntRange>()
    fun add(t: Token, m: MatchResult) {
        if (taken.any { m.range.first <= it.last && m.range.last >= it.first }) return
        taken += m.range
        tokens += t
    }

    DURATION_HMS_RE.findAll(text).forEach { m ->
        val (h, mi, s) = m.destructured
        add(Token.Duration(m.range.first, h.toInt() * 3600 + mi.toInt() * 60 + s.toInt()), m)
    }
    KCAL_RE.findAll(text).forEach { m -> add(Token.Kcal(m.range.first, toNumber(m.groupValues[1])), m) }
    DIST_RE.findAll(text).forEach { m ->
        val value = toNumber(m.groupValues[1])
        val meters = if (m.groupValues[2].lowercase().startsWith("km")) value * 1000 else value * METERS_PER_MILE
        add(Token.Distance(m.range.first, meters), m)
    }
    DURATION_WORDS_RE.findAll(text).forEach { m ->
        val h = m.groupValues[1].toIntOrNull() ?: 0
        val s = m.groupValues[3].toIntOrNull() ?: 0
        add(Token.Duration(m.range.first, h * 3600 + m.groupValues[2].toInt() * 60 + s), m)
    }
    DATE_RE.findAll(text).forEach { m ->
        resolveDayMonth(m.groupValues[1].toInt(), m.groupValues[2].toInt(), order)?.let { (day, month) ->
            add(Token.Date(m.range.first, month, day), m)
        }
    }
    DATE_DAY_MONTH_RE.findAll(text).forEach { m ->
        val month = MONTHS.getValue(m.groupValues[2].take(3).lowercase())
        val day = m.groupValues[1].toInt()
        if (day in 1..31) add(Token.Date(m.range.first, month, day), m)
    }
    DATE_MONTH_DAY_RE.findAll(text).forEach { m ->
        val month = MONTHS.getValue(m.groupValues[1].take(3).lowercase())
        val day = m.groupValues[2].toInt()
        if (day in 1..31) add(Token.Date(m.range.first, month, day), m)
    }
    // mm:ss solo si no hay otra cosa en esa posición (pantalla "Resultados de hoy").
    DURATION_MS_RE.findAll(text).forEach { m ->
        add(Token.Duration(m.range.first, m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()), m)
    }
    return tokens.sortedBy { it.index }
}

/**
 * Agrupa las líneas del OCR en filas por posición vertical y las ordena de
 * arriba abajo y de izquierda a derecha. Sin coordenadas, respeta el orden recibido.
 */
fun groupIntoRows(lines: List<OcrLine>): List<String> {
    val visible = lines.filter { it.text.isNotBlank() }
    if (!visible.all { it.top != null && it.bottom != null }) return visible.map { it.text }

    data class Item(val text: String, val center: Double, val height: Double, val left: Int)
    val items = visible
        .map { Item(it.text, (it.top!! + it.bottom!!) / 2.0, maxOf(1, it.bottom - it.top).toDouble(), it.left ?: 0) }
        .sortedBy { it.center }

    val rows = mutableListOf<MutableList<Item>>()
    for (item in items) {
        val row = rows.lastOrNull()
        if (row != null) {
            val rowCenter = row.sumOf { it.center } / row.size
            val rowHeight = row.sumOf { it.height } / row.size
            if (abs(item.center - rowCenter) < minOf(rowHeight, item.height) * 0.5) {
                row += item
                continue
            }
        }
        rows += mutableListOf(item)
    }
    return rows.map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text } }
}

/**
 * Extrae todos los días de la pantalla. Recorre los valores en orden de lectura:
 * cada fecha abre un entrenamiento nuevo y los valores siguientes se le asignan.
 */
fun parseWorkoutLog(
    lines: List<OcrLine>,
    order: DateOrder = DateOrder.DMY,
    today: LocalDate = LocalDate.now(),
): List<ParsedWorkout> {
    val tokens = groupIntoRows(lines).flatMap { tokenize(normalizeOcrText(it), order) }
    val hasDates = tokens.any { it is Token.Date }

    val result = mutableListOf<ParsedWorkout>()
    // Pantalla sin fechas ("Resultados de hoy"): un único entrenamiento con la fecha de hoy.
    var current: ParsedWorkout? = if (hasDates) null else ParsedWorkout(today, dateIsToday = true)

    for (t in tokens) {
        if (t is Token.Date) {
            val date = resolveDate(t.month, t.day, today) ?: continue
            // Un día sin valores también se guarda: la revisión lo deja vacío y editable.
            current?.let { result += it }
            current = ParsedWorkout(date)
            continue
        }
        // Valores antes de la primera fecha (fila cortada arriba): se ignoran.
        val c = current ?: continue
        current = when {
            t is Token.Duration && c.durationSec == null -> c.copy(durationSec = t.seconds)
            t is Token.Kcal && c.kcal == null -> c.copy(kcal = t.value)
            t is Token.Distance && c.distanceM == null -> c.copy(distanceM = t.meters.roundToInt())
            else -> c
        }
    }
    current?.let { if (hasDates || it.hasValues) result += it }

    // Si la misma fecha aparece dos veces (foto repetida o fila partida), se combinan.
    val byDate = LinkedHashMap<LocalDate, ParsedWorkout>()
    for (w in result) {
        val prev = byDate[w.date]
        byDate[w.date] = prev?.copy(
            durationSec = prev.durationSec ?: w.durationSec,
            kcal = prev.kcal ?: w.kcal,
            distanceM = prev.distanceM ?: w.distanceM,
        ) ?: w
    }
    return byDate.values.sortedByDescending { it.date }
}

fun parseWorkoutLogText(lines: List<String>, order: DateOrder = DateOrder.DMY, today: LocalDate = LocalDate.now()) =
    parseWorkoutLog(lines.map { OcrLine(it) }, order, today)

/** Identificador estable para evitar duplicados en Health Connect y en el historial. */
fun workoutClientId(date: LocalDate, durationSec: Int): String = "ringfit-$date-$durationSec"

fun formatDuration(totalSec: Int): String =
    "%d:%02d:%02d".format(totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60)

/** Acepta "0:20:49", "20:49" o "20". Devuelve segundos o null. */
fun parseDurationInput(input: String): Int? {
    val parts = input.trim().split(":").map { it.trim() }
    if (parts.any { !it.matches(Regex("""\d+""")) }) return null
    val n = parts.map { it.toInt() }
    return when (n.size) {
        3 -> n[0] * 3600 + n[1] * 60 + n[2]
        2 -> n[0] * 60 + n[1]
        1 -> n[0] * 60
        else -> null
    }
}

sealed interface DecimalInput {
    data object Empty : DecimalInput
    data object Invalid : DecimalInput
    data class Value(val value: Double) : DecimalInput
}

/** Acepta "74,25" o "74.25". */
fun parseDecimalInput(input: String): DecimalInput {
    val t = input.trim()
    if (t.isEmpty()) return DecimalInput.Empty
    if (!t.matches(Regex("""\d+([.,]\d+)?"""))) return DecimalInput.Invalid
    return DecimalInput.Value(toNumber(t))
}
