package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.modules.inky.InkyModule
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
class InkySnapshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8)
  fun inky_render_phone_portrait() {
    composeTestRule.setContent {
      PapirusTheme {
        InkyModule(
          isTablet = false,
          onFormatAction = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/inky_phone_portrait.png")
  }

  @Test
  @Config(qualifiers = RobolectricDeviceQualifiers.PixelTablet)
  fun inky_render_tablet_landscape() {
    composeTestRule.setContent {
      PapirusTheme {
        InkyModule(
          isTablet = true,
          onFormatAction = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/inky_tablet_landscape.png")
  }

  @Test
  @Config(qualifiers = RobolectricDeviceQualifiers.PixelFold)
  fun inky_render_foldable() {
    composeTestRule.setContent {
      PapirusTheme {
        InkyModule(
          isTablet = false,
          onFormatAction = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/inky_foldable.png")
  }
}
