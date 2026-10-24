package com.solof.fitlog.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.solof.fitlog.FitLogApp
import com.solof.fitlog.health.HealthConnect
import com.solof.fitlog.health.HealthStatus
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

val Context.app: FitLogApp get() = applicationContext as FitLogApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
    )
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, borderColor: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = borderColor?.let { androidx.compose.foundation.BorderStroke(1.dp, it) }
            ?: androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2E8)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
fun CardTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

@Composable
fun MutedText(text: String, color: Color = Muted) = Text(text, style = MaterialTheme.typography.bodyMedium, color = color)

/** Selector nativo de hora de Android, en formato 24 h. */
fun showTimePicker(context: Context, initial: LocalTime, onPicked: (LocalTime) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onPicked(LocalTime.of(h, m)) }, initial.hour, initial.minute, true).show()
}

/** Selector nativo de fecha de Android; no deja elegir días futuros. */
fun showDatePicker(context: Context, initial: LocalDate, onPicked: (LocalDate) -> Unit) {
    DatePickerDialog(
        context,
        { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
}

class HealthState(val status: HealthStatus?, val grant: () -> Unit)

/** Estado de Health Connect, refrescado cada vez que la pantalla vuelve a primer plano. */
@Composable
fun rememberHealthState(): HealthState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<HealthStatus?>(null) }
    val refresh = {
        scope.launch { status = runCatching { HealthConnect.status(context) }.getOrDefault(HealthStatus.UNAVAILABLE) }
        Unit
    }
    val launcher = rememberLauncherForActivityResult(HealthConnect.permissionContract()) { refresh() }
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { refresh() }
    return HealthState(status) { launcher.launch(HealthConnect.PERMISSIONS) }
}
