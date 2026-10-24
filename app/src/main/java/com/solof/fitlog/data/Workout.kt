package com.solof.fitlog.data

import com.solof.fitlog.parser.workoutClientId
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Entrenamiento ya confirmado, tal como se guarda en el historial y en Health Connect. */
data class Workout(
    val date: LocalDate,
    val startTime: LocalTime,
    val durationSec: Int,
    val kcal: Double?,
    val distanceM: Int?,
    val inHealthConnect: Boolean = false,
) {
    val id: String get() = workoutClientId(date, durationSec)

    fun start(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime = ZonedDateTime.of(date, startTime, zone)

    fun end(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime = start(zone).plusSeconds(durationSec.toLong())
}
