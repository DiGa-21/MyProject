package com.myhomechores.app.features.activities

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.myhomechores.app.ui.theme.MyHomeChoresTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TryActivitiesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun catalog_shows_all_six_cards_and_placeholder_dialog() {
        composeRule.setContent {
            MyHomeChoresTheme { TryActivitiesContent(onOpenEnglish = {}) }
        }

        listOf("Английский", "Математика", "Медитация", "Дыхание", "Природа", "Обо мне")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }

        composeRule.onNodeWithText("Математика").performClick()
        composeRule.onNodeWithText("Раздел скоро появится").assertIsDisplayed()
    }

    @Test
    fun introduction_sends_the_current_word_to_the_speaker() {
        val speaker = FakeWordSpeaker()
        composeRule.setContent {
            MyHomeChoresTheme {
                EnglishNatureLessonScreen(
                    rewardStatus = "NotRequested",
                    onCompleted = {},
                    onBack = {},
                    speakerFactory = { speaker },
                )
            }
        }

        composeRule.onNodeWithText("Послушать").performClick()
        assertEquals(listOf("sun"), speaker.spokenWords)
    }

}

private class FakeWordSpeaker : WordSpeaker {
    val spokenWords = mutableListOf<String>()
    override fun speak(word: String) { spokenWords += word }
    override fun close() = Unit
}
