package com.myhomechores.app.features.activities

data class NatureWord(
    val id: String,
    val english: String,
    val russian: String,
)

enum class EnglishLessonStage {
    INTRODUCTION,
    LISTEN_AND_CHOOSE,
    MATCH_PAIRS,
    RECALL,
    FINAL_QUIZ,
    COMPLETED,
}

sealed interface AnswerFeedback {
    data class TryAgain(val correctWordId: String) : AnswerFeedback
}

data class EnglishLessonState(
    val words: List<NatureWord>,
    val stage: EnglishLessonStage,
    val currentIndex: Int,
    val selectedPairIds: List<String> = emptyList(),
    val matchedWordIds: Set<String> = emptySet(),
    val finalQueue: List<String> = emptyList(),
    val correctFinalWordIds: Set<String> = emptySet(),
    val feedback: AnswerFeedback? = null,
)

sealed interface EnglishLessonEvent {
    data object RepeatedWord : EnglishLessonEvent
    data class Answered(val answerWordId: String) : EnglishLessonEvent
    data class SelectedPairCard(val cardId: String) : EnglishLessonEvent
    data object ContinueAfterFeedback : EnglishLessonEvent
    data object Restart : EnglishLessonEvent
}

private val natureWords = listOf(
    NatureWord(id = "sun", english = "sun", russian = "солнце"),
    NatureWord(id = "tree", english = "tree", russian = "дерево"),
    NatureWord(id = "flower", english = "flower", russian = "цветок"),
    NatureWord(id = "river", english = "river", russian = "река"),
)

fun initialEnglishLessonState(): EnglishLessonState = EnglishLessonState(
    words = natureWords,
    stage = EnglishLessonStage.INTRODUCTION,
    currentIndex = 0,
)

fun finalQuizState(queue: List<String>): EnglishLessonState = EnglishLessonState(
    words = natureWords,
    stage = EnglishLessonStage.FINAL_QUIZ,
    currentIndex = 0,
    finalQueue = queue,
)

fun reduceEnglishLesson(
    state: EnglishLessonState,
    event: EnglishLessonEvent,
): EnglishLessonState {
    if (event == EnglishLessonEvent.Restart) return initialEnglishLessonState()
    if (event == EnglishLessonEvent.ContinueAfterFeedback) return state.copy(feedback = null)
    if (state.feedback != null) return state

    return when (state.stage) {
        EnglishLessonStage.INTRODUCTION -> reduceIntroduction(state, event)
        EnglishLessonStage.LISTEN_AND_CHOOSE -> reducePracticeAnswers(
            state = state,
            event = event,
            nextStage = EnglishLessonStage.MATCH_PAIRS,
        )
        EnglishLessonStage.MATCH_PAIRS -> reducePairMatching(state, event)
        EnglishLessonStage.RECALL -> reducePracticeAnswers(
            state = state,
            event = event,
            nextStage = EnglishLessonStage.FINAL_QUIZ,
        )
        EnglishLessonStage.FINAL_QUIZ -> reduceFinalQuiz(state, event)
        EnglishLessonStage.COMPLETED -> state
    }
}

private fun reduceIntroduction(
    state: EnglishLessonState,
    event: EnglishLessonEvent,
): EnglishLessonState {
    if (event != EnglishLessonEvent.RepeatedWord) return state
    val nextIndex = state.currentIndex + 1
    return if (nextIndex >= state.words.size) {
        state.copy(stage = EnglishLessonStage.LISTEN_AND_CHOOSE, currentIndex = 0)
    } else {
        state.copy(currentIndex = nextIndex)
    }
}

private fun reducePracticeAnswers(
    state: EnglishLessonState,
    event: EnglishLessonEvent,
    nextStage: EnglishLessonStage,
): EnglishLessonState {
    if (event !is EnglishLessonEvent.Answered) return state
    val expectedWord = state.words[state.currentIndex]
    if (event.answerWordId != expectedWord.id) {
        return state.copy(feedback = AnswerFeedback.TryAgain(expectedWord.id))
    }

    val nextIndex = state.currentIndex + 1
    if (nextIndex < state.words.size) return state.copy(currentIndex = nextIndex)

    return if (nextStage == EnglishLessonStage.FINAL_QUIZ) {
        state.copy(
            stage = nextStage,
            currentIndex = 0,
            finalQueue = state.words.map(NatureWord::id),
        )
    } else {
        state.copy(stage = nextStage, currentIndex = 0)
    }
}

private fun reducePairMatching(
    state: EnglishLessonState,
    event: EnglishLessonEvent,
): EnglishLessonState {
    if (event !is EnglishLessonEvent.SelectedPairCard) return state
    val cardId = event.cardId
    val wordId = cardId.substringAfter(':', missingDelimiterValue = "")
    val language = cardId.substringBefore(':', missingDelimiterValue = "")
    if (wordId !in state.words.map(NatureWord::id) || language !in setOf("en", "ru")) return state
    if (wordId in state.matchedWordIds || cardId in state.selectedPairIds) return state

    if (state.selectedPairIds.isEmpty()) return state.copy(selectedPairIds = listOf(cardId))

    val firstCard = state.selectedPairIds.single()
    val firstWordId = firstCard.substringAfter(':')
    val firstLanguage = firstCard.substringBefore(':')
    if (firstWordId != wordId || firstLanguage == language) {
        return state.copy(
            selectedPairIds = emptyList(),
            feedback = AnswerFeedback.TryAgain(firstWordId),
        )
    }

    val matched = state.matchedWordIds + wordId
    return if (matched.size == state.words.size) {
        state.copy(
            stage = EnglishLessonStage.RECALL,
            currentIndex = 0,
            selectedPairIds = emptyList(),
            matchedWordIds = matched,
        )
    } else {
        state.copy(selectedPairIds = emptyList(), matchedWordIds = matched)
    }
}

private fun reduceFinalQuiz(
    state: EnglishLessonState,
    event: EnglishLessonEvent,
): EnglishLessonState {
    if (event !is EnglishLessonEvent.Answered || state.finalQueue.isEmpty()) return state
    val expectedWordId = state.finalQueue.first()
    val remainingQueue = state.finalQueue.drop(1)

    if (event.answerWordId != expectedWordId) {
        return state.copy(
            finalQueue = remainingQueue + expectedWordId,
            feedback = AnswerFeedback.TryAgain(expectedWordId),
        )
    }

    val correctIds = state.correctFinalWordIds + expectedWordId
    return if (remainingQueue.isEmpty() && correctIds.size == state.words.size) {
        state.copy(
            stage = EnglishLessonStage.COMPLETED,
            finalQueue = emptyList(),
            correctFinalWordIds = correctIds,
        )
    } else {
        state.copy(finalQueue = remainingQueue, correctFinalWordIds = correctIds)
    }
}
