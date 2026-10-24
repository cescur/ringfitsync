package com.solof.fitlog.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.roundToInt

private val today: LocalDate = LocalDate.of(2026, 10, 24)

private fun d(s: String) = LocalDate.parse(s)

// Simula líneas del OCR con coordenadas: cada fila a una altura distinta, varias columnas por fila.
private fun rows(vararg r: List<String>): List<OcrLine> =
    r.flatMapIndexed { y, cols ->
        cols.mapIndexed { x, text -> OcrLine(text, top = y * 40, bottom = y * 40 + 30, left = x * 300, right = x * 300 + 250) }
    }

class EnglishScreenTest {
    @Test
    fun `lee varios días con mes-día, Cal y millas`() {
        val lines = rows(
            listOf("Exercise Log"),
            listOf("10/23 (Fri.)"),
            listOf("Time Exercising", "Calories Burned", "Distance Traveled"),
            listOf("0:20:49", "74.25 Cal", "0.77 mi."),
            listOf("10/22 (Thu.)"),
            listOf("Time Exercising", "Calories Burned", "Distance Traveled"),
            listOf("0:35:02", "127.10kcal", "1.85km"),
        )
        assertEquals(
            listOf(
                ParsedWorkout(d("2026-10-23"), 20 * 60 + 49, 74.25, (0.77 * 1609.344).roundToInt()),
                ParsedWorkout(d("2026-10-22"), 35 * 60 + 2, 127.1, 1850),
            ),
            parseWorkoutLog(lines, DateOrder.MDY, today),
        )
    }

    @Test
    fun `texto sin coordenadas y año anterior para fechas futuras`() {
        val out = parseWorkoutLogText(
            listOf("11/29 (Fri.)", "Time Exercising 0:20:49", "Calories Burned 74.25 Cal", "Distance Traveled 0.77 mi."),
            DateOrder.MDY,
            today,
        )
        assertEquals(1, out.size)
        assertEquals(d("2025-11-29"), out[0].date)
    }
}

class SpanishScreenTest {
    @Test
    fun `lee día-mes con coma decimal y kcal`() {
        val lines = rows(
            listOf("Registro de ejercicio"),
            listOf("23/10 (vie.)"),
            listOf("Tiempo de ejercicio", "Calorías quemadas", "Distancia recorrida"),
            listOf("0:20:49", "74,25 kcal", "1,23 km"),
            listOf("21/10 (mié.)"),
            listOf("Tiempo de ejercicio", "Calorías quemadas", "Distancia recorrida"),
            listOf("1:02:10", "210,50 kcal", "3,40 km"),
            listOf("20/10 (mar.)"),
            listOf("Tiempo de ejercicio", "Calorías quemadas", "Distancia recorrida"),
            listOf("0:15:00", "50,00 kcal", "0,80 km"),
        )
        assertEquals(
            listOf(
                ParsedWorkout(d("2026-10-23"), 1249, 74.25, 1230),
                ParsedWorkout(d("2026-10-21"), 3730, 210.5, 3400),
                ParsedWorkout(d("2026-10-20"), 900, 50.0, 800),
            ),
            parseWorkoutLog(lines, DateOrder.DMY, today),
        )
    }

    @Test
    fun `deduce el orden si un número es mayor que 12`() {
        val out = parseWorkoutLogText(listOf("10/23", "0:10:00", "30 kcal", "0,5 km"), DateOrder.DMY, today)
        assertEquals(d("2026-10-23"), out[0].date)
    }

    @Test
    fun `tolera O en vez de 0, espacios sueltos y unidades mal leídas`() {
        val out = parseWorkoutLog(rows(listOf("2O/1O (mar.)"), listOf("O: 2O :49", "74, 25 kcaI", "1,2O krn")), today = today)
        assertEquals(listOf(ParsedWorkout(d("2026-10-20"), 1249, 74.25, 1200)), out)
    }

    @Test
    fun `tolera l o I en lugar de 1`() {
        val out = parseWorkoutLogText(listOf("l9/l0", "Tiempo l:05:00", "3l2,4 kcal", "I,5 km"), today = today)
        assertEquals(listOf(ParsedWorkout(d("2026-10-19"), 3900, 312.4, 1500)), out)
    }

    @Test
    fun `deja vacíos los campos que faltan en lugar de descartar el día`() {
        val lines = rows(
            listOf("23/10 (vie.)"),
            listOf("0:20:49", "0,77 km"),
            listOf("22/10 (jue.)"),
            listOf("74,25 kcal"),
            listOf("21/10 (mié.)"),
        )
        assertEquals(
            listOf(
                ParsedWorkout(d("2026-10-23"), durationSec = 1249, distanceM = 770),
                ParsedWorkout(d("2026-10-22"), kcal = 74.25),
                ParsedWorkout(d("2026-10-21")),
            ),
            parseWorkoutLog(lines, today = today),
        )
    }

    @Test
    fun `ignora valores de una fila cortada antes de la primera fecha`() {
        val lines = rows(
            listOf("0:10:00", "40,00 kcal", "0,30 km"),
            listOf("23/10 (vie.)"),
            listOf("0:20:49", "74,25 kcal", "0,77 km"),
        )
        assertEquals(listOf(ParsedWorkout(d("2026-10-23"), 1249, 74.25, 770)), parseWorkoutLog(lines, today = today))
    }

    @Test
    fun `acepta fechas con nombre de mes`() {
        val out = parseWorkoutLogText(listOf("23 oct. (vie.)", "0:20:49 74,25 kcal 0,77 km"), today = today)
        assertEquals(d("2026-10-23"), out[0].date)
        assertEquals(1249, out[0].durationSec)
    }

    @Test
    fun `ordena por posición vertical aunque el OCR devuelva bloques desordenados`() {
        val lines = rows(
            listOf("23/10"),
            listOf("0:20:49", "74,25 kcal", "0,77 km"),
            listOf("22/10"),
            listOf("0:10:00", "30,00 kcal", "0,40 km"),
        )
        val shuffled = listOf(lines[5], lines[0], lines[7], lines[2], lines[4], lines[1], lines[6], lines[3])
        assertEquals(
            listOf(d("2026-10-23") to 1249, d("2026-10-22") to 600),
            parseWorkoutLog(shuffled, today = today).map { it.date to it.durationSec },
        )
    }

    @Test
    fun `agrupa en la misma fila columnas con alturas ligeramente distintas`() {
        val lines = listOf(
            OcrLine("23/10 (vie.)", top = 0, bottom = 30, left = 0),
            OcrLine("74,25", top = 52, bottom = 80, left = 300),
            OcrLine("kcal", top = 55, bottom = 82, left = 380),
            OcrLine("0:20:49", top = 50, bottom = 80, left = 0),
            OcrLine("0,77 km", top = 48, bottom = 78, left = 600),
        )
        assertEquals(listOf(ParsedWorkout(d("2026-10-23"), 1249, 74.25, 770)), parseWorkoutLog(lines, today = today))
    }
}

class TodayResultsScreenTest {
    @Test
    fun `usa la fecha de hoy si no hay fechas`() {
        val out = parseWorkoutLogText(
            listOf("Resultados de hoy", "Tiempo de ejercicio activo", "20:49", "Calorías quemadas", "74,25 kcal", "Distancia recorrida", "0,77 km"),
            today = today,
        )
        assertEquals(listOf(ParsedWorkout(today, 1249, 74.25, 770, dateIsToday = true)), out)
    }

    @Test
    fun `acepta 20 min 49 s`() {
        assertEquals(1249, parseWorkoutLogText(listOf("Tiempo 20 min 49 s", "74,25 kcal"), today = today)[0].durationSec)
    }

    @Test
    fun `no devuelve nada si no hay valores`() {
        assertTrue(parseWorkoutLogText(listOf("Hola", "Menú principal"), today = today).isEmpty())
    }
}

class RealScreenshotTest {
    // Texto que devolvió el OCR del móvil para test/resources/registro-es-2026-10-24.png
    // (juego en español, captura de la Switch). Los iconos de botones se sustituyeron por texto.
    private val registroEs20261024 = listOf(
        "Elige una fecha para consultar",
        "el registro de ejercicios del día.",
        "Såbado, 24/10",
        "Tiempo de actividad Calorias quemadas Distancia recorrída",
        "0:00:00 0.00 kcal 0.00 km",
        "Domingo, 20/09",
        "Tiempo de actividad Calorías quemadas Distancia recorrida",
        "0:02:32 21.00 kcal 0.50 km",
        "Sábado, 22/07",
        "Tiempo de actividad Calorías quemadas Distancia recorrida",
        "O:03:19 13.00 kcal 0.50 km",
        "Usa [análogo] y [cruceta] en el JoyCon derecho para desplazarte por las distintas opciones.",
    )

    @Test
    fun `registro en español, 24-10-2026`() {
        assertEquals(
            listOf(
                ParsedWorkout(d("2026-10-24"), 0, 0.0, 0),
                ParsedWorkout(d("2026-09-20"), 152, 21.0, 500),
                ParsedWorkout(d("2026-07-22"), 199, 13.0, 500),
            ),
            parseWorkoutLogText(registroEs20261024, today = today),
        )
    }
}

class ParserUtilsTest {
    @Test
    fun resolveDayMonth() {
        assertEquals(5 to 3, resolveDayMonth(5, 3, DateOrder.DMY))
        assertEquals(3 to 5, resolveDayMonth(5, 3, DateOrder.MDY))
        assertEquals(13 to 3, resolveDayMonth(13, 3, DateOrder.MDY))
        assertNull(resolveDayMonth(13, 13, DateOrder.DMY))
    }

    @Test
    fun resolveDate() {
        assertEquals(d("2026-10-24"), resolveDate(10, 24, today))
        assertEquals(d("2025-10-25"), resolveDate(10, 25, today))
        assertNull(resolveDate(2, 30, today))
    }

    @Test
    fun `normalizeOcrText no toca palabras`() {
        assertEquals("Calorías quemadas 74,25 cal", normalizeOcrText("Calorías quemadas 74,25 Cal"))
        assertEquals("Distance Traveled (Dom.)", normalizeOcrText("Distance Traveled (Dom.)"))
    }

    @Test
    fun `id estable`() {
        assertEquals("ringfit-2026-10-23-1249", workoutClientId(d("2026-10-23"), 1249))
    }

    @Test
    fun `entradas de la revisión`() {
        assertEquals("0:20:49", formatDuration(1249))
        assertEquals(1249, parseDurationInput("0:20:49"))
        assertEquals(1249, parseDurationInput("20:49"))
        assertEquals(1200, parseDurationInput("20"))
        assertNull(parseDurationInput("abc"))
        assertEquals(DecimalInput.Value(74.25), parseDecimalInput("74,25"))
        assertEquals(DecimalInput.Empty, parseDecimalInput(""))
        assertEquals(DecimalInput.Invalid, parseDecimalInput("7x"))
    }
}
