package com.livevault.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.livevault.core.ui.theme.LiveVaultTheme
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysAllCoreElements() {
        composeTestRule.setContent {
            LiveVaultTheme {
                LoginScreen(
                    onNavigateToHome = {},
                    onNavigateToRegister = {},
                    viewModel = mockk(relaxed = true)
                )
            }
        }

        // Verify branding and input fields
        composeTestRule.onNodeWithText("LiveVault").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign Up").assertIsDisplayed()
    }

    @Test
    fun loginScreen_inputsAcceptText() {
        composeTestRule.setContent {
            LiveVaultTheme {
                LoginScreen(
                    onNavigateToHome = {},
                    onNavigateToRegister = {},
                    viewModel = mockk(relaxed = true)
                )
            }
        }

        composeTestRule.onNodeWithText("Email").performTextInput("test@livevault.app")
        composeTestRule.onNodeWithText("Password").performTextInput("secret123")
        composeTestRule.onNodeWithText("Sign In").performClick()
    }
}
