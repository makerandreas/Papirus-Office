package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.modules.cellina.CellinaModule
import com.example.ui.theme.PapirusTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CellinaSnapshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  @Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel8)
  fun cellina_render_phone_portrait() {
    composeTestRule.setContent {
      PapirusTheme {
        CellinaModule(
          isTablet = false,
          onFormulaSelected = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/cellina_phone_portrait.png")
  }

  @Test
  @Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.PixelTablet)
  fun cellina_render_tablet_landscape() {
    composeTestRule.setContent {
      PapirusTheme {
        CellinaModule(
          isTablet = true,
          onFormulaSelected = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/cellina_tablet_landscape.png")
  }

  @Test
  @Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.PixelFold)
  fun cellina_render_foldable() {
    composeTestRule.setContent {
      PapirusTheme {
        CellinaModule(
          isTablet = false,
          onFormulaSelected = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/cellina_foldable.png")
  }
}
