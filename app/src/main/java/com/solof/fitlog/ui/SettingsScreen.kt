package com.solof.fitlog.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.solof.fitlog.BuildConfigInfo
import com.solof.fitlog.health.HealthStatus
import com.solof.fitlog.parser.DateOrder

private const val REPO_URL = "https://github.com/cescur/ringfitsync"
private const val INSPIRATION_URL = "https://github.com/watanabetoshinori/RingFitConnect"

const val PRIVACY_TEXT =
    "El texto de las fotos se reconoce en el propio móvil y los entrenamientos solo se guardan en esta app y en " +
        "Health Connect. La app no usa internet, no tiene anuncios ni analítica, y solo comparte datos cuando tú " +
        "exportas el CSV."

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = context.app.settings
    val health = rememberHealthState()
    var dateOrder by remember { mutableStateOf(settings.dateOrder) }
    var startTime by remember { mutableStateOf(settings.defaultStartTime) }

    Scaffold(topBar = { AppBar("Ajustes", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard {
                CardTitle("Formato de fecha en el juego")
                MutedText("Si un número es mayor que 12, el orden se deduce solo.")
                listOf(DateOrder.DMY to "Día/mes (29/11)", DateOrder.MDY to "Mes/día (11/29)").forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { dateOrder = value; settings.dateOrder = value },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = dateOrder == value, onClick = { dateOrder = value; settings.dateOrder = value })
                        Text(label)
                    }
                }
            }

            SectionCard {
                CardTitle("Hora de inicio por defecto")
                MutedText("La pantalla del juego no dice a qué hora entrenaste. Se usa esta hora y puedes cambiarla en la revisión.")
                OutlinedButton(onClick = {
                    showTimePicker(context, startTime) { startTime = it; settings.defaultStartTime = it }
                }) { Text(startTime.toString()) }
            }

            SectionCard {
                CardTitle("Health Connect")
                MutedText(health.status?.text ?: "Comprobando…", color = if (health.status == HealthStatus.OK) Ok else Muted)
                if (health.status == HealthStatus.MISSING_PERMISSIONS) {
                    Button(onClick = health.grant, modifier = Modifier.fillMaxWidth()) { Text("Dar permisos") }
                }
                if (health.status != null && health.status != HealthStatus.UNAVAILABLE) {
                    Text(
                        "Abrir ajustes de Health Connect",
                        color = Accent,
                        modifier = Modifier.clickable {
                            runCatching { context.startActivity(com.solof.fitlog.health.HealthConnect.settingsIntent()) }
                        }.padding(vertical = 6.dp),
                    )
                }
            }

            SectionCard {
                CardTitle("Privacidad")
                MutedText(PRIVACY_TEXT)
            }

            SectionCard {
                CardTitle("Acerca de")
                MutedText("Versión ${BuildConfigInfo.versionName(context)}")
                MutedText(
                    "Esta app no está afiliada, patrocinada ni aprobada por Nintendo. Nintendo Switch y Ring Fit Adventure son " +
                        "marcas registradas de Nintendo. Health Connect es una marca de Google LLC.",
                )
                val linkStyle = TextLinkStyles(SpanStyle(color = Accent, textDecoration = TextDecoration.Underline))
                Text(
                    buildAnnotatedString {
                        append("Software libre con licencia MIT. La lógica de lectura se inspira en ")
                        withLink(LinkAnnotation.Url(INSPIRATION_URL, linkStyle)) { append("RingFitConnect") }
                        append(" de Toshinori Watanabe (MIT).")
                    },
                    color = Muted,
                )
                MutedText("Esta app se hizo con ayuda de inteligencia artificial (Claude Code), con la dirección y las pruebas de su autor.")
                Text(
                    "Código fuente en GitHub",
                    color = Accent,
                    modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) }
                        .padding(vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(topBar = { AppBar("Privacidad", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            SectionCard {
                CardTitle("Cómo usa FitLog tus datos")
                MutedText(PRIVACY_TEXT)
                MutedText(
                    "FitLog solo escribe en Health Connect sesiones de ejercicio, calorías activas y distancia de los días " +
                        "que confirmas, y lee esos mismos tipos para poder reemplazar o borrar lo que guardó.",
                )
            }
        }
    }
}
