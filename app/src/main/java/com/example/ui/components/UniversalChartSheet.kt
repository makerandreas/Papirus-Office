package com.example.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalChartSheet(
    onDismiss: () -> Unit,
    title: String = "Charts"
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Chart options not available in this build")
    }
}
