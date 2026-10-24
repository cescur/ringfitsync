package cl.cescur.fitlog.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cl.cescur.fitlog.BuildConfigInfo
import cl.cescur.fitlog.R
import cl.cescur.fitlog.health.HealthConnect
import cl.cescur.fitlog.health.HealthStatus
import cl.cescur.fitlog.parser.DateOrder

private const val REPO_URL = "https://github.com/cescur/ringfitsync"
private const val INSPIRATION_URL = "https://github.com/watanabetoshinori/RingFitConnect"

private val DATE_ORDER_LABELS = mapOf(DateOrder.DMY to R.string.settings_dmy, DateOrder.MDY to R.string.settings_mdy)

private fun Context.openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable (() -> Unit)? = null,
    // Último parámetro a propósito: así una lambda final es la acción al pulsar, no el contenido.
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = iconTint) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onPrivacy: () -> Unit) {
    val context = LocalContext.current
    val settings = context.app.settings
    val health = rememberHealthState()
    var dateOrder by remember { mutableStateOf(settings.dateOrder) }
    var startTime by remember { mutableStateOf(settings.defaultStartTime) }
    var choosingDateOrder by remember { mutableStateOf(false) }

    Scaffold(topBar = { AppBar(stringResource(R.string.settings), onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { SectionHeader(stringResource(R.string.settings_import)) }
            item {
                SettingItem(Icons.Outlined.Event, stringResource(R.string.settings_date_format), stringResource(DATE_ORDER_LABELS.getValue(dateOrder))) {
                    choosingDateOrder = true
                }
            }
            item {
                SettingItem(
                    Icons.Outlined.Schedule,
                    stringResource(R.string.settings_start_time),
                    stringResource(R.string.settings_start_time_hint, startTime.toString()),
                ) {
                    showTimePicker(context, startTime) { startTime = it; settings.defaultStartTime = it }
                }
            }

            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }
            item { SectionHeader("Health Connect") }
            item {
                val ok = health.status == HealthStatus.OK
                SettingItem(
                    if (ok) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    stringResource(R.string.settings_health_status),
                    stringResource(health.status?.text ?: R.string.checking),
                    iconTint = when (health.status) {
                        null -> Muted
                        HealthStatus.OK -> Ok
                        else -> Warn
                    },
                    trailing = if (health.status == HealthStatus.MISSING_PERMISSIONS) {
                        { FilledTonalButton(onClick = health.grant) { Text(stringResource(R.string.grant_permissions)) } }
                    } else {
                        null
                    },
                )
            }
            if (health.status != null && health.status != HealthStatus.UNAVAILABLE) {
                item {
                    SettingItem(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(R.string.settings_open_health), stringResource(R.string.settings_open_health_hint)) {
                        runCatching { context.startActivity(HealthConnect.settingsIntent()) }
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }
            item { SectionHeader(stringResource(R.string.settings_privacy)) }
            item { SettingItem(Icons.Outlined.Policy, stringResource(R.string.settings_privacy_policy), stringResource(R.string.settings_privacy_hint), onClick = onPrivacy) }

            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }
            item { SectionHeader(stringResource(R.string.settings_about)) }
            item { SettingItem(Icons.Outlined.Info, stringResource(R.string.settings_version), BuildConfigInfo.versionName(context)) }
            item {
                SettingItem(Icons.Outlined.Code, stringResource(R.string.settings_source), "github.com/cescur/ringfitsync") { context.openUrl(REPO_URL) }
            }
            item {
                SettingItem(Icons.Outlined.Lightbulb, stringResource(R.string.settings_inspired), stringResource(R.string.settings_inspired_hint)) {
                    context.openUrl(INSPIRATION_URL)
                }
            }
            item { SettingItem(Icons.Outlined.Description, stringResource(R.string.settings_license), stringResource(R.string.settings_license_hint)) }
            item {
                SettingItem(
                    Icons.Outlined.SmartToy,
                    stringResource(R.string.settings_ai),
                    stringResource(R.string.settings_ai_hint),
                )
            }
            item {
                SettingItem(
                    Icons.Outlined.Gavel,
                    stringResource(R.string.settings_legal),
                    stringResource(R.string.settings_legal_hint),
                )
            }
        }
    }

    if (choosingDateOrder) {
        AlertDialog(
            onDismissRequest = { choosingDateOrder = false },
            icon = { Icon(Icons.Outlined.Event, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_date_format)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.settings_date_format_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DATE_ORDER_LABELS.forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = dateOrder == value, role = Role.RadioButton) {
                                    dateOrder = value
                                    settings.dateOrder = value
                                    choosingDateOrder = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = dateOrder == value, onClick = null)
                            Text(stringResource(label), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingDateOrder = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(topBar = { AppBar(stringResource(R.string.settings_privacy), onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text(stringResource(R.string.privacy_heading), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.privacy_text), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
            Text(
                stringResource(R.string.privacy_text_health),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
