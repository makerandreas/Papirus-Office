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

/**
 * Composition smoke test, not a visual regression test.
 *
 * These three cases render `InkyModule` on a phone portrait, a
 * tablet landscape and a foldable qualifier and write one PNG each under
 * `src/test/screenshots/`.
 *
 * Nothing compares the PNGs to anything. CI runs `testDebugUnitTest` with
 * `roborazzi.test.record=true`, which records, and `verifyRoborazziDebug` appears nowhere
 * in this repository. A pixel that moves is not a failure; a green run here means the
 * composition reached the capture call and no more.
 *
 * `captureRoboImage(onRoot())` resolves the root semantics node and throws when the
 * composition produced nothing, so a broken or empty composition already fails these
 * cases. What no case covers is a change in what that composition draws.
 *
 * Committing goldens was deferred until `runs-on` was pinned to `ubuntu-24.04` and
 * the Compose BOM upgrade landed, because both move pixel rendering and rebaselining
 * before them would have baked in a baseline with no product reason behind it. Both
 * have now landed (audit 024), so `.github/workflows/goldens.yml` records the PNGs on
 * a pinned runner and opens them as a pull request. `ci.yml` still records rather than
 * verifies; switching it to `verifyRoborazziDebug` must land after those goldens do.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class InkySnapshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  @Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel8)
  fun inky_render_phone_portrait() {
    composeTestRule.setContent {
      PapirusTheme {
        InkyModule(
          isTablet = false,
          onFormatAction = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/inky_phone_portrait.png")
  }

  @Test
  @Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.PixelTablet)
  fun inky_render_tablet_landscape() {
    composeTestRule.setContent {
      PapirusTheme {
        InkyModule(
          isTablet = true,
          onFormatAction = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/inky_tablet_landscape.png")
  }

  @Test
  @Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.PixelFold)
  fun inky_render_foldable() {
    composeTestRule.setContent {
      PapirusTheme {
        InkyModule(
          isTablet = false,
          onFormatAction = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/inky_foldable.png")
  }
}
