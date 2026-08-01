package com.myhomechores.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.myhomechores.app.features.scaffold.ScaffoldScreen
import com.myhomechores.app.ui.theme.MyHomeChoresTheme
import org.junit.Rule
import org.junit.Test

class ScaffoldScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scaffoldStatus_isVisible() {
        composeRule.setContent {
            MyHomeChoresTheme {
                ScaffoldScreen(environment = "test")
            }
        }

        composeRule
            .onNodeWithText("Каркас приложения готов")
            .assertIsDisplayed()
    }
}

