package com.kouda.tactical

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.kouda.tactical.ui.minecraft.MinecraftScreen
import com.kouda.tactical.ui.minecraft.MinecraftViewModel
import com.kouda.tactical.ui.roblox.RobloxScreen
import com.kouda.tactical.ui.roblox.RobloxViewModel
import com.kouda.tactical.ui.theme.KoudaTheme

class MainActivity : ComponentActivity() {

    private val sourceViewModel: KoudaViewModel by viewModels()
    private val minecraftViewModel: MinecraftViewModel by viewModels()
    private val robloxViewModel: RobloxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoudaTheme {
                var screen by remember { mutableStateOf("menu") }

                when (screen) {
                    "menu" -> MenuScreen(
                        onEnterSource    = { screen = "source" },
                        onEnterMinecraft = { screen = "minecraft" },
                        onEnterRoblox    = { screen = "roblox" }
                    )
                    "source" -> ServerListScreen(
                        viewModel = sourceViewModel,
                        onBack    = { screen = "menu" }
                    )
                    "minecraft" -> MinecraftScreen(
                        viewModel = minecraftViewModel,
                        onBack    = { screen = "menu" }
                    )
                    "roblox" -> RobloxScreen(
                        viewModel = robloxViewModel,
                        onBack    = { screen = "menu" }
                    )
                }
            }
        }
    }
}
