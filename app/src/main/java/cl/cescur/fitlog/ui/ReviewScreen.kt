package cl.cescur.fitlog.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import cl.cescur.fitlog.R
import cl.cescur.fitlog.data.Workout
import cl.cescur.fitlog.health.HealthConnect
import cl.cescur.fitlog.health.HealthStatus
import cl.cescur.fitlog.parser.DecimalInput
import cl.cescur.fitlog.parser.ParsedWorkout
import cl.cescur.fitlog.parser.formatDuration
import cl.cescur.fitlog.parser.groupIntoRows
import cl.cescur.fitlog.parser.parseDecimalInput
import cl.cescur.fitlog.parser.parseDurationInput
import cl.cescur.fitlog.parser.workoutClientId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

private data class Draft(
    val key: String,
    val checked: Boolean,
    val date: LocalDate?,
    val startTime: LocalTime,
    val duration: String,
    val kcal: String,
    val km: String,
    val dateIsToday: Boolean = false,
)

private fun ParsedWorkout.toDraft(startTime: LocalTime, i: Int) = Draft(
    key = "$i-$date",
    // Un día con 0:00:00 (sin actividad) no se marca.
    checked = hasValues && durationSec != 0,
    date = date,
    startTime = startTime,
    duration = durationSec?.let(::formatDuration) ?: "",
    kcal = kcal?.let { formatDecimalInput(it) } ?: "",
    km = distanceM?.let { formatDecimalInput(it / 1000.0, 2) } ?: "",
    dateIsToday = dateIsToday,
)

private sealed interface Checked {
    data class Valid(val workout: Workout) : Checked
    data class Invalid(val draft: Draft, @StringRes val error: Int) : Checked
}

private fun validate(d: Draft): Checked {
    val date = d.date ?: return Checked.Invalid(d, R.string.error_no_date)
    val durationSec = parseDurationInput(d.duration)?.takeIf { it > 0 }
        ?: return Checked.Invalid(d, R.string.error_no_duration)
    val kcal = when (val k = parseDecimalInput(d.kcal)) {
        DecimalInput.Invalid -> return Checked.Invalid(d, R.string.error_kcal)
        DecimalInput.Empty -> null
        is DecimalInput.Value -> k.value
    }
    val distanceM = when (val k = parseDecimalInput(d.km)) {
        DecimalInput.Invalid -> return Checked.Invalid(d, R.string.error_distance)
        DecimalInput.Empty -> null
        is DecimalInput.Value -> Math.round(k.value * 1000).toInt()
    }
    return Checked.Valid(Workout(date, d.startTime, durationSec, kcal, distanceM))
}

private suspend fun loadPreview(context: Context, uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1200) sample *= 2
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }?.asImageBitmap()
    }.getOrNull()
}

@Composable
fun ReviewScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val app = context.app
    val res = LocalResources.current
    val scan = remember { app.pendingScan }
    val scope = rememberCoroutineScope()
    val drafts = remember { mutableStateListOf<Draft>() }
    var existing by remember { mutableStateOf<List<Workout>>(emptyList()) }
    var showText by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, String>?>(null) }
    var askLocalOnly by remember { mutableStateOf<List<Workout>?>(null) }
    var savedOk by remember { mutableStateOf(false) }

    val preview by produceState<ImageBitmap?>(null, scan) { value = scan?.let { loadPreview(context, it.imageUri) } }
    val ocrText = remember(scan) { scan?.let { groupIntoRows(it.lines).joinToString("\n") } ?: "" }

    LaunchedEffect(scan) {
        val parsed = scan?.workouts ?: return@LaunchedEffect
        val found = withContext(Dispatchers.IO) { app.db.onDates(parsed.map { it.date }) }
        existing = found
        val defaultStart = app.settings.defaultStartTime
        drafts.clear()
        parsed.forEachIndexed { i, w ->
            var d = w.toDraft(defaultStart, i)
            // Si ya está guardado tal cual, no se vuelve a marcar.
            if (w.durationSec != null && found.any { it.id == workoutClientId(w.date, w.durationSec) }) d = d.copy(checked = false)
            // Conserva la hora de inicio que tuviera el registro anterior de ese día.
            found.firstOrNull { it.date == w.date }?.let { d = d.copy(startTime = it.startTime) }
            drafts += d
        }
    }

    fun update(key: String, f: (Draft) -> Draft) {
        val i = drafts.indexOfFirst { it.key == key }
        if (i >= 0) drafts[i] = f(drafts[i])
    }

    fun persist(workouts: List<Workout>, useHealth: Boolean) {
        saving = true
        scope.launch {
            val failures = mutableListOf<String>()
            for (w in workouts) {
                try {
                    // Si ese día ya tenía otro registro (otra duración), se reemplaza.
                    for (old in existing.filter { it.date == w.date && it.id != w.id }) {
                        if (old.inHealthConnect && useHealth) HealthConnect.delete(context, old)
                        withContext(Dispatchers.IO) { app.db.delete(old.id) }
                    }
                    if (useHealth) HealthConnect.write(context, w)
                    withContext(Dispatchers.IO) { app.db.save(w.copy(inHealthConnect = useHealth)) }
                } catch (e: Exception) {
                    failures += "${w.date}: ${e.message}"
                }
            }
            saving = false
            val ok = workouts.size - failures.size
            val summary = res.getQuantityString(if (useHealth) R.plurals.review_saved_health else R.plurals.review_saved_local, ok, ok)
            savedOk = true
            message = res.getString(if (failures.isEmpty()) R.string.review_saved else R.string.review_saved_errors) to
                (listOf(summary) + failures).joinToString("\n")
        }
    }

    fun save() {
        val selected = drafts.filter { it.checked }.map(::validate)
        if (selected.isEmpty()) {
            message = res.getString(R.string.review_nothing_title) to res.getString(R.string.review_nothing_body)
            return
        }
        val errors = selected.filterIsInstance<Checked.Invalid>()
        if (errors.isNotEmpty()) {
            message = res.getString(R.string.review_check_title) to errors.joinToString("\n") {
                "${it.draft.date ?: res.getString(R.string.review_new_day)}: ${res.getString(it.error)}"
            }
            return
        }
        val workouts = selected.filterIsInstance<Checked.Valid>().map { it.workout }
        scope.launch {
            val ok = runCatching { HealthConnect.status(context) }.getOrNull() == HealthStatus.OK
            if (ok) persist(workouts, true) else askLocalOnly = workouts
        }
    }

    Scaffold(topBar = { AppBar(stringResource(R.string.review_title), onBack = onBack) }) { padding ->
        if (scan == null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { MutedText(stringResource(R.string.review_no_scan)) }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(
                    Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color(0xFFE3E7EC)),
                ) {
                    preview?.let { Image(it, contentDescription = stringResource(R.string.review_image), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                }
            }
            if (drafts.isEmpty()) {
                item {
                    SectionCard {
                        MutedText(stringResource(R.string.review_nothing_found))
                    }
                }
            }
            items(drafts, key = { it.key }) { d ->
                val durationSec = parseDurationInput(d.duration)
                val sameSaved = d.date != null && durationSec != null && existing.any { it.id == workoutClientId(d.date, durationSec) }
                val other = if (sameSaved) null else existing.firstOrNull { it.date == d.date }
                SectionCard(Modifier.alpha(if (d.checked) 1f else 0.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CardTitle(d.date?.toString() ?: stringResource(R.string.review_new_day))
                        Box(Modifier.weight(1f))
                        Switch(checked = d.checked, onCheckedChange = { v -> update(d.key) { it.copy(checked = v) } })
                    }
                    if (durationSec == 0) MutedText(stringResource(R.string.review_no_activity))
                    if (d.dateIsToday) MutedText(stringResource(R.string.review_date_today))
                    if (sameSaved) MutedText(stringResource(R.string.review_already_saved))
                    other?.let { MutedText(stringResource(R.string.review_replaces, formatDuration(it.durationSec)), color = Warn) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PickerBox(stringResource(R.string.field_date), d.date?.toString() ?: stringResource(R.string.field_pick), Modifier.weight(1f), Icons.Outlined.Event) {
                            showDatePicker(context, d.date ?: LocalDate.now()) { v -> update(d.key) { it.copy(date = v) } }
                        }
                        PickerBox(stringResource(R.string.field_start_time), d.startTime.toString(), Modifier.weight(1f), Icons.Outlined.Schedule) {
                            showTimePicker(context, d.startTime) { v -> update(d.key) { it.copy(startTime = v) } }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(stringResource(R.string.field_duration), d.duration, "0:20:49", KeyboardType.Text, Modifier.weight(1f)) { v -> update(d.key) { it.copy(duration = v) } }
                        NumberField(stringResource(R.string.field_kcal), d.kcal, "74,25", KeyboardType.Decimal, Modifier.weight(1f)) { v -> update(d.key) { it.copy(kcal = v) } }
                    }
                    NumberField(stringResource(R.string.field_distance), d.km, "1,23", KeyboardType.Decimal, Modifier.fillMaxWidth(0.5f)) { v -> update(d.key) { it.copy(km = v) } }
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        drafts += Draft("manual-${System.currentTimeMillis()}", true, null, app.settings.defaultStartTime, "", "", "")
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text(stringResource(R.string.review_add_day)) }
            }
            item {
                Button(onClick = ::save, enabled = !saving, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text(stringResource(if (saving) R.string.review_saving else R.string.review_save), fontSize = 17.sp)
                }
            }
            item {
                Text(
                    stringResource(if (showText) R.string.review_hide_text else R.string.review_show_text),
                    color = Accent,
                    modifier = Modifier.fillMaxWidth().clickable { showText = !showText }.padding(8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            if (showText) {
                item {
                    SectionCard {
                        Text(ocrText.ifEmpty { stringResource(R.string.review_empty_text) }, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        OutlinedButton(
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ocrText)
                                context.startActivity(Intent.createChooser(send, res.getString(R.string.share_text)))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.share_text)) }
                    }
                }
            }
        }
    }

    askLocalOnly?.let { workouts ->
        AlertDialog(
            onDismissRequest = { askLocalOnly = null },
            title = { Text(stringResource(R.string.review_local_only_title)) },
            text = { Text(stringResource(R.string.review_local_only_body)) },
            confirmButton = { TextButton(onClick = { askLocalOnly = null; persist(workouts, false) }) { Text(stringResource(R.string.review_save_here)) } },
            dismissButton = { TextButton(onClick = { askLocalOnly = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    message?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = {
                    message = null
                    if (savedOk) onSaved()
                }) { Text(stringResource(if (savedOk) R.string.review_view_history else R.string.ok)) }
            },
        )
    }
}

@Composable
private fun PickerBox(label: String, value: String, modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = Muted)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.ui.graphics.Color(0xFFF1F3F6))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Icon(icon, contentDescription = null, tint = Muted)
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, placeholder: String, type: KeyboardType, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = modifier,
    )
}
