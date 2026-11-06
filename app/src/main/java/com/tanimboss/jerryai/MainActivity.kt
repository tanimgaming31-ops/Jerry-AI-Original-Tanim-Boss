// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai

import android.os.Bundle
import androidx.activity.viewModels
import com.tanimboss.jerryai.voice.VoiceViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tanimboss.jerryai.ui.JerryApp

class MainActivity : ComponentActivity() {
    private val voice: VoiceViewModel by viewModels()
    override fun onStop() {
        if (!isChangingConfigurations) voice.stop()
        super.onStop()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { JerryApp(viewModel<JerryViewModel>()) }
    }
}
