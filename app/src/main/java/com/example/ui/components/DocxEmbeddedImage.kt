package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makerandreas.papirusoffice.data.util.OpenXmlUnits
import java.io.File

@Composable
fun DocxEmbeddedImage(
    imageFile: File?,
    extentCx: Long = 0L,
    extentCy: Long = 0L
) {
    if (imageFile == null || !imageFile.exists()) return

    // Menghitung ukuran asli dari tag OpenXML
    val widthDp = OpenXmlUnits.emuToDp(extentCx)
    val heightDp = OpenXmlUnits.emuToDp(extentCy)

    val modifier = if (widthDp != Dp.Unspecified && heightDp != Dp.Unspecified) {
        Modifier
            .width(widthDp)
            .height(heightDp)
    } else {
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    }

    // Memuat gambar secara asinkron tanpa mengunci UI Thread
    AsyncImage(
        model = imageFile,
        contentDescription = "Docx Image Element",
        modifier = modifier.padding(vertical = 4.dp),
        contentScale = ContentScale.Fit
    )
}
