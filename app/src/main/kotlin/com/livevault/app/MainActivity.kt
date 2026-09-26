package com.livevault.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.livevault.app.navigation.MainNavGraph
import com.livevault.app.navigation.Screen
import com.livevault.core.network.repository.AuthRepository
import com.livevault.core.ui.theme.LiveVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LiveVaultTheme {
                val navController = rememberNavController()
                var startRoute by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val token = authRepository.getAccessToken()
                    val targetRecordingId = intent?.getStringExtra("KEY_RECORDING_ID")

                    if (token != null) {
                        if (targetRecordingId != null) {
                            startRoute = Screen.Player.createRoute(targetRecordingId)
                        } else {
                            startRoute = Screen.Home.route
                        }
                    } else {
                        startRoute = Screen.Login.route
                    }
                }

                startRoute?.let { route ->
                    MainNavGraph(
                        navController = navController,
                        startDestination = route
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
