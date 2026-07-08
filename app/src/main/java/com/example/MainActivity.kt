package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.game.engine.GameViewModel
import com.example.game.ui.GameView
import com.example.game.ui.GameUIOverlay
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Custom Canvas Self-drawn game view
                        AndroidView(
                            factory = { context ->
                                GameView(context).apply {
                                    viewModel = gameViewModel
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Compose menus and dialogue overlay
                        GameUIOverlay(
                            viewModel = gameViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        gameViewModel.saveGame()
    }
}
