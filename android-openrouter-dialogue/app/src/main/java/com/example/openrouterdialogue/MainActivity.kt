package com.example.openrouterdialogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.openrouterdialogue.api.OpenRouterApi
import com.example.openrouterdialogue.ui.OpenRouterDialogueScreen
import com.example.openrouterdialogue.ui.OpenRouterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OpenRouterDialogueScreen(
                viewModel = OpenRouterViewModel(
                    service = OpenRouterApi.create()
                )
            )
        }
    }
}
