package cl.cescur.fitlog.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cl.cescur.fitlog.data.PeriodStats
import cl.cescur.fitlog.data.Workout
import cl.cescur.fitlog.data.monthlyStats
import cl.cescur.fitlog.data.toCsv
import cl.cescur.fitlog.data.weeklyStats
import cl.cescur.fitlog.health.HealthConnect
import cl.cescur.fitlog.parser.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.Locale

private fun km(m: Int?) = m?.let { String.format(Locale.ROOT, "%.2f km", it / 1000.0).replace('.', ',') } ?: "–"

private fun kcal(v: Double?) = v?.let { String.format(Locale.ROOT, "%.1f kcal", it).replace('.', ',') } ?: "–"

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val scope = rememberCoroutineScope()
    var workouts by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var tab by remember { mutableIntStateOf(0) }
    var toDelete by remember { mutableStateOf<Workout?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) { workouts = withContext(Dispatchers.IO) { app.db.list() } }
    val weeks = remember(workouts) { weeklyStats(workouts) }
    val months = remember(workouts) { monthlyStats(workouts) }

    fun exportCsv() {
        try {
            val dir = File(context.cacheDir, "export").apply { mkdirs() }
            val file = File(dir, "fitlog-${LocalDate.now()}.csv").apply { writeText(toCsv(workouts)) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/csv")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, "Exportar CSV"))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(topBar = { AppBar("Historial", onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val labels = listOf("Entrenamientos", "Semanas", "Meses")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    labels.forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = tab == i,
                            onClick = { tab = i },
                            shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
            }
            if (workouts.isEmpty()) item { MutedText("Todavía no hay entrenamientos guardados.") }
            when (tab) {
                0 -> items(workouts, key = { it.id }) { w ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CardTitle("${w.date} · ${w.startTime}")
                            Box(Modifier.weight(1f))
                            TextButton(onClick = { toDelete = w }) { Text("Borrar", color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
                        }
                        Text("${formatDuration(w.durationSec)} · ${kcal(w.kcal)} · ${km(w.distanceM)}")
                        if (!w.inHealthConnect) MutedText("Solo en el historial (no está en Health Connect).")
                    }
                }
                1 -> items(weeks, key = { it.key }) { PeriodCard(it) }
                else -> items(months, key = { it.key }) { PeriodCard(it) }
            }
            if (workouts.isNotEmpty()) {
                item { OutlinedButton(onClick = ::exportCsv, modifier = Modifier.fillMaxWidth()) { Text("Exportar todo a CSV") } }
            }
        }
    }

    toDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Borrar entrenamiento") },
            text = { Text("¿Borrar el del ${w.date}? También se borra de Health Connect.") },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    scope.launch {
                        try {
                            if (w.inHealthConnect) HealthConnect.delete(context, w)
                            withContext(Dispatchers.IO) { app.db.delete(w.id) }
                            reload++
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo borrar de Health Connect: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PeriodCard(p: PeriodStats) {
    SectionCard {
        CardTitle(p.label)
        MutedText("${p.sessions} día${if (p.sessions == 1) "" else "s"} de ejercicio")
        Text("Total: ${formatDuration(p.totalSec)} · ${kcal(p.totalKcal)} · ${km(p.totalM)}")
        Text("Media: ${formatDuration(p.avgSec)} · ${kcal(p.avgKcal)} · ${km(p.avgM)}")
    }
}
