package com.myhomechores.app.features.activities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishNatureLessonTest {
    @Test
    fun lesson_starts_with_four_nature_words() {
        val state = initialEnglishLessonState()

        assertEquals(EnglishLessonStage.INTRODUCTION, state.stage)
        assertEquals(listOf("sun", "tree", "flower", "river"), state.words.map { it.english })
        assertEquals(listOf("солнце", "дерево", "цветок", "река"), state.words.map { it.russian })
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun confirming_all_four_words_starts_listen_and_choose() {
        var state = initialEnglishLessonState()
        repeat(4) { state = reduceEnglishLesson(state, EnglishLessonEvent.RepeatedWord) }

        assertEquals(EnglishLessonStage.LISTEN_AND_CHOOSE, state.stage)
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun correct_listen_answers_advance_to_pair_matching() {
        var state = introductionCompletedState()

        natureWordIds.forEach { answer ->
            state = reduceEnglishLesson(state, EnglishLessonEvent.Answered(answer))
        }

        assertEquals(EnglishLessonStage.MATCH_PAIRS, state.stage)
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun wrong_practice_answer_keeps_the_word_available_without_penalty() {
        val state = introductionCompletedState()
        val next = reduceEnglishLesson(state, EnglishLessonEvent.Answered("tree"))

        assertEquals(EnglishLessonStage.LISTEN_AND_CHOOSE, next.stage)
        assertEquals(0, next.currentIndex)
        assertEquals(AnswerFeedback.TryAgain("sun"), next.feedback)
    }

    @Test
    fun four_correct_pairs_advance_to_recall() {
        var state = matchingState()

        natureWordIds.forEach { wordId ->
            state = reduceEnglishLesson(state, EnglishLessonEvent.SelectedPairCard("en:$wordId"))
            state = reduceEnglishLesson(state, EnglishLessonEvent.SelectedPairCard("ru:$wordId"))
        }

        assertEquals(EnglishLessonStage.RECALL, state.stage)
        assertEquals(natureWordIds.toSet(), state.matchedWordIds)
    }

    @Test
    fun correct_recall_answers_create_four_item_final_queue() {
        var state = recallState()

        natureWordIds.forEach { answer ->
            state = reduceEnglishLesson(state, EnglishLessonEvent.Answered(answer))
        }

        assertEquals(EnglishLessonStage.FINAL_QUIZ, state.stage)
        assertEquals(natureWordIds, state.finalQueue)
    }

    @Test
    fun wrong_final_answer_returns_word_to_end_of_queue() {
        val state = finalQuizState(queue = natureWordIds)
        val next = reduceEnglishLesson(state, EnglishLessonEvent.Answered("tree"))

        assertEquals(listOf("tree", "flower", "river", "sun"), next.finalQueue)
        assertTrue(next.correctFinalWordIds.isEmpty())
        assertEquals(AnswerFeedback.TryAgain("sun"), next.feedback)
    }

    @Test
    fun lesson_completes_only_after_all_four_words_are_correct() {
        var state = finalQuizState(queue = natureWordIds)
        natureWordIds.forEach { answer ->
            state = reduceEnglishLesson(state, EnglishLessonEvent.Answered(answer))
        }

        assertEquals(EnglishLessonStage.COMPLETED, state.stage)
        assertEquals(4, state.correctFinalWordIds.size)
    }

    @Test
    fun restart_returns_to_the_first_introduction_word() {
        val restarted = reduceEnglishLesson(
            finalQuizState(queue = natureWordIds),
            EnglishLessonEvent.Restart,
        )

        assertEquals(initialEnglishLessonState(), restarted)
    }

    private fun introductionCompletedState(): EnglishLessonState {
        var state = initialEnglishLessonState()
        repeat(4) { state = reduceEnglishLesson(state, EnglishLessonEvent.RepeatedWord) }
        return state
    }

    private fun matchingState(): EnglishLessonState {
        var state = introductionCompletedState()
        natureWordIds.forEach { answer ->
            state = reduceEnglishLesson(state, EnglishLessonEvent.Answered(answer))
        }
        return state
    }

    private fun recallState(): EnglishLessonState {
        var state = matchingState()
        natureWordIds.forEach { wordId ->
            state = reduceEnglishLesson(state, EnglishLessonEvent.SelectedPairCard("en:$wordId"))
            state = reduceEnglishLesson(state, EnglishLessonEvent.SelectedPairCard("ru:$wordId"))
        }
        return state
    }

    private val natureWordIds = listOf("sun", "tree", "flower", "river")
}
