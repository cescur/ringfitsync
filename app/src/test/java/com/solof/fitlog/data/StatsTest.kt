package com.solof.fitlog.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

private fun w(date: String, start: String, sec: Int, kcal: Double?, m: Int?) =
    Workout(LocalDate.parse(date), LocalTime.parse(start), sec, kcal, m)

private val ws = listOf(
    w("2026-10-19", "19:00", 1200, 60.0, 1000), // lunes
    w("2026-10-25", "19:00", 1800, 90.0, null), // domingo
    w("2026-10-26", "20:30", 600, null, 500), // lunes siguiente
    w("2026-09-30", "19:00", 900, 45.0, 700),
)

class StatsTest {
    @Test
    fun `semanas empiezan en lunes`() {
        assertEquals(LocalDate.parse("2026-10-19"), weekStart(LocalDate.parse("2026-10-25")))
        assertEquals(LocalDate.parse("2026-10-19"), weekStart(LocalDate.parse("2026-10-19")))
        assertEquals(LocalDate.parse("2026-10-26"), weekStart(LocalDate.parse("2026-10-26")))
    }

    @Test
    fun `totales y medias por semana`() {
        val weeks = weeklyStats(ws)
        assertEquals(listOf("2026-10-26", "2026-10-19", "2026-09-28"), weeks.map { it.key })
        val w = weeks[1]
        assertEquals(2, w.sessions)
        assertEquals(3000, w.totalSec)
        assertEquals(150.0, w.totalKcal, 0.001)
        assertEquals(1000, w.totalM)
        assertEquals(1500, w.avgSec)
    }

    @Test
    fun `totales por mes`() {
        assertEquals(
            listOf(Triple("2026-10", "oct 2026", 3), Triple("2026-09", "sep 2026", 1)),
            monthlyStats(ws).map { Triple(it.key, it.label, it.sessions) },
        )
    }

    @Test
    fun `CSV ordenado por fecha con campos vacíos`() {
        assertEquals(
            listOf(
                "fecha,hora_inicio,duracion,duracion_seg,kcal,distancia_km",
                "2026-09-30,19:00,0:15:00,900,45,0.70",
                "2026-10-19,19:00,0:20:00,1200,60,1.00",
            ),
            toCsv(ws).split("\n").take(3),
        )
    }

    @Test
    fun `id estable y hora de fin`() {
        val first = ws[0]
        assertEquals("ringfit-2026-10-19-1200", first.id)
        assertEquals(LocalTime.parse("19:20"), first.end().toLocalTime())
    }
}
