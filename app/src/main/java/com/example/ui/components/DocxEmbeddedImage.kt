package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.R
import com.makerandreas.papirusoffice.data.DocumentImages
import java.io.File

/** Test tags for the three states an embedded image can be in. */
object DocumentImageTags {
    const val IMAGE = "document_image"
    const val PENDING = "document_image_pending"
    const val MISSING = "document_image_missing"
}

/**
 * Cache keys whose predecode is still running (Plan 6C). While a key is in
 * this set the image draws its placeholder without starting its own request;
 * when the predecode lands, the image composes straight from the memory
 * cache, so it goes from placeholder to picture in one frame.
 */
val LocalPendingImageDecodes = compositionLocalOf { emptySet<String>() }

/** Builds the one request shape the page and the predecoder share. */
fun documentImageRequest(
    context: android.content.Context,
    file: File,
    decodeSize: Pair<Int, Int>
): ImageRequest = ImageRequest.Builder(context)
    .data(file)
    .memoryCacheKey(DocumentImages.cacheKey(file, decodeSize))
    .size(Size(decodeSize.first, decodeSize.second))
    .crossfade(false)
    .build()

/**
 * An embedded document image drawn in exactly the box the paginator reserved
 * ([widthDp] x [heightDp] on the sheet). The box never collapses: while the
 * bitmap decodes it holds a flat placeholder, and if the file is gone or
 * cannot be decoded it says so instead of disappearing.
 *
 * [widthUnits]/[heightUnits] are the declared layout extent; the decode size
 * comes from them, not from zoom, so predecoded bitmaps are reused.
 */
@Composable
fun DocxEmbeddedImage(
    imageFile: File?,
    widthDp: Dp,
    heightDp: Dp,
    widthUnits: Float,
    heightUnits: Float
) {
    // Exactly the reserved box: the paginator already placed spacing around it.
    val boxModifier = Modifier.size(widthDp, heightDp)
    if (imageFile == null || !imageFile.isFile) {
        MissingDocumentImage(boxModifier)
        return
    }
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val decodeSize = remember(widthUnits, heightUnits, density) {
        DocumentImages.decodeSizePx(DocumentImages.box(widthUnits, heightUnits), density)
    }
    val key = DocumentImages.cacheKey(imageFile, decodeSize)
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest
    if (key in LocalPendingImageDecodes.current) {
        Box(boxModifier.background(placeholderColor).testTag(DocumentImageTags.PENDING))
        return
    }
    var failed by remember(key) { mutableStateOf(false) }
    if (failed) {
        MissingDocumentImage(boxModifier)
        return
    }
    val request = remember(key) { documentImageRequest(context, imageFile, decodeSize) }
    val placeholder = remember(placeholderColor) { ColorPainter(placeholderColor) }
    AsyncImage(
        model = request,
        contentDescription = stringResource(R.string.cd_docx_image_element),
        modifier = boxModifier.testTag(DocumentImageTags.IMAGE),
        placeholder = placeholder,
        contentScale = ContentScale.Fit,
        onError = { failed = true }
    )
}

/** The reserved box with a plain "Image unavailable" label. */
@Composable
fun MissingDocumentImage(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.inky_image_unavailable)
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .semantics { contentDescription = label }
            .testTag(DocumentImageTags.MISSING),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
