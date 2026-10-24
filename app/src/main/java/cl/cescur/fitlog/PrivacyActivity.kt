package cl.cescur.fitlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.cescur.fitlog.ui.FitLogTheme
import cl.cescur.fitlog.ui.PrivacyScreen

/** Política de privacidad que Health Connect muestra desde su diálogo de permisos. */
class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FitLogTheme { PrivacyScreen(onBack = ::finish) } }
    }
}
