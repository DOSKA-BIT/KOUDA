package com.kouda.tactical

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.kouda.tactical.ui.theme.KoudaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: KoudaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoudaTheme {
                var screen by remember { mutableStateOf("menu") }
                when (screen) {
                    "menu"    -> MenuScreen(onEnter = { screen = "servers" })
                    "servers" -> ServerListScreen(viewModel = viewModel, onBack = { screen = "menu" })
                }
            }
        }
    }
}
