package com.myhomechores.app.features.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EnglishNatureLessonScreen(
    rewardStatus: String,
    onCompleted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    speakerFactory: (() -> WordSpeaker)? = null,
) {
    var state by remember { mutableStateOf(initialEnglishLessonState()) }
    val context = LocalContext.current
    val speaker = remember { speakerFactory?.invoke() ?: AndroidWordSpeaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.close() } }
    LaunchedEffect(state.stage) {
        if (state.stage == EnglishLessonStage.COMPLETED) onCompleted()
    }

    val feedback = state.feedback as? AnswerFeedback.TryAgain
    if (feedback != null) {
        val correct = state.words.first { it.id == feedback.correctWordId }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Почти получилось") },
            text = { Text("Правильный ответ: ${correct.english} — ${correct.russian}. Попробуй ещё раз.") },
            confirmButton = {
                TextButton(onClick = {
                    state = reduceEnglishLesson(state, EnglishLessonEvent.ContinueAfterFeedback)
                }) { Text("Продолжить") }
            },
        )
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.height(48.dp)) { Text("Назад к занятиям") }
        Text("Английский · Природа", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        val progress = lessonProgress(state)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(9.dp),
            color = Color(0xFFFFB300),
        )

        when (state.stage) {
            EnglishLessonStage.INTRODUCTION -> IntroductionStep(
                word = state.words[state.currentIndex],
                position = state.currentIndex + 1,
                speaker = speaker,
                onRepeated = { state = reduceEnglishLesson(state, EnglishLessonEvent.RepeatedWord) },
            )
            EnglishLessonStage.LISTEN_AND_CHOOSE -> AnswerStep(
                title = "Послушай и выбери картинку",
                currentWord = state.words[state.currentIndex],
                words = state.words,
                showEnglishOptions = false,
                speaker = speaker,
                onAnswer = { state = reduceEnglishLesson(state, EnglishLessonEvent.Answered(it)) },
            )
            EnglishLessonStage.MATCH_PAIRS -> PairMatchingStep(
                state = state,
                onCard = { state = reduceEnglishLesson(state, EnglishLessonEvent.SelectedPairCard(it)) },
            )
            EnglishLessonStage.RECALL -> AnswerStep(
                title = "Как это называется по-английски?",
                currentWord = state.words[state.currentIndex],
                words = state.words,
                showEnglishOptions = true,
                speaker = null,
                onAnswer = { state = reduceEnglishLesson(state, EnglishLessonEvent.Answered(it)) },
            )
            EnglishLessonStage.FINAL_QUIZ -> {
                val current = state.words.first { it.id == state.finalQueue.first() }
                AnswerStep(
                    title = "Финальная проверка",
                    currentWord = current,
                    words = state.words,
                    showEnglishOptions = true,
                    speaker = null,
                    onAnswer = { state = reduceEnglishLesson(state, EnglishLessonEvent.Answered(it)) },
                )
            }
            EnglishLessonStage.COMPLETED -> CompletedStep(
                rewardStatus = rewardStatus,
                onBack = onBack,
                onRestart = { state = reduceEnglishLesson(state, EnglishLessonEvent.Restart) },
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun IntroductionStep(
    word: NatureWord,
    position: Int,
    speaker: WordSpeaker,
    onRepeated: () -> Unit,
) {
    LessonCard {
        Text("Слово $position из 4", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        NatureWordIllustration(word.id, Modifier.fillMaxWidth().height(180.dp))
        Text(word.english, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(word.russian, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Послушай и повтори слово вслух.", textAlign = TextAlign.Center)
        OutlinedButton(onClick = { speaker.speak(word.english) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Послушать")
        }
        Button(onClick = onRepeated, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Повторил(а)") }
    }
}

@Composable
private fun AnswerStep(
    title: String,
    currentWord: NatureWord,
    words: List<NatureWord>,
    showEnglishOptions: Boolean,
    speaker: WordSpeaker?,
    onAnswer: (String) -> Unit,
) {
    LessonCard {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (speaker != null) {
            Button(onClick = { speaker.speak(currentWord.english) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Послушать слово")
            }
        } else {
            NatureWordIllustration(currentWord.id, Modifier.fillMaxWidth().height(180.dp))
        }
        words.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { word ->
                    OutlinedButton(
                        onClick = { onAnswer(word.id) },
                        modifier = Modifier.weight(1f).height(if (showEnglishOptions) 56.dp else 132.dp),
                    ) {
                        if (showEnglishOptions) {
                            Text(word.english, fontWeight = FontWeight.Bold)
                        } else {
                            NatureWordIllustration(word.id, Modifier.size(92.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairMatchingStep(state: EnglishLessonState, onCard: (String) -> Unit) {
    LessonCard {
        Text("Соедини английское слово с переводом", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        val cards = state.words.flatMap { word -> listOf("en:${word.id}" to word.english, "ru:${word.id}" to word.russian) }
        cards.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (cardId, label) ->
                    val wordId = cardId.substringAfter(':')
                    val matched = wordId in state.matchedWordIds
                    Button(
                        onClick = { onCard(cardId) },
                        enabled = !matched,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) { Text(if (matched) "Готово" else label) }
                }
            }
        }
    }
}

@Composable
private fun CompletedStep(rewardStatus: String, onBack: () -> Unit, onRestart: () -> Unit) {
    LessonCard {
        Text("Все четыре слова выучены!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            when (rewardStatus) {
                "Granting" -> "Сохраняем награду…"
                "AlreadyClaimed" -> "Сегодня награда уже получена — повторение всё равно полезно."
                else -> "+5 звёзд"
            },
            color = Color(0xFF8A5A00),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("В раздел «Попробуй»") }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Повторить урок") }
    }
}

@Composable
private fun LessonCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEFF)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private fun lessonProgress(state: EnglishLessonState): Float = when (state.stage) {
    EnglishLessonStage.INTRODUCTION -> state.currentIndex / 24f
    EnglishLessonStage.LISTEN_AND_CHOOSE -> (4 + state.currentIndex) / 24f
    EnglishLessonStage.MATCH_PAIRS -> (8 + state.matchedWordIds.size) / 24f
    EnglishLessonStage.RECALL -> (12 + state.currentIndex) / 24f
    EnglishLessonStage.FINAL_QUIZ -> (16 + state.correctFinalWordIds.size * 2) / 24f
    EnglishLessonStage.COMPLETED -> 1f
}
