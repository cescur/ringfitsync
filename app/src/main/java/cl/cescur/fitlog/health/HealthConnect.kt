package cl.cescur.fitlog.health

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import cl.cescur.fitlog.R
import cl.cescur.fitlog.data.Workout

enum class HealthStatus(@StringRes val text: Int) {
    OK(R.string.health_ok),
    MISSING_PERMISSIONS(R.string.health_missing),
    UNAVAILABLE(R.string.health_unavailable),
    NEEDS_UPDATE(R.string.health_needs_update),
}

object HealthConnect {
    private val WRITE = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
    )

    /** Escritura para guardar; lectura para que borrar y reemplazar funcione con los datos propios. */
    val PERMISSIONS: Set<String> = WRITE + setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    fun settingsIntent() = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)

    private fun client(context: Context) = HealthConnectClient.getOrCreate(context)

    suspend fun status(context: Context): HealthStatus =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> HealthStatus.UNAVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthStatus.NEEDS_UPDATE
            else -> {
                val granted = client(context).permissionController.getGrantedPermissions()
                if (granted.containsAll(WRITE)) HealthStatus.OK else HealthStatus.MISSING_PERMISSIONS
            }
        }

    private fun ids(w: Workout) = Triple("${w.id}-session", "${w.id}-kcal", "${w.id}-distance")

    /**
     * Escribe la sesión, las calorías y la distancia. Health Connect sobrescribe los registros
     * con el mismo clientRecordId, así que importar dos veces el mismo día no duplica nada.
     */
    suspend fun write(context: Context, w: Workout) {
        val start = w.start()
        val end = w.end()
        val (sessionId, kcalId, distanceId) = ids(w)
        val version = System.currentTimeMillis()
        val records = buildList<Record> {
            add(
                ExerciseSessionRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    metadata = Metadata.manualEntry(sessionId, version),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                    title = "Ring Fit Adventure",
                ),
            )
            w.kcal?.takeIf { it > 0 }?.let {
                add(
                    ActiveCaloriesBurnedRecord(
                        startTime = start.toInstant(),
                        startZoneOffset = start.offset,
                        endTime = end.toInstant(),
                        endZoneOffset = end.offset,
                        energy = Energy.kilocalories(it),
                        metadata = Metadata.manualEntry(kcalId, version),
                    ),
                )
            }
            w.distanceM?.takeIf { it > 0 }?.let {
                add(
                    DistanceRecord(
                        startTime = start.toInstant(),
                        startZoneOffset = start.offset,
                        endTime = end.toInstant(),
                        endZoneOffset = end.offset,
                        distance = Length.meters(it.toDouble()),
                        metadata = Metadata.manualEntry(distanceId, version),
                    ),
                )
            }
        }
        client(context).insertRecords(records)
    }

    suspend fun delete(context: Context, w: Workout) {
        val (sessionId, kcalId, distanceId) = ids(w)
        val c = client(context)
        c.deleteRecords(ExerciseSessionRecord::class, emptyList(), listOf(sessionId))
        c.deleteRecords(ActiveCaloriesBurnedRecord::class, emptyList(), listOf(kcalId))
        c.deleteRecords(DistanceRecord::class, emptyList(), listOf(distanceId))
    }
}
