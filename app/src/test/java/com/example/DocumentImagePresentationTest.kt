package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.DocumentImageTags
import com.example.ui.components.DocxEmbeddedImage
import com.example.ui.components.LocalPendingImageDecodes
import com.example.ui.theme.PapirusTheme
import com.makerandreas.papirusoffice.data.DocumentImages
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Base64

/**
 * Plan 6C (F-3): an embedded image always occupies exactly the box the
 * paginator reserved, in each of its three states: missing, waiting for the
 * predecode, and drawn.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DocumentImagePresentationTest {

    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun png(name: String): File = File(context.cacheDir, name).apply {
        writeBytes(Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAIAAAAmkwkpAAAAEElEQVR4nGNQSDgARwzEcQD+QxQB7BidQwAAAABJRU5ErkJggg=="
        ))
    }

    @Test
    fun missingMediaKeepsTheReservedBoxAndSaysSo() {
        compose.setContent {
            PapirusTheme {
                DocxEmbeddedImage(
                    imageFile = File(context.cacheDir, "does-not-exist.png"),
                    widthDp = 146.dp, heightDp = 82.dp, widthUnits = 292f, heightUnits = 165f
                )
            }
        }
        compose.onNodeWithTag(DocumentImageTags.MISSING)
            .assertWidthIsEqualTo(146.dp)
            .assertHeightIsEqualTo(82.dp)
        compose.onNodeWithContentDescription(context.getString(R.string.inky_image_unavailable)).assertExists()
    }

    @Test
    fun nullFileIsTreatedAsMissingNotAsNothing() {
        compose.setContent {
            PapirusTheme {
                DocxEmbeddedImage(null, widthDp = 100.dp, heightDp = 75.dp, widthUnits = 0f, heightUnits = 0f)
            }
        }
        compose.onNodeWithTag(DocumentImageTags.MISSING)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(75.dp)
    }

    @Test
    fun pendingPredecodeHoldsAPlaceholderOfTheSameSize() {
        val file = png("plan6c-pending.png")
        val density = context.resources.displayMetrics.density
        val key = DocumentImages.cacheKey(file, DocumentImages.decodeSizePx(DocumentImages.box(292f, 165f), density))
        compose.setContent {
            PapirusTheme {
                CompositionLocalProvider(LocalPendingImageDecodes provides setOf(key)) {
                    DocxEmbeddedImage(file, widthDp = 146.dp, heightDp = 82.dp, widthUnits = 292f, heightUnits = 165f)
                }
            }
        }
        compose.onNodeWithTag(DocumentImageTags.PENDING)
            .assertWidthIsEqualTo(146.dp)
            .assertHeightIsEqualTo(82.dp)
        compose.onNodeWithTag(DocumentImageTags.IMAGE).assertDoesNotExist()
    }

    @Test
    fun storedImageDrawsAtTheReservedSize() {
        val file = png("plan6c-drawn.png")
        compose.setContent {
            PapirusTheme {
                DocxEmbeddedImage(file, widthDp = 146.dp, heightDp = 82.dp, widthUnits = 292f, heightUnits = 165f)
            }
        }
        compose.onNodeWithTag(DocumentImageTags.IMAGE)
            .assertWidthIsEqualTo(146.dp)
            .assertHeightIsEqualTo(82.dp)
        compose.onNodeWithTag(DocumentImageTags.PENDING).assertDoesNotExist()
    }
}
