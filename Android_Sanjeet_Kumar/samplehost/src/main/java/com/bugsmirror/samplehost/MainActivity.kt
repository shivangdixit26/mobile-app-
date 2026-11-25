package com.bugsmirror.samplehost

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bugsmirror.samplehost.ui.RiskCheckerApp
import com.bugsmirror.samplehost.ui.theme.SampleHostTheme

/** The only Activity: shows the Compose UI in [RiskCheckerApp]. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleHostTheme {
                RiskCheckerApp()
            }
        }
    }
}
