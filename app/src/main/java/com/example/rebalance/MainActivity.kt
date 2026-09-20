package com.example.rebalance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.rebalance.ui.RebalanceApp
import com.example.rebalance.ui.RebalanceViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RebalanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RebalanceApp(viewModel = viewModel)
        }
    }
}
