# Раздел «Попробуй» и английский урок — план реализации

> **Для агентных исполнителей:** ОБЯЗАТЕЛЬНЫЙ ДОПОЛНИТЕЛЬНЫЙ НАВЫК: использовать `superpowers:subagent-driven-development` (рекомендуется) или `superpowers:executing-plans`, выполняя план задача за задачей. Для отслеживания шагов используются чекбоксы `- [ ]`.

**Цель:** добавить в детский экран дел категорию «Попробуй» с сеткой интерактивных занятий и реализовать первый английский урок «Природа» с Android Text-to-Speech, закреплением, итоговой проверкой и ежедневной наградой 5 звёзд.

**Архитектура:** логика английского урока оформляется как чистый reducer без Android-зависимостей, UI разделяется на отдельные Compose-файлы, а системное произношение скрывается за интерфейсом `WordSpeaker`. Факт выдачи дневной награды записывается атомарно в существующую Room-таблицу `rewards`; детский экран наблюдает сумму наград и добавляет её к отображаемому балансу. `ScaffoldScreen.kt` отвечает только за переход из существующего фильтра в новый раздел.

**Технологии:** Kotlin 2.3.21, Jetpack Compose Material 3, Android `TextToSpeech`, Room 2.7.2, JUnit 4, Compose UI Test.

## Общие ограничения

- Минимальная версия Android: `minSdk 28`; целевая версия: `targetSdk 37`.
- Интерактивные карточки отображаются только в «Попробуй» и не попадают во «Все», «Дом», «Учёба» и «Здоровье».
- Английский урок не изменяет прогресс обязательных дел и доступ к основной игре.
- Урок не записывает голос ребёнка, не оценивает произношение и не использует таймер.
- Успешное первое прохождение за календарный день даёт ровно 5 звёзд; повтор в тот же день награды не даёт.
- Ошибки не уменьшают награду и не ограничивают количество попыток.
- Все интерактивные элементы на телефоне имеют область нажатия не менее 48 dp.
- До отправки на GitHub результат проверяется автоматическими тестами, сборкой и вручную на симуляторе.

---

## Структура файлов

Новые файлы:

- `app/src/main/java/com/myhomechores/app/features/activities/ActivityCatalog.kt` — шесть карточек раздела и их доступность.
- `app/src/main/java/com/myhomechores/app/features/activities/EnglishNatureLesson.kt` — словарь, этапы, события и чистый reducer урока.
- `app/src/main/java/com/myhomechores/app/features/activities/WordSpeaker.kt` — интерфейс произношения и Android-реализация Text-to-Speech.
- `app/src/main/java/com/myhomechores/app/features/activities/NatureWordIllustration.kt` — программные векторные иллюстрации четырёх слов.
- `app/src/main/java/com/myhomechores/app/features/activities/TryActivitiesScreen.kt` — сетка 2×3, заглушки и маршрут к уроку.
- `app/src/main/java/com/myhomechores/app/features/activities/EnglishNatureLessonScreen.kt` — знакомство, три упражнения, проверка и экран успеха.
- `app/src/test/java/com/myhomechores/app/features/activities/EnglishNatureLessonTest.kt` — unit-тесты reducer.
- `app/src/androidTest/java/com/myhomechores/app/features/activities/TryActivitiesScreenTest.kt` — Compose-тест сетки и заглушек.
- `app/src/androidTest/java/com/myhomechores/app/features/activities/EnglishNatureLessonScreenTest.kt` — Compose-тест основного пути урока.

Изменяемые файлы:

- `app/src/main/java/com/myhomechores/app/data/AppRepository.kt` — API наблюдения и атомарной выдачи награды.
- `app/src/main/java/com/myhomechores/app/data/local/Daos.kt` — `INSERT OR IGNORE` и сумма звёзд по ребёнку.
- `app/src/main/java/com/myhomechores/app/data/RoomAppRepository.kt` — детерминированный ключ дневной награды.
- `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldViewModel.kt` — ID ребёнка, поток бонусных звёзд и команда выдачи награды.
- `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt` — фильтр «Попробуй», маршрут и отображение бонуса.
- `app/src/main/AndroidManifest.xml` — декларация доступности TTS-сервисов Android 11+.
- `app/src/androidTest/java/com/myhomechores/app/data/local/RoomRepositoryTest.kt` — проверка одной награды в день и повторной награды завтра.
- тестовые реализации `AppRepository` в `ChildConnectionViewModelTest.kt` и `InviteCodeViewModelTest.kt` — новые методы интерфейса.

---

### Задача 1. Чистая модель английского урока

**Файлы:**

- Создать: `app/src/main/java/com/myhomechores/app/features/activities/EnglishNatureLesson.kt`
- Создать тест: `app/src/test/java/com/myhomechores/app/features/activities/EnglishNatureLessonTest.kt`

**Интерфейсы:**

- Создаёт `NatureWord`, `EnglishLessonStage`, `EnglishLessonState`, `EnglishLessonEvent`.
- Создаёт `initialEnglishLessonState(): EnglishLessonState`.
- Создаёт `reduceEnglishLesson(state: EnglishLessonState, event: EnglishLessonEvent): EnglishLessonState`.
- UI последующих задач изменяет состояние только через reducer.

- [ ] **Шаг 1. Написать падающий тест начального словаря и знакомства**

```kotlin
@Test
fun lesson_starts_with_four_nature_words() {
    val state = initialEnglishLessonState()

    assertEquals(EnglishLessonStage.INTRODUCTION, state.stage)
    assertEquals(listOf("sun", "tree", "flower", "river"), state.words.map { it.english })
    assertEquals(0, state.currentIndex)
}

@Test
fun confirming_all_four_words_starts_listen_and_choose() {
    var state = initialEnglishLessonState()
    repeat(4) { state = reduceEnglishLesson(state, EnglishLessonEvent.RepeatedWord) }

    assertEquals(EnglishLessonStage.LISTEN_AND_CHOOSE, state.stage)
    assertEquals(0, state.currentIndex)
}
```

- [ ] **Шаг 2. Запустить тест и подтвердить правильное падение**

Команда:

```powershell
.\gradlew.bat testDevDebugUnitTest --tests "com.myhomechores.app.features.activities.EnglishNatureLessonTest"
```

Ожидается: `FAIL` из-за отсутствующих типов и функций модели урока.

- [ ] **Шаг 3. Реализовать словарь, этапы и переход после знакомства**

```kotlin
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
```

Словарь в `initialEnglishLessonState()` задаётся в порядке `sun`, `tree`, `flower`, `river` с переводами из спецификации.

- [ ] **Шаг 4. Добавить падающие тесты трёх этапов закрепления**

Тесты должны доказывать:

```kotlin
@Test fun correct_listen_answers_advance_to_pair_matching()
@Test fun four_correct_pairs_advance_to_recall()
@Test fun correct_recall_answers_create_four_item_final_queue()
@Test fun wrong_practice_answer_keeps_the_word_available_without_penalty()
```

Для `MATCH_PAIRS` использовать восемь стабильных карточек: `en:sun`, `ru:sun`, ..., `en:river`, `ru:river`. Совпадение определяется частью после `:`.

- [ ] **Шаг 5. Запустить тесты и подтвердить падение на отсутствующих переходах**

Команда та же. Ожидается: начальные тесты проходят, новые падают на неверном или неизменившемся `stage`.

- [ ] **Шаг 6. Реализовать минимальные переходы закрепления**

Правила reducer:

- `LISTEN_AND_CHOOSE`: правильный ответ увеличивает `currentIndex`; после четвёртого переводит в `MATCH_PAIRS`; неправильный устанавливает мягкий `feedback`, индекс не меняет.
- `MATCH_PAIRS`: две карточки с одинаковым word ID добавляют слово в `matchedWordIds`; после четырёх совпадений этап становится `RECALL`.
- `RECALL`: правильный ответ увеличивает индекс; после четвёртого создаёт `finalQueue = listOf("sun", "tree", "flower", "river")` и этап `FINAL_QUIZ`.
- `ContinueAfterFeedback` только закрывает подсказку и оставляет текущий вопрос доступным снова.

- [ ] **Шаг 7. Написать падающие тесты итоговой очереди**

```kotlin
@Test
fun wrong_final_answer_returns_word_to_end_of_queue() {
    val state = finalQuizState(queue = listOf("sun", "tree", "flower", "river"))
    val next = reduceEnglishLesson(state, EnglishLessonEvent.Answered("tree"))

    assertEquals(listOf("tree", "flower", "river", "sun"), next.finalQueue)
    assertTrue(next.correctFinalWordIds.isEmpty())
}

@Test
fun lesson_completes_only_after_all_four_words_are_correct() {
    var state = finalQuizState(queue = listOf("sun", "tree", "flower", "river"))
    listOf("sun", "tree", "flower", "river").forEach {
        state = reduceEnglishLesson(state, EnglishLessonEvent.Answered(it))
    }

    assertEquals(EnglishLessonStage.COMPLETED, state.stage)
    assertEquals(4, state.correctFinalWordIds.size)
}
```

- [ ] **Шаг 8. Реализовать итоговую очередь и повтор урока**

Правильный ответ удаляет первый элемент очереди и добавляет ID в `correctFinalWordIds`. Неправильный удаляет первый элемент, добавляет его в конец и показывает правильный ответ. `Restart` возвращает `initialEnglishLessonState()`.

- [ ] **Шаг 9. Запустить unit-тесты модели**

Ожидается: все тесты `EnglishNatureLessonTest` проходят.

- [ ] **Шаг 10. Сделать коммит модели**

```powershell
git add app/src/main/java/com/myhomechores/app/features/activities/EnglishNatureLesson.kt app/src/test/java/com/myhomechores/app/features/activities/EnglishNatureLessonTest.kt
git commit -m "feat: add English nature lesson state machine"
```

---

### Задача 2. Атомарная ежедневная награда в Room

**Файлы:**

- Изменить: `app/src/main/java/com/myhomechores/app/data/AppRepository.kt`
- Изменить: `app/src/main/java/com/myhomechores/app/data/local/Daos.kt`
- Изменить: `app/src/main/java/com/myhomechores/app/data/RoomAppRepository.kt`
- Изменить тест: `app/src/androidTest/java/com/myhomechores/app/data/local/RoomRepositoryTest.kt`
- Изменить тестовые fake-репозитории: `ChildConnectionViewModelTest.kt`, `InviteCodeViewModelTest.kt`

**Интерфейсы:**

- Добавляет `fun observeActivityRewardStars(childId: String): Flow<Int>`.
- Добавляет `suspend fun claimDailyActivityReward(childId: String, activityId: String, date: LocalDate, stars: Int): Boolean`.
- `Boolean` равен `true` только если запись создана впервые.
- Использует существующую `RewardEntity`; миграция схемы не требуется.

- [ ] **Шаг 1. Написать падающий Room-тест одной награды в день**

```kotlin
@Test
fun activityRewardIsGrantedOncePerChildActivityAndDay() = runBlocking {
    val date = LocalDate.of(2026, 8, 13)

    assertTrue(repository.claimDailyActivityReward("child-1", "english-nature", date, 5))
    assertFalse(repository.claimDailyActivityReward("child-1", "english-nature", date, 5))
    assertEquals(5, repository.observeActivityRewardStars("child-1").first())

    assertTrue(repository.claimDailyActivityReward("child-1", "english-nature", date.plusDays(1), 5))
    assertEquals(10, repository.observeActivityRewardStars("child-1").first())
}
```

- [ ] **Шаг 2. Запустить instrumentation-тест и подтвердить падение**

```powershell
.\gradlew.bat connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.myhomechores.app.data.local.RoomRepositoryTest
```

Ожидается: ошибка компиляции из-за отсутствующих методов.

- [ ] **Шаг 3. Добавить методы DAO**

```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertIfMissing(reward: RewardEntity): Long

@Query("SELECT COALESCE(SUM(stars), 0) FROM rewards WHERE childId = :childId")
fun observeTotalStars(childId: String): Flow<Int>
```

- [ ] **Шаг 4. Реализовать репозиторий в одной Room-транзакции**

Использовать стабильный первичный ключ:

```kotlin
val rewardKey = "$childId:activity:$activityId:$date"
val rewardId = UUID.nameUUIDFromBytes(rewardKey.toByteArray()).toString()
val inserted = database.rewardDao().insertIfMissing(
    RewardEntity(
        id = rewardId,
        childId = childId,
        completionId = "$activityId:$date",
        stars = stars,
        fragments = 0,
        updatedAt = System.currentTimeMillis(),
    ),
)
return inserted != -1L
```

Добавить методы с нулевым результатом в `NoOpAppRepository` и тестовые fake-репозитории, чтобы все варианты сборки компилировались.

- [ ] **Шаг 5. Запустить Room-тест и все unit-тесты**

Ожидается: награда в тот же день не дублируется, на следующий день сумма становится 10.

- [ ] **Шаг 6. Сделать коммит хранения награды**

```powershell
git add app/src/main/java/com/myhomechores/app/data app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldViewModel.kt app/src/androidTest/java/com/myhomechores/app/data/local/RoomRepositoryTest.kt app/src/test/java/com/myhomechores/app/features
git commit -m "feat: persist daily activity rewards"
```

---

### Задача 3. Каталог «Попробуй» и сетка 2×3

**Файлы:**

- Создать: `app/src/main/java/com/myhomechores/app/features/activities/ActivityCatalog.kt`
- Создать: `app/src/main/java/com/myhomechores/app/features/activities/TryActivitiesScreen.kt`
- Создать тест: `app/src/androidTest/java/com/myhomechores/app/features/activities/TryActivitiesScreenTest.kt`

**Интерфейсы:**

- `ActivityCatalogItem(id, title, marker, containerColor, available)`.
- `TryActivitiesContent(onEnglishClick: () -> Unit)`.
- Доступен только элемент с ID `english`; остальные показывают snackbar/диалог «Раздел скоро появится».

- [ ] **Шаг 1. Написать падающий Compose-тест каталога**

```kotlin
@Test
fun catalogShowsSixCardsAndComingSoonForUnavailableCard() {
    composeRule.setContent { MyHomeChoresTheme { TryActivitiesContent(onEnglishClick = {}) } }

    listOf("Английский", "Математика", "Медитация", "Дыхание", "Природа", "Обо мне")
        .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }

    composeRule.onNodeWithText("Математика").performClick()
    composeRule.onNodeWithText("Раздел скоро появится").assertIsDisplayed()
}
```

- [ ] **Шаг 2. Запустить тест и подтвердить падение из-за отсутствующего экрана**

- [ ] **Шаг 3. Реализовать фиксированный каталог и адаптивную сетку**

Использовать `LazyVerticalGrid(columns = GridCells.Fixed(2))`, отступы по 12–16 dp и карточки одинаковой высоты не менее 132 dp. Значки выполнить кодовыми векторными/Canvas-маркерами в едином стиле, без emoji. Карточка имеет `Card(onClick = ...)`, семантическое имя и минимальную высоту области нажатия 48 dp.

- [ ] **Шаг 4. Реализовать сообщение заглушки без пустого маршрута**

Хранить `comingSoonTitle: String?` через `rememberSaveable`; при нажатии недоступной карточки подставлять значение `comingSoonTitle` в `AlertDialog`: например, для математики показывать `«Математика»: раздел скоро появится`. Диалог содержит кнопку «Хорошо».

- [ ] **Шаг 5. Запустить Compose-тест и проверить сетку на API 34 эмуляторе**

Ожидается: все шесть карточек находятся, «Математика» показывает сообщение, переход на новый экран не происходит.

- [ ] **Шаг 6. Сделать коммит каталога**

```powershell
git add app/src/main/java/com/myhomechores/app/features/activities/ActivityCatalog.kt app/src/main/java/com/myhomechores/app/features/activities/TryActivitiesScreen.kt app/src/androidTest/java/com/myhomechores/app/features/activities/TryActivitiesScreenTest.kt
git commit -m "feat: add Try activity catalog"
```

---

### Задача 4. Системное произношение Android

**Файлы:**

- Создать: `app/src/main/java/com/myhomechores/app/features/activities/WordSpeaker.kt`
- Изменить: `app/src/main/AndroidManifest.xml`
- Создать тестовую реализацию в `EnglishNatureLessonScreenTest.kt`

**Интерфейсы:**

- `WordSpeaker` предоставляет `val availability: StateFlow<SpeechAvailability>`, `fun speak(word: String)` и `fun close()`.
- `AndroidWordSpeaker(context)` использует `TextToSpeech`, `Locale.US` и `QUEUE_FLUSH`.
- UI получает интерфейс через параметр, чтобы Compose-тест использовал `FakeWordSpeaker` без настоящего звука.

- [ ] **Шаг 1. Написать падающий тест контракта через fake speaker**

В Compose-тесте открыть экран знакомства, нажать «Послушать» и проверить:

```kotlin
assertEquals(listOf("sun"), fakeSpeaker.spokenWords)
```

Отдельный тест с `SpeechAvailability.Unavailable` должен увидеть текст «Произношение сейчас недоступно. Можно продолжить без звука» и активную кнопку «Повторил(а)».

- [ ] **Шаг 2. Запустить тест и подтвердить падение из-за отсутствующего контракта**

- [ ] **Шаг 3. Реализовать `WordSpeaker` и Android-адаптер**

```kotlin
interface WordSpeaker : AutoCloseable {
    val availability: StateFlow<SpeechAvailability>
    fun speak(word: String)
}

enum class SpeechAvailability { Initializing, Available, Unavailable }
```

В `onInit` вызвать `isLanguageAvailable(Locale.US)`, затем `setLanguage(Locale.US)`. Любой код `LANG_MISSING_DATA`, `LANG_NOT_SUPPORTED` или `ERROR` переводит состояние в `Unavailable`. `close()` вызывает `textToSpeech.shutdown()` ровно один раз.

- [ ] **Шаг 4. Добавить TTS query в manifest**

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.TTS_SERVICE" />
    </intent>
</queries>
```

- [ ] **Шаг 5. Запустить тест с fake speaker и собрать devDebug**

Ожидается: тест не зависит от голоса эмулятора; `assembleDevDebug` проходит.

- [ ] **Шаг 6. Сделать коммит TTS-адаптера**

```powershell
git add app/src/main/java/com/myhomechores/app/features/activities/WordSpeaker.kt app/src/main/AndroidManifest.xml app/src/androidTest/java/com/myhomechores/app/features/activities/EnglishNatureLessonScreenTest.kt
git commit -m "feat: add Android word pronunciation"
```

---

### Задача 5. Экран английского урока и векторные изображения

**Файлы:**

- Создать: `app/src/main/java/com/myhomechores/app/features/activities/NatureWordIllustration.kt`
- Создать: `app/src/main/java/com/myhomechores/app/features/activities/EnglishNatureLessonScreen.kt`
- Дополнить тест: `app/src/androidTest/java/com/myhomechores/app/features/activities/EnglishNatureLessonScreenTest.kt`

**Интерфейсы:**

- `EnglishNatureLessonScreen(wordSpeaker: WordSpeaker, rewardStatus: ActivityRewardStatus, onBack: () -> Unit, onCompleted: () -> Unit, onRestart: () -> Unit)`.
- `onCompleted()` вызывается один раз при первом переходе в `COMPLETED`, а не при каждой recomposition.
- `NatureWordIllustration(wordId, modifier)` рисует `sun`, `tree`, `flower`, `river` через Compose `Canvas`, сохраняя одинаковый размер и понятное `contentDescription`.

- [ ] **Шаг 1. Написать падающий Compose-тест знакомства**

Проверить последовательность:

```kotlin
composeRule.onNodeWithText("sun").assertIsDisplayed()
composeRule.onNodeWithText("солнце").assertIsDisplayed()
composeRule.onNodeWithText("Послушать").performClick()
composeRule.onNodeWithText("Повторил(а)").performClick()
composeRule.onNodeWithText("tree").assertIsDisplayed()
```

- [ ] **Шаг 2. Реализовать экран знакомства и одинаковую область иллюстраций**

Иллюстрация занимает фиксированную карточку с `aspectRatio(1f)`; смена слова не меняет внешние размеры. Показать прогресс `1 из 4`, слово, перевод, «Послушать», подсказку «Теперь повтори слово вслух» и «Повторил(а)».

- [ ] **Шаг 3. Написать падающий тест закрепления и мягкой ошибки**

Тест должен пройти четыре знакомства, выбрать неверный вариант в «Послушай и выбери», увидеть `«Почти! Правильный ответ: …»`, закрыть подсказку и снова увидеть тот же вопрос.

- [ ] **Шаг 4. Реализовать три UI этапа закрепления**

- `LISTEN_AND_CHOOSE`: четыре квадратные карточки-иллюстрации; слово автоматически не произносится, но есть повторная кнопка звука.
- `MATCH_PAIRS`: адаптивная сетка 2 столбца из восьми текстовых карточек; совпавшая пара становится бледной и недоступной.
- `RECALL`: крупная иллюстрация и четыре кнопки английских слов.

Каждый ответ передаётся reducer; UI не дублирует правила переходов.

- [ ] **Шаг 5. Написать падающий тест итоговой очереди**

Сценарий теста: пройти подготовленные этапы через reducer state, дать неверный ответ на `sun`, затем правильные ответы; проверить, что экран успеха не появляется до повторного правильного `sun`.

- [ ] **Шаг 6. Реализовать итоговую проверку и экран успеха**

На итоговом вопросе показывать изображение и четыре английских варианта. Экран успеха содержит:

- «Все четыре слова выучены!»;
- текст `«+5 звёзд»` либо `«Сегодня награда уже получена — повторение всё равно полезно»`;
- кнопки «В раздел “Попробуй”» и «Повторить урок».

`LaunchedEffect(state.stage)` вызывает `onCompleted` только при `COMPLETED`; родительский экран/основная игра не получают событий. `AndroidWordSpeaker` создаётся через `remember(context)`, а `DisposableEffect` обязательно вызывает `close()` при выходе с экрана.

- [ ] **Шаг 7. Запустить unit- и Compose-тесты урока**

Ожидается: все этапы доступны, UI не завершает урок раньше reducer, fake speaker получает слова.

- [ ] **Шаг 8. Сделать коммит урока**

```powershell
git add app/src/main/java/com/myhomechores/app/features/activities app/src/androidTest/java/com/myhomechores/app/features/activities
git commit -m "feat: build English nature activity"
```

---

### Задача 6. Интеграция в «Мои дела» и отображение награды

**Файлы:**

- Изменить: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldViewModel.kt`
- Изменить: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt`
- Дополнить: `app/src/androidTest/java/com/myhomechores/app/ScaffoldScreenTest.kt`
- Создать unit-тест: `app/src/test/java/com/myhomechores/app/features/scaffold/ActivityRewardViewModelTest.kt`

**Интерфейсы:**

- `ScaffoldUiState` получает `childId: String = LOCAL_CHILD_ID`, `activityRewardStars: Int = 0` и `activityRewardStatus: ActivityRewardStatus = ActivityRewardStatus.NotRequested`.
- `ActivityRewardStatus` имеет значения `NotRequested`, `Granting`, `Granted`, `AlreadyClaimed`.
- `ScaffoldViewModel.beginActivity()` сбрасывает статус в `NotRequested`.
- `ScaffoldViewModel.completeActivity(activityId: String, date: LocalDate = LocalDate.now(), stars: Int = 5)` вызывает репозиторий и меняет статус `Granting → Granted` либо `Granting → AlreadyClaimed`.
- `ChildModeScreen` получает `activityRewardStars` и callback завершения урока.

- [ ] **Шаг 1. Написать падающий ViewModel-тест выдачи награды**

Fake repository возвращает поток суммы звёзд и фиксирует вызовы. Проверить:

```kotlin
viewModel.completeActivity("english-nature", LocalDate.of(2026, 8, 13), 5)
advanceUntilIdle()

assertEquals(5, viewModel.state.value.activityRewardStars)
assertEquals(ActivityRewardStatus.Granted, viewModel.state.value.activityRewardStatus)
```

После `beginActivity()` повторный вызов с той же датой возвращает `false`, сумма остаётся 5, а статус становится `AlreadyClaimed`.

- [ ] **Шаг 2. Запустить unit-тест и подтвердить падение**

- [ ] **Шаг 3. Реализовать поток награды во ViewModel**

При изменении `repository.observeChild()` выбирать `profile.id`, а при отсутствии профиля — константу `local-child`. Через `collectLatest` наблюдать `observeActivityRewardStars(childId)`, чтобы старая подписка отменялась при перепривязке ребёнка.

- [ ] **Шаг 4. Написать падающий Compose-тест фильтра «Попробуй»**

Тест открывает режим ребёнка с подготовленным профилем, переходит в «Дела», прокручивает строку до «Попробуй», нажимает и проверяет карточки «Английский» и «Математика». Одновременно `«Обязательные дела»` и `«Дела на выбор»` не должны отображаться в выбранной категории.

- [ ] **Шаг 5. Интегрировать пятую кнопку и условный контент**

В `ChoresTab`:

```kotlin
val categories = listOf("Все", "Дом", "Учёба", "Здоровье", "Попробуй")
```

После строки фильтров использовать две взаимоисключающие ветки:

- `category == "Попробуй"` — `TryActivitiesContent` и вложенный `EnglishNatureLessonScreen`;
- иначе — существующие обязательные дела, карточка мини-игры, дела на выбор и помощник без изменения фильтрации.

Не добавлять интерактивные задания в список `chores`.

- [ ] **Шаг 6. Подключить отображаемые звёзды и дневную выдачу**

Отображаемый баланс равен текущему локальному балансу экрана плюс `activityRewardStars`. При открытии «Английского» вызвать `ScaffoldViewModel.beginActivity()`. После завершения урока вызвать `ScaffoldViewModel.completeActivity("english-nature", LocalDate.now(), 5)`. Экран успеха показывает индикатор при `Granting`, `«+5 звёзд»` при `Granted` и `«Сегодня награда уже получена — повторение всё равно полезно»` при `AlreadyClaimed`.

- [ ] **Шаг 7. Запустить ViewModel, Compose и существующие тесты**

```powershell
.\gradlew.bat testDevDebugUnitTest
.\gradlew.bat connectedDevDebugAndroidTest
```

Ожидается: новые тесты проходят; старые сценарии родителя, ребёнка, авторизации и подключения не регрессируют.

- [ ] **Шаг 8. Сделать интеграционный коммит**

```powershell
git add app/src/main/java/com/myhomechores/app/features/scaffold app/src/test/java/com/myhomechores/app/features/scaffold app/src/androidTest/java/com/myhomechores/app/ScaffoldScreenTest.kt
git commit -m "feat: integrate Try activities into child chores"
```

---

### Задача 7. Финальная проверка на симуляторе

**Файлы:**

- При необходимости изменить только файлы, в которых проверка обнаружит конкретный дефект.
- Обновить: `docs/superpowers/specs/2026-08-13-try-interactive-activities-design.md` только если фактическое утверждённое поведение пришлось согласованно изменить.

- [ ] **Шаг 1. Запустить полный локальный набор проверок**

```powershell
.\gradlew.bat testDevDebugUnitTest connectedDevDebugAndroidTest lintDevDebug assembleDevDebug
```

Ожидается: `BUILD SUCCESSFUL`, без новых lint-ошибок и упавших тестов.

- [ ] **Шаг 2. Проверить сценарий на эмуляторе Pixel 6 API 34**

Ручной чек-лист:

1. «Попробуй» доступна горизонтальной прокруткой после «Здоровье».
2. В «Попробуй» видны шесть карточек 2×3; нижняя навигация не перекрывает последнюю строку.
3. Пять заглушек показывают сообщение и остаются на том же экране.
4. «Английский» открывает четыре слова темы «Природа».
5. «Послушать» произносит английское слово; экран не прыгает при смене слова.
6. Ошибка в каждом упражнении сопровождается спокойной подсказкой.
7. Ошибка в тесте возвращает слово; успех появляется только после четырёх правильных слов.
8. Первый успех добавляет 5 звёзд, повтор в тот же день — нет.
9. После полного закрытия и повторного открытия приложения награда дня не начисляется заново.
10. Процент обязательных дел и доступ к игре не изменяются.

- [ ] **Шаг 3. Проверить небольшой экран и увеличенный системный шрифт**

На эмуляторе установить размер шрифта «Крупный» и проверить: фильтр прокручивается, карточки и ответы не обрезаются, экран урока прокручивается, кнопки остаются доступными.

- [ ] **Шаг 4. Просмотреть итоговый diff и секреты**

```powershell
git diff --check
git status --short
git diff --stat HEAD~6..HEAD
git grep -n -E "sb_(publishable|secret)|SUPABASE_PUBLISHABLE_KEY=" -- . ":(exclude)local.properties"
```

Ожидается: только осознанные файлы, нет ключей и локальных настроек.

- [ ] **Шаг 5. Сделать финальный исправляющий коммит только при наличии изменений**

```powershell
git add app/src/main/AndroidManifest.xml app/src/main/java/com/myhomechores/app/data/AppRepository.kt app/src/main/java/com/myhomechores/app/data/RoomAppRepository.kt app/src/main/java/com/myhomechores/app/data/local/Daos.kt app/src/main/java/com/myhomechores/app/features/activities app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldViewModel.kt app/src/test/java/com/myhomechores/app/features app/src/androidTest/java/com/myhomechores/app
git commit -m "fix: polish Try activity flow"
```

- [ ] **Шаг 6. Показать результат пользователю до push**

Открыть готовую версию на симуляторе и попросить пользователя проверить внешний вид и прохождение урока. `git push` выполнять только после подтверждения пользователя.
