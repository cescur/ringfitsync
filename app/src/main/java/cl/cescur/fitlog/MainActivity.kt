package cl.cescur.fitlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.cescur.fitlog.ui.FitLogTheme
import cl.cescur.fitlog.ui.HistoryScreen
import cl.cescur.fitlog.ui.HomeScreen
import cl.cescur.fitlog.ui.PrivacyScreen
import cl.cescur.fitlog.ui.ReviewScreen
import cl.cescur.fitlog.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitLogTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onScanned = { nav.navigate("review") },
                            onHistory = { nav.navigate("history") },
                            onSettings = { nav.navigate("settings") },
                        )
                    }
                    composable("review") {
                        ReviewScreen(
                            onBack = { nav.popBackStack() },
                            onSaved = { nav.navigate("history") { popUpTo("home") } },
                        )
                    }
                    composable("history") { HistoryScreen(onBack = { nav.popBackStack() }) }
                    composable("settings") {
                        SettingsScreen(onBack = { nav.popBackStack() }, onPrivacy = { nav.navigate("privacy") })
                    }
                    composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}
