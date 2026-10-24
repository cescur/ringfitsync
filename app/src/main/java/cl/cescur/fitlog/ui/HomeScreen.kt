package cl.cescur.fitlog.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import cl.cescur.fitlog.R
import cl.cescur.fitlog.Scan
import cl.cescur.fitlog.health.HealthStatus
import cl.cescur.fitlog.ocr.readImage
import cl.cescur.fitlog.parser.parseWorkoutLog
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeScreen(onScanned: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val health = rememberHealthState()
    var busy by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun process(uri: Uri) {
        busy = true
        scope.launch {
            try {
                val lines = readImage(context, uri)
                val workouts = parseWorkoutLog(lines, context.app.settings.dateOrder)
                context.app.pendingScan = Scan(uri, lines, workouts)
                onScanned()
            } catch (e: Exception) {
                Log.e("FitLog", "No se pudo leer la imagen", e)
                error = e.toString()
            } finally {
                busy = false
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        cameraUri?.takeIf { ok }?.let(::process)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::process)
    }

    Scaffold(
        topBar = {
            AppBar("FitLog") {
                val ok = health.status == HealthStatus.OK
                IconButton(onClick = onSettings) {
                    Icon(
                        if (ok) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = health.status?.text ?: "Comprobando Health Connect",
                        tint = when (health.status) {
                            null -> Muted
                            HealthStatus.OK -> Ok
                            else -> Warn
                        },
                    )
                }
                IconButton(onClick = onHistory) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Historial") }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val status = health.status
            if (status != null && status != HealthStatus.OK) {
                SectionCard(borderColor = Warn) {
                    MutedText(status.text, color = Warn)
                    if (status == HealthStatus.MISSING_PERMISSIONS) {
                        OutlinedButton(onClick = health.grant, modifier = Modifier.fillMaxWidth()) { Text("Dar permisos") }
                    }
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(112.dp).background(AccentSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_ring), contentDescription = null, tint = Accent, modifier = Modifier.size(84.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("Importar entrenamientos", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Haz una foto a la pantalla «Registro de ejercicio» de Ring Fit Adventure o elige una captura de la Switch.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted,
                    textAlign = TextAlign.Center,
                )
            }

            if (busy) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(10.dp))
                    MutedText("Leyendo la imagen…")
                }
            } else {
                Button(
                    onClick = {
                        val dir = File(context.cacheDir, "images").apply { mkdirs() }
                        val file = File(dir, "foto-${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        cameraUri = uri
                        takePicture.launch(uri)
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Hacer foto", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Elegir de la galería", fontSize = 16.sp) }
            }
        }
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("No se pudo leer la imagen") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("Aceptar") } },
        )
    }
}
