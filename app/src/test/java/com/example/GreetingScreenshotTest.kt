package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.SalimKeypad
import com.example.ui.theme.SalimPhoneTheme
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun dialer_keypad_screenshot() {
    composeTestRule.setContent {
      SalimPhoneTheme {
        SalimKeypad(
          onDigitClicked = {},
          onSpeedDialTriggered = {},
          onVoicemailTriggered = {},
          onCallClicked = {},
          onBackspaceClicked = {},
          onBackspaceLongClicked = {},
          hasDigits = true
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
