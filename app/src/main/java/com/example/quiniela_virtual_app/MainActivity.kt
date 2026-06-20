package com.example.quiniela_virtual_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.quiniela_virtual_app.presentation.navigation.QuinielaNavGraph
import com.example.quiniela_virtual_app.presentation.theme.QuinielaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuinielaTheme {
                QuinielaNavGraph()
            }
        }
    }
}
