package cl.cescur.fitlog.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.cescur.fitlog.BuildConfigInfo
import cl.cescur.fitlog.health.HealthConnect
import cl.cescur.fitlog.health.HealthStatus
import cl.cescur.fitlog.parser.DateOrder

private const val REPO_URL = "https://github.com/cescur/ringfitsync"
private const val INSPIRATION_URL = "https://github.com/watanabetoshinori/RingFitConnect"

const val PRIVACY_TEXT =
    "El texto de las fotos se reconoce en el propio móvil y los entrenamientos solo se guardan en esta app y en " +
        "Health Connect. La app no usa internet, no tiene anuncios ni analítica, y solo comparte datos cuando tú " +
        "exportas el CSV."

private val DATE_ORDER_LABELS = mapOf(DateOrder.DMY to "Día/mes (29/11)", DateOrder.MDY to "Mes/día (11/29)")

private fun Context.openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

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

    Scaffold(topBar = { AppBar("Ajustes", onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { SectionHeader("Importación") }
            item {
                SettingItem(Icons.Outlined.Event, "Formato de fecha del juego", DATE_ORDER_LABELS.getValue(dateOrder)) {
                    choosingDateOrder = true
                }
            }
            item {
                SettingItem(
                    Icons.Outlined.Schedule,
                    "Hora de inicio por defecto",
                    "$startTime · el juego no guarda la hora, puedes cambiarla al revisar",
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
                    "Estado",
                    health.status?.text ?: "Comprobando…",
                    iconTint = when (health.status) {
                        null -> Muted
                        HealthStatus.OK -> Ok
                        else -> Warn
                    },
                    trailing = if (health.status == HealthStatus.MISSING_PERMISSIONS) {
                        { FilledTonalButton(onClick = health.grant) { Text("Dar permisos") } }
                    } else {
                        null
                    },
                )
            }
            if (health.status != null && health.status != HealthStatus.UNAVAILABLE) {
                item {
                    SettingItem(Icons.AutoMirrored.Outlined.OpenInNew, "Abrir Health Connect", "Permisos y datos guardados") {
                        runCatching { context.startActivity(HealthConnect.settingsIntent()) }
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }
            item { SectionHeader("Privacidad") }
            item { SettingItem(Icons.Outlined.Policy, "Política de privacidad", "Todo se queda en tu móvil", onClick = onPrivacy) }

            item { HorizontalDivider(Modifier.padding(top = 8.dp)) }
            item { SectionHeader("Acerca de") }
            item { SettingItem(Icons.Outlined.Info, "Versión", BuildConfigInfo.versionName(context)) }
            item {
                SettingItem(Icons.Outlined.Code, "Código fuente", "github.com/cescur/ringfitsync") { context.openUrl(REPO_URL) }
            }
            item {
                SettingItem(Icons.Outlined.Lightbulb, "Inspirada en RingFitConnect", "De Toshinori Watanabe, licencia MIT") {
                    context.openUrl(INSPIRATION_URL)
                }
            }
            item { SettingItem(Icons.Outlined.Description, "Licencia", "MIT, software libre") }
            item {
                SettingItem(
                    Icons.Outlined.SmartToy,
                    "Hecha con IA",
                    "Se programó con Claude Code, con la dirección y las pruebas de su autor.",
                )
            }
            item {
                SettingItem(
                    Icons.Outlined.Gavel,
                    "Aviso legal",
                    "No está afiliada, patrocinada ni aprobada por Nintendo. Nintendo Switch y Ring Fit Adventure son " +
                        "marcas registradas de Nintendo. Health Connect es una marca de Google LLC.",
                )
            }
        }
    }

    if (choosingDateOrder) {
        AlertDialog(
            onDismissRequest = { choosingDateOrder = false },
            icon = { Icon(Icons.Outlined.Event, contentDescription = null) },
            title = { Text("Formato de fecha del juego") },
            text = {
                Column {
                    Text(
                        "Si un número es mayor que 12, el orden se deduce solo.",
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
                            Text(label, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingDateOrder = false }) { Text("Cerrar") } },
        )
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(topBar = { AppBar("Privacidad", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text("Cómo usa FitLog tus datos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(PRIVACY_TEXT, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
            Text(
                "FitLog solo escribe en Health Connect sesiones de ejercicio, calorías activas y distancia de los días " +
                    "que confirmas, y lee esos mismos tipos para poder reemplazar o borrar lo que guardó.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
