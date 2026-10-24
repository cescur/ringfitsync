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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.cescur.fitlog.R
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

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val res = LocalResources.current
    val scope = rememberCoroutineScope()
    var workouts by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var tab by remember { mutableIntStateOf(0) }
    var toDelete by remember { mutableStateOf<Workout?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) { workouts = withContext(Dispatchers.IO) { app.db.list() } }
    val monthNames = stringArrayResource(R.array.months_short).toList()
    val weeks = remember(workouts, monthNames) { weeklyStats(workouts, monthNames) }
    val months = remember(workouts, monthNames) { monthlyStats(workouts, monthNames) }

    fun exportCsv() {
        try {
            val dir = File(context.cacheDir, "export").apply { mkdirs() }
            val file = File(dir, "fitlog-${LocalDate.now()}.csv").apply { writeText(toCsv(workouts)) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/csv")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, res.getString(R.string.history_export)))
        } catch (e: Exception) {
            Toast.makeText(context, res.getString(R.string.history_export_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(topBar = { AppBar(stringResource(R.string.history), onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val labels = listOf(R.string.tab_workouts, R.string.tab_weeks, R.string.tab_months).map { stringResource(it) }
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
            if (workouts.isEmpty()) item { MutedText(stringResource(R.string.history_empty)) }
            when (tab) {
                0 -> items(workouts, key = { it.id }) { w ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CardTitle("${w.date} · ${w.startTime}")
                            Box(Modifier.weight(1f))
                            TextButton(onClick = { toDelete = w }) { Text(stringResource(R.string.delete), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
                        }
                        Text("${formatDuration(w.durationSec)} · ${formatKcal(w.kcal)} · ${formatKm(w.distanceM)}")
                        if (!w.inHealthConnect) MutedText(stringResource(R.string.history_local_only))
                    }
                }
                1 -> items(weeks, key = { it.key }) { PeriodCard(it) }
                else -> items(months, key = { it.key }) { PeriodCard(it) }
            }
            if (workouts.isNotEmpty()) {
                item { OutlinedButton(onClick = ::exportCsv, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.history_export)) } }
            }
        }
    }

    toDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_body, w.date.toString())) },
            confirmButton = {
                TextButton(onClick = {
                    toDelete = null
                    scope.launch {
                        try {
                            if (w.inHealthConnect) HealthConnect.delete(context, w)
                            withContext(Dispatchers.IO) { app.db.delete(w.id) }
                            reload++
                        } catch (e: Exception) {
                            Toast.makeText(context, res.getString(R.string.history_delete_failed, e.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun PeriodCard(p: PeriodStats) {
    SectionCard {
        CardTitle(p.label)
        MutedText(pluralStringResource(R.plurals.history_days, p.sessions, p.sessions))
        Text(stringResource(R.string.history_total, formatDuration(p.totalSec), formatKcal(p.totalKcal), formatKm(p.totalM)))
        Text(stringResource(R.string.history_average, formatDuration(p.avgSec), formatKcal(p.avgKcal), formatKm(p.avgM)))
    }
}
