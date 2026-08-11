package com.myhomechores.app.features.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myhomechores.app.BuildConfig
import com.myhomechores.app.data.AppRepository
import com.myhomechores.app.R
import com.myhomechores.app.core.AppConfig
import com.myhomechores.app.domain.model.AppRole
import com.myhomechores.app.ui.theme.MyHomeChoresTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class ChildTab { ROOM, CHORES, GAME, SHOP, PROFILE }

private enum class ParentTab { OVERVIEW, CHORES, GAME, CHILDREN, PROFILE }

private enum class Hero(
    val id: String,
    val displayName: String,
    val roomOwnerName: String,
    val description: String,
    val imageRes: Int,
    val roomImageRes: Int,
) {
    BOY("boy", "Том", "Тома", "Любит исследовать и пробовать новое", R.drawable.dragon_boy, R.drawable.room_tom),
    GIRL("girl", "Лили", "Лили", "Поддерживает и замечает твои успехи", R.drawable.dragon_girl, R.drawable.room_lily),
}

private data class Chore(
    val id: String,
    val title: String,
    val category: String,
    val reward: Int,
    val hint: String,
    val color: Color,
    val required: Boolean,
)

private val chores = listOf(
    Chore("teeth", "Почистить зубы", "Здоровье", 2, "Утром и вечером", Color(0xFFD9F5EA), required = true),
    Chore("bed", "Заправить кровать", "Дом", 2, "Начать день с порядка", Color(0xFFDCEBFF), required = true),
    Chore("homework", "Сделать уроки", "Учёба", 5, "Проверить задания в дневнике", Color(0xFFE8DEFF), required = true),
    Chore("reading", "Почитать 15 минут", "Учёба", 3, "Выбери любую интересную книгу", Color(0xFFE8DEFF), required = false),
    Chore("walk", "Погулять", "Здоровье", 4, "Минимум 30 минут на свежем воздухе", Color(0xFFFFE8BC), required = false),
    Chore("table", "Помочь накрыть на стол", "Дом", 3, "Небольшая помощь семье", Color(0xFFDCEBFF), required = false),
)

private val parentDefaultChores = listOf(
    Chore("parent_children", "Проверить дела детей", "Семья", 3, "Посмотреть прогресс и поддержать ребёнка", Color(0xFFE8DEFF), required = true),
    Chore("parent_work", "Сделать главную рабочую задачу", "Работа", 5, "Выбери один важный шаг на сегодня", Color(0xFFDCEBFF), required = true),
    Chore("parent_home", "Навести порядок дома", "Дом", 3, "10 минут маленьких улучшений", Color(0xFFFFE8BC), required = false),
    Chore("parent_walk", "Сделать перерыв и прогуляться", "Здоровье", 3, "Немного движения помогает восстановить силы", Color(0xFFD9F5EA), required = false),
)

@Composable
fun ScaffoldScreen(
    environment: String,
    modifier: Modifier = Modifier,
    repository: AppRepository = NoOpAppRepository,
) {
    val scaffoldViewModel: ScaffoldViewModel = viewModel(factory = ScaffoldViewModel.Factory(repository))
    val state by scaffoldViewModel.state.collectAsState()
    val selectedRole = state.selectedRole
    val childCompletedIds = state.childCompletedIds
    var childName by rememberSaveable { mutableStateOf("Алекс") }
    var parentName by rememberSaveable { mutableStateOf("Родитель") }

    when (selectedRole) {
        null -> ModeSelectionScreen(
            environment = environment,
            modifier = modifier,
            onChildClick = { scaffoldViewModel.selectRole(AppRole.CHILD) },
            onParentClick = { scaffoldViewModel.selectRole(AppRole.PARENT) },
        )

        AppRole.CHILD -> ChildModeScreen(
            modifier = modifier,
            completedIds = childCompletedIds,
            onCompletedIdsChange = scaffoldViewModel::updateChildCompletedIds,
            onChildNameChange = { childName = it; scaffoldViewModel.updateChildName(it) },
            onBack = { scaffoldViewModel.selectRole(null) },
        )
        AppRole.PARENT -> ParentModeScreen(
            modifier = modifier,
            parentName = parentName,
            onParentNameChange = { parentName = it; scaffoldViewModel.updateParentName(it) },
            childName = childName,
            childCompletedIds = childCompletedIds,
            onChildCompletedIdsChange = scaffoldViewModel::updateChildCompletedIds,
            onBack = { scaffoldViewModel.selectRole(null) },
        )
    }
}

@Composable
private fun ModeSelectionScreen(
    environment: String,
    modifier: Modifier = Modifier,
    onChildClick: () -> Unit,
    onParentClick: () -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "Логотип приложения Мой путь",
                modifier = Modifier.size(150.dp).clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(18.dp))
            Text(AppConfig.WORKING_NAME, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Маленькие шаги — большие победы", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(30.dp))
            ModeCard("Режим ребёнка", "Дела, звёзды, помощник и награды", "Р", onChildClick)
            Spacer(Modifier.height(12.dp))
            ModeCard("Режим родителя", "Настройка дел и подтверждение результатов", "В", onParentClick)
            Spacer(Modifier.height(26.dp))
            Text("Каркас 0.2 · $environment", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModeCard(title: String, description: String, marker: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(marker, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ChildModeScreen(
    modifier: Modifier = Modifier,
    completedIds: Set<String>,
    onCompletedIdsChange: (Set<String>) -> Unit,
    onChildNameChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedHeroId by rememberSaveable { mutableStateOf<String?>(null) }
    var childName by rememberSaveable { mutableStateOf("") }
    val selectedHero = Hero.values().firstOrNull { it.id == selectedHeroId }
    if (selectedHero == null) {
        HeroSelectionScreen(modifier = modifier, onBack = onBack) { selectedHeroId = it.id }
        return
    }
    if (childName.isBlank()) {
        ChildProfileSetupScreen(hero = selectedHero, modifier = modifier, onBack = onBack) { name ->
            childName = name
            onChildNameChange(name)
        }
        return
    }

    var tab by rememberSaveable { mutableStateOf(ChildTab.ROOM) }
    var selectedOptionalIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var stars by rememberSaveable { mutableStateOf(24) }
    var category by rememberSaveable { mutableStateOf("Все") }
    val requiredChores = chores.filter { it.required }
    val requiredDone = requiredChores.count { completedIds.contains(it.id) }
    val gameProgress = if (requiredChores.isEmpty()) 0f else requiredDone.toFloat() / requiredChores.size

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { ChildBottomBar(tab = tab, gameProgress = gameProgress, onTabSelected = { tab = it }) },
    ) { padding ->
        when (tab) {
            ChildTab.ROOM -> RoomTab(padding, stars, selectedHero, childName, onBack)
            ChildTab.CHORES -> ChoresTab(
                padding = padding,
                stars = stars,
                completedIds = completedIds,
                selectedOptionalIds = selectedOptionalIds,
                category = category,
                hero = selectedHero,
                childName = childName,
                onBack = onBack,
                onCategoryChange = { category = it },
                onOptionalSelect = { chore ->
                    selectedOptionalIds = if (selectedOptionalIds.contains(chore.id)) {
                        selectedOptionalIds - chore.id
                    } else {
                        selectedOptionalIds + chore.id
                    }
                },
            ) { chore ->
                if (completedIds.contains(chore.id)) {
                    onCompletedIdsChange(completedIds - chore.id)
                    stars = (stars - chore.reward).coerceAtLeast(0)
                } else {
                    onCompletedIdsChange(completedIds + chore.id)
                    stars += chore.reward
                }
            }
            ChildTab.GAME -> GameTab(padding = padding, stars = stars, progress = gameProgress, hero = selectedHero, childName = childName)
            ChildTab.SHOP -> ShopTab(padding, stars, selectedHero, childName) { price -> if (stars >= price) stars -= price }
            ChildTab.PROFILE -> ProfileTab(
                padding = padding,
                stars = stars,
                completed = completedIds.size,
                hero = selectedHero,
                childName = childName,
                onNameChange = { newName ->
                    childName = newName
                    onChildNameChange(newName)
                },
            )
        }
    }
}

@Composable
private fun ChildProfileSetupScreen(
    hero: Hero,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.align(Alignment.Start).height(44.dp)) { Text("Назад") }
            Spacer(Modifier.height(18.dp))
            Image(
                painter = painterResource(hero.imageRes),
                contentDescription = hero.displayName,
                modifier = Modifier.size(190.dp).clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(16.dp))
            Text("Создадим твой личный кабинет", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Как тебя будут называть в приложении?", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(20) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Твоё имя или псевдоним") },
                placeholder = { Text("Например, Саша") },
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onComplete(name.trim()) }, enabled = name.trim().length >= 2, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Войти в комнату") }
        }
    }
}

@Composable
private fun HeroSelectionScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onHeroSelected: (Hero) -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.align(Alignment.Start).height(44.dp)) { Text("Назад") }
            Spacer(Modifier.height(24.dp))
            Text("Выбери помощника", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Твой дракончик будет рядом, когда ты выполняешь дела.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            HeroChoiceCard(Hero.BOY, onHeroSelected)
            Spacer(Modifier.height(14.dp))
            HeroChoiceCard(Hero.GIRL, onHeroSelected)
        }
    }
}

@Composable
private fun HeroChoiceCard(hero: Hero, onHeroSelected: (Hero) -> Unit) {
    Card(
        onClick = { onHeroSelected(hero) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Image(
                painter = painterResource(hero.imageRes),
                contentDescription = hero.displayName,
                modifier = Modifier.size(138.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f)) {
                Text(hero.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(hero.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onHeroSelected(hero) }, modifier = Modifier.height(48.dp)) { Text("Выбрать") }
            }
        }
    }
}

@Composable
private fun ChildTopBar(
    title: String,
    stars: Int,
    onBack: (() -> Unit)? = null,
    hero: Hero? = null,
    childName: String? = null,
    subtitle: String? = null,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            OutlinedButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(44.dp)) { Text("Назад") }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle ?: if (childName.isNullOrBlank()) "Твой маленький мир" else "Привет, $childName!", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        if (hero != null) {
            Image(painter = painterResource(hero.imageRes), contentDescription = hero.displayName, modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(8.dp))
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFFE3A1))
                .clearAndSetSemantics { contentDescription = "$stars звёзд" }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("$stars", fontWeight = FontWeight.ExtraBold, color = Color(0xFF6E4A00))
            StarIcon(Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ChoresTab(
    padding: PaddingValues,
    stars: Int,
    completedIds: Set<String>,
    selectedOptionalIds: Set<String>,
    category: String,
    hero: Hero,
    childName: String,
    onBack: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onOptionalSelect: (Chore) -> Unit,
    onToggle: (Chore) -> Unit,
) {
    val categories = listOf("Все", "Дом", "Учёба", "Здоровье")
    val requiredChores = chores.filter { it.required }
    val optionalChores = chores.filter { !it.required }
    val visibleRequired = requiredChores.filter { category == "Все" || it.category == category }
    val visibleOptional = optionalChores.filter { category == "Все" || it.category == category }
    val requiredDone = requiredChores.count { completedIds.contains(it.id) }
    val allRequiredDone = requiredDone == requiredChores.size
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ChildTopBar("Мои дела", stars, onBack, hero, childName) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Дела на сегодня", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$requiredDone из ${requiredChores.size} обязательных дел", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${requiredDone * 100 / requiredChores.size}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { requiredDone.toFloat() / requiredChores.size }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)))
                Spacer(Modifier.height(14.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { item -> FilterChip(selected = category == item, onClick = { onCategoryChange(item) }, label = { Text(item) }) }
                }
            }
        }
        item { SectionHeader("Обязательные дела") }
        items(visibleRequired, key = { it.id }) { chore -> ChoreCard(chore, completedIds.contains(chore.id), onToggle) }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (allRequiredDone) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (allRequiredDone) "Мини-игра открыта" else "Мини-игра откроется после обязательных дел", fontWeight = FontWeight.Bold)
                    Text(if (allRequiredDone) "Ты выполнил основу дня. Теперь можно играть и отдыхать." else "Это не штраф и не соревнование — просто спокойный план на сегодня.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Дела на выбор", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Выбирай любое количество дел, если хочется получить дополнительные звёзды.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(visibleOptional, key = { it.id }) { chore ->
            OptionalChoreCard(chore, completedIds.contains(chore.id), selectedOptionalIds.contains(chore.id), true, onOptionalSelect, onToggle)
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Text("П", color = Color.White, fontWeight = FontWeight.Bold) }
                    Column(Modifier.weight(1f)) {
                        Text("Помощник рядом", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(if (requiredDone == 0) "Начни с одного простого дела — это уже победа!" else "Отличный темп! Следующее дело можно сделать, когда будет удобно.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoreCard(chore: Chore, done: Boolean, onToggle: (Chore) -> Unit) {
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = chore.color)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskCheckbox(done = done, label = chore.title, onToggle = { onToggle(chore) })
            Column(Modifier.weight(1f)) {
                Text(
                    chore.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    chore.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "+${chore.reward} звёзд",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun OptionalChoreCard(
    chore: Chore,
    done: Boolean,
    selected: Boolean,
    canSelect: Boolean,
    onSelect: (Chore) -> Unit,
    onToggle: (Chore) -> Unit,
) {
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = chore.color)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskCheckbox(done = done, enabled = selected || done, label = chore.title, onToggle = { onToggle(chore) })
            Column(Modifier.weight(1f)) {
                Text(
                    chore.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    "+${chore.reward} звёзд · ${chore.hint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (done) {
                Text("Готово", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            } else if (selected) {
                Text("Выбрано", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else {
                OutlinedButton(onClick = { onSelect(chore) }, enabled = canSelect, modifier = Modifier.height(48.dp)) { Text("Выбрать") }
            }
        }
    }
}

@Composable
private fun TaskCheckbox(
    done: Boolean,
    label: String,
    enabled: Boolean = true,
    onToggle: () -> Unit,
) {
    val successColor = Color(0xFF2E9B57)
    val borderColor = when {
        done -> successColor
        enabled -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outline.copy(alpha = .35f)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = if (done) "$label, выполнено" else "$label, не выполнено" }
            .toggleable(
                value = done,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (done) successColor else Color.White.copy(alpha = .82f))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Canvas(Modifier.size(27.dp)) {
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(size.width * .17f, size.height * .52f),
                    end = androidx.compose.ui.geometry.Offset(size.width * .42f, size.height * .77f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(size.width * .42f, size.height * .77f),
                    end = androidx.compose.ui.geometry.Offset(size.width * .86f, size.height * .24f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun RoomTab(padding: PaddingValues, stars: Int, hero: Hero, childName: String, onBack: () -> Unit) {
    var eyesClosed by remember(hero) { mutableStateOf(false) }

    LaunchedEffect(hero) {
        while (true) {
            delay(1_500)
            eyesClosed = true
            delay(120)
            eyesClosed = false
        }
    }

    val displayedRoomImage = when {
        !eyesClosed -> hero.roomImageRes
        hero == Hero.BOY -> R.drawable.room_tom_blink
        else -> R.drawable.room_lily_blink
    }

    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { RoomTopBar(stars = stars, hero = hero, onBack = onBack) }
        item {
            Card(Modifier.padding(horizontal = 12.dp).fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6DFFF))) {
                Column(Modifier.padding(8.dp)) {
                    Text("Комната ${hero.roomOwnerName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(638f / 663f).clip(RoundedCornerShape(22.dp))) {
                        Image(
                            painter = painterResource(displayedRoomImage),
                            contentDescription = "Комната ${hero.roomOwnerName}: ${hero.displayName} в уютной комнате",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )

                        val displayedName = childName.trim().ifBlank { "друг" }
                        val bubbleWidthFraction = when {
                            displayedName.length <= 7 -> .22f
                            displayedName.length <= 12 -> .28f
                            else -> .36f
                        }
                        // The right edge and the tail stay next to the dragon while the
                        // bubble grows only to the left for a long child name.
                        val bubbleRightFraction = .408f
                        val bubbleXFraction = (bubbleRightFraction - bubbleWidthFraction).coerceAtLeast(.035f)
                        val nameStyle = when {
                            displayedName.length <= 7 -> MaterialTheme.typography.labelLarge
                            displayedName.length <= 12 -> MaterialTheme.typography.labelMedium
                            else -> MaterialTheme.typography.labelSmall
                        }
                        val bubbleColor = Color(0xFFFFF8EE)
                        val roomWidth = maxWidth

                        Box(
                            modifier = Modifier
                                .offset(x = maxWidth * bubbleXFraction, y = maxHeight * .248f)
                                .width(maxWidth * bubbleWidthFraction)
                                .height(maxHeight * .18f)
                                .clearAndSetSemantics {
                                    contentDescription = "Дракон говорит: Привет, $displayedName!"
                                },
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val bodyBottom = size.height * .82f
                                val radius = 18.dp.toPx().coerceAtMost(bodyBottom * .28f)
                                val tailRootRight = size.width - (roomWidth * .045f).toPx()
                                val tailTipX = size.width - (roomWidth * .025f).toPx()
                                val tailRootLeft = size.width - (roomWidth * .09f).toPx()
                                val tailTipY = size.height * .93f
                                val bubble = Path().apply {
                                    moveTo(radius, 0f)
                                    lineTo(size.width - radius, 0f)
                                    quadraticBezierTo(size.width, 0f, size.width, radius)
                                    lineTo(size.width, bodyBottom - radius)
                                    quadraticBezierTo(size.width, bodyBottom, size.width - radius, bodyBottom)
                                    lineTo(tailRootRight, bodyBottom)
                                    cubicTo(
                                        tailRootRight,
                                        size.height * .86f,
                                        tailTipX - (roomWidth * .008f).toPx(),
                                        tailTipY,
                                        tailTipX,
                                        tailTipY,
                                    )
                                    cubicTo(
                                        size.width - (roomWidth * .04f).toPx(),
                                        size.height * .91f,
                                        size.width - (roomWidth * .07f).toPx(),
                                        size.height * .86f,
                                        tailRootLeft,
                                        bodyBottom,
                                    )
                                    lineTo(radius, bodyBottom)
                                    quadraticBezierTo(0f, bodyBottom, 0f, bodyBottom - radius)
                                    lineTo(0f, radius)
                                    quadraticBezierTo(0f, 0f, radius, 0f)
                                    close()
                                }
                                drawPath(path = bubble, color = bubbleColor)
                                drawPath(
                                    path = bubble,
                                    color = Color(0xFF71687A),
                                    style = Stroke(width = 1.25.dp.toPx()),
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 8.dp, top = 5.dp, end = 8.dp, bottom = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = "Привет,",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                                Text(
                                    text = "$displayedName!",
                                    modifier = Modifier.offset(y = (-2).dp),
                                    style = nameStyle,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Продолжай выполнять дела, чтобы открыть новые предметы для комнаты.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RoomTopBar(stars: Int, hero: Hero, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.height(48.dp)) {
            Text("Назад")
        }
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(hero.imageRes),
            contentDescription = hero.displayName,
            modifier = Modifier.size(44.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFFFE3A1))
                .clearAndSetSemantics { contentDescription = "$stars звёзд" }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("$stars", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF6E4A00))
            StarIcon(Modifier.size(28.dp))
        }
    }
}

@Composable
private fun StarIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outerRadius = size.minDimension / 2f
        val innerRadius = outerRadius * .46f
        val path = Path()

        repeat(10) { index ->
            val radius = if (index % 2 == 0) outerRadius else innerRadius
            val angle = -PI / 2 + index * PI / 5
            val x = centerX + cos(angle).toFloat() * radius
            val y = centerY + sin(angle).toFloat() * radius
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = Color(0xFFFFB300))
    }
}

@Composable
private fun SectionHeader(text: String) { Text(text, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

@Composable
private fun ShopTab(padding: PaddingValues, stars: Int, hero: Hero, childName: String, onBuy: (Int) -> Unit) {
    val items = listOf("Драконья пицца" to 12, "Крутые очки" to 20, "Космическая лампа" to 30, "Мягкая подушка" to 16)
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ChildTopBar("Магазин", stars, hero = hero, childName = childName) }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEFF))) {
                Column(Modifier.padding(18.dp)) {
                    Text("Сюрприз дня", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Загляни после всех обязательных дел", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { }, modifier = Modifier.height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7653C9))) { Text("Посмотреть позже") }
                }
            }
        }
        item { SectionHeader("Награды для комнаты") }
        items(items.chunked(2)) { pair ->
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (name, price) -> ShopItem(name, price, stars >= price, onBuy, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShopItem(name: String, price: Int, canBuy: Boolean, onBuy: (Int) -> Unit, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFE9C4)), contentAlignment = Alignment.Center) { Text(name.take(1), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(8.dp))
            Text(name, fontWeight = FontWeight.Bold, maxLines = 2, minLines = 2)
            Text("$price звёзд", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Button(onClick = { onBuy(price) }, enabled = canBuy, modifier = Modifier.fillMaxWidth().height(44.dp), contentPadding = PaddingValues(0.dp)) { Text("Купить") }
        }
    }
}

@Composable
private fun GameTab(
    padding: PaddingValues,
    stars: Int,
    progress: Float,
    hero: Hero,
    childName: String,
) {
    var played by rememberSaveable { mutableStateOf(false) }
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
    val unlocked = progress >= 1f
    LazyColumn(
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ChildTopBar("Игра", stars, hero = hero, childName = childName) }
        item {
            Card(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEFF)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GameProgressIcon(
                        progress = progress,
                        modifier = Modifier.size(128.dp),
                        contentDescription = "Игра открыта на $percent процентов",
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        if (unlocked) "Игра открыта!" else "Открываем игру",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (unlocked) "Все обязательные дела выполнены. Пазл готов!"
                        else "Выполняй обязательные дела — золотой цвет постепенно заполнит значок игры.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                        color = Color(0xFFFFB300),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("$percent%", color = Color(0xFF6E4A00), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { played = true },
                        enabled = unlocked && !played,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(if (played) "Сегодня уже сыграли" else if (unlocked) "Собрать пазл" else "Пока закрыто")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(
    padding: PaddingValues,
    stars: Int,
    completed: Int,
    hero: Hero,
    childName: String,
    onNameChange: (String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ChildTopBar("Профиль", stars, hero = hero, childName = childName) }
        item {
            EditableNameCard(
                name = childName,
                subtitle = "Личный кабинет · ${hero.displayName}",
                imageRes = hero.imageRes,
                imageDescription = hero.displayName,
                containerColor = Color(0xFFD9F5EA),
                onNameChange = onNameChange,
            )
        }
        item { SectionHeader("Мои результаты") }
        item {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Выполнено", completed.toString(), Modifier.weight(1f))
                StatCard("Звёзды", stars.toString(), Modifier.weight(1f))
                StatCard("Серия", "3", Modifier.weight(1f))
            }
        }
        item { SectionHeader("Значки") }
        item { Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Badge("Старт", "1"); Badge("Знаток", "5"); Badge("Забота", "3") } }
    }
}

@Composable
private fun EditableNameCard(
    name: String,
    subtitle: String,
    imageRes: Int,
    imageDescription: String,
    containerColor: Color,
    onNameChange: (String) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(name) { mutableStateOf(name) }
    Card(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = imageDescription,
                    modifier = Modifier.size(72.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!editing) {
                    OutlinedButton(
                        onClick = {
                            draft = name
                            editing = true
                        },
                        modifier = Modifier.height(48.dp),
                    ) { Text("Изменить") }
                }
            }
            if (editing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(20) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Имя или псевдоним") },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            draft = name
                            editing = false
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text("Отмена") }
                    Button(
                        onClick = {
                            onNameChange(draft.trim())
                            editing = false
                        },
                        enabled = draft.trim().length >= 2,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text("Сохранить") }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) } } }

@Composable
private fun Badge(title: String, marker: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text(marker, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer) }; Spacer(Modifier.height(4.dp)); Text(title, style = MaterialTheme.typography.labelSmall) } }

@Composable
private fun ChildBottomBar(tab: ChildTab, gameProgress: Float, onTabSelected: (ChildTab) -> Unit) {
    Surface(shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            NavItem("Комната", "К", tab == ChildTab.ROOM, Modifier.weight(1f)) { onTabSelected(ChildTab.ROOM) }
            NavItem("Дела", "Д", tab == ChildTab.CHORES, Modifier.weight(1f)) { onTabSelected(ChildTab.CHORES) }
            GameProgressNavItem(
                progress = gameProgress,
                selected = tab == ChildTab.GAME,
                modifier = Modifier.weight(1f),
            ) { onTabSelected(ChildTab.GAME) }
            NavItem("Магазин", "М", tab == ChildTab.SHOP, Modifier.weight(1f)) { onTabSelected(ChildTab.SHOP) }
            NavItem("Профиль", "П", tab == ChildTab.PROFILE, Modifier.weight(1f)) { onTabSelected(ChildTab.PROFILE) }
        }
    }
}

@Composable
private fun NavItem(label: String, marker: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onClick, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)) { Text(marker, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GameProgressNavItem(
    progress: Float,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(5.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        ) {
            GameProgressIcon(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Игра открыта на ${(progress.coerceIn(0f, 1f) * 100).toInt()} процентов",
            )
        }
        Text("Игра", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GameProgressIcon(progress: Float, modifier: Modifier = Modifier, contentDescription: String) {
    val fraction = progress.coerceIn(0f, 1f)
    val outline = MaterialTheme.colorScheme.primary
    Canvas(modifier.clearAndSetSemantics { this.contentDescription = contentDescription }) {
        val radius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * .28f)
        drawRoundRect(color = Color(0xFFE8DEFF), cornerRadius = radius)
        clipRect(top = size.height * (1f - fraction)) {
            drawRoundRect(color = Color(0xFFFFB300), cornerRadius = radius)
        }
        drawRoundRect(color = outline, cornerRadius = radius, style = Stroke(width = 2.dp.toPx()))

        val stroke = 2.5.dp.toPx()
        drawLine(outline, androidx.compose.ui.geometry.Offset(size.width * .25f, size.height * .5f), androidx.compose.ui.geometry.Offset(size.width * .48f, size.height * .5f), stroke, StrokeCap.Round)
        drawLine(outline, androidx.compose.ui.geometry.Offset(size.width * .365f, size.height * .385f), androidx.compose.ui.geometry.Offset(size.width * .365f, size.height * .615f), stroke, StrokeCap.Round)
        drawCircle(outline, radius = size.minDimension * .055f, center = androidx.compose.ui.geometry.Offset(size.width * .67f, size.height * .43f))
        drawCircle(outline, radius = size.minDimension * .055f, center = androidx.compose.ui.geometry.Offset(size.width * .76f, size.height * .58f))
    }
}

@Composable
private fun ParentModeScreen(
    modifier: Modifier = Modifier,
    parentName: String,
    onParentNameChange: (String) -> Unit,
    childName: String,
    childCompletedIds: Set<String>,
    onChildCompletedIdsChange: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(ParentTab.OVERVIEW) }
    var completedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var customTaskTitles by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var stars by rememberSaveable { mutableStateOf(12) }
    var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }

    val allParentChores = parentDefaultChores + customTaskTitles.mapIndexed { index, title ->
        Chore("parent_custom_$index", title, "Моё дело", 2, "Добавлено вами", Color(0xFFE8DEFF), required = false)
    }
    val requiredParentChores = allParentChores.filter { it.required }
    val parentGameProgress = if (requiredParentChores.isEmpty()) {
        0f
    } else {
        requiredParentChores.count { completedIds.contains(it.id) }.toFloat() / requiredParentChores.size
    }

    if (showAddTaskDialog) {
        AddParentTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAdd = { title ->
                customTaskTitles = customTaskTitles + title
                showAddTaskDialog = false
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ParentBottomBar(
                tab = tab,
                gameProgress = parentGameProgress,
                onTabSelected = { tab = it },
            )
        },
    ) { padding ->
        when (tab) {
            ParentTab.OVERVIEW -> ParentOverviewTab(
                padding = padding,
                stars = stars,
                parentName = parentName,
                completedCount = completedIds.size,
                totalCount = allParentChores.size,
                onBack = onBack,
            )
            ParentTab.CHORES -> ParentChoresTab(
                padding = padding,
                stars = stars,
                parentName = parentName,
                chores = allParentChores,
                completedIds = completedIds,
                onBack = onBack,
                onAddTask = { showAddTaskDialog = true },
                onToggle = { chore ->
                    if (completedIds.contains(chore.id)) {
                        completedIds = completedIds - chore.id
                        stars = (stars - chore.reward).coerceAtLeast(0)
                    } else {
                        completedIds = completedIds + chore.id
                        stars += chore.reward
                    }
                },
            )
            ParentTab.GAME -> ParentGameTab(
                padding = padding,
                stars = stars,
                progress = parentGameProgress,
                onBack = onBack,
            )
            ParentTab.CHILDREN -> ParentChildrenTab(
                padding = padding,
                childName = childName,
                childCompletedIds = childCompletedIds,
                onBack = onBack,
                onMarkChildChore = { chore -> onChildCompletedIdsChange(childCompletedIds + chore.id) },
            )
            ParentTab.PROFILE -> ParentProfileTab(
                padding = padding,
                stars = stars,
                completedCount = completedIds.size,
                parentName = parentName,
                onNameChange = onParentNameChange,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun ParentOverviewTab(
    padding: PaddingValues,
    stars: Int,
    parentName: String,
    completedCount: Int,
    totalCount: Int,
    onBack: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ChildTopBar("Мой кабинет", stars, onBack, subtitle = "Ваш личный помощник рядом") }
        item {
            ParentAssistantHero(
                parentName = parentName,
                completedCount = completedCount,
                totalCount = totalCount,
            )
        }
    }
}

@Composable
private fun ParentChoresTab(
    padding: PaddingValues,
    stars: Int,
    parentName: String,
    chores: List<Chore>,
    completedIds: Set<String>,
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onToggle: (Chore) -> Unit,
) {
    val requiredDone = chores.count { it.required && completedIds.contains(it.id) }
    val requiredTotal = chores.count { it.required }
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ChildTopBar("Мои дела", stars, onBack, subtitle = "Обязательные и дела по желанию") }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Сегодня", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$requiredDone из $requiredTotal обязательных дел", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${completedIds.size} готово", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { if (requiredTotal == 0) 0f else requiredDone.toFloat() / requiredTotal }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)))
            }
        }
        item { Button(onClick = onAddTask, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(50.dp)) { Text("Добавить своё дело") } }
        item { SectionHeader("Список дел") }
        items(chores, key = { it.id }) { chore ->
            ParentChoreCard(chore = chore, done = completedIds.contains(chore.id), onToggle = onToggle)
        }
        item { ParentAssistantCard(parentName = parentName, completedCount = completedIds.size, totalCount = chores.size) }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text("Ваши дела не влияют на награды ребёнка. Это отдельный личный список для поддержки всей семьи.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun ParentChoreCard(chore: Chore, done: Boolean, onToggle: (Chore) -> Unit) {
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = chore.color)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskCheckbox(done = done, label = chore.title, onToggle = { onToggle(chore) })
            Column(Modifier.weight(1f)) {
                Text(
                    chore.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    chore.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "+${chore.reward} звёзд · ${if (chore.required) "важное" else "по желанию"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun parentAssistantMessage(parentName: String, completedCount: Int, totalCount: Int): String = when {
    completedCount == 0 -> "$parentName, начни с одного небольшого дела. Всё сразу делать не обязательно."
    completedCount < totalCount -> "Отличный шаг, $parentName! Уже выполнено $completedCount. Продолжай, когда будет удобно."
    else -> "$parentName, все дела готовы. Ты сегодня отлично справляешься!"
}

@Composable
private fun ParentAssistantHero(parentName: String, completedCount: Int, totalCount: Int) {
    val message = parentAssistantMessage(parentName, completedCount, totalCount)
    val bubbleColor = MaterialTheme.colorScheme.surface.copy(alpha = .98f)

    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3DDF4)),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                "Ваш помощник",
                modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(.68f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
            ) {
                val compactAssistantLayout = maxWidth < 380.dp
                val helperShiftFraction = if (compactAssistantLayout) .20f else .18f
                val bubbleXFraction = if (compactAssistantLayout) .55f else .56f
                val bubbleWidthFraction = if (compactAssistantLayout) .43f else .42f
                val baseBubbleHeightFraction = if (compactAssistantLayout) .34f else .31f
                val messageLayoutLength = message.length + if (compactAssistantLayout) 5 else 0
                val bubbleExtraHeightFraction = when {
                    messageLayoutLength > 120 -> .12f
                    messageLayoutLength > 100 -> .08f
                    messageLayoutLength > 84 -> .04f
                    else -> 0f
                }
                val bubbleHeightFraction = baseBubbleHeightFraction + bubbleExtraHeightFraction
                val bubbleBottomFraction = if (compactAssistantLayout) .76f else .73f
                val bubbleYFraction = bubbleBottomFraction - bubbleHeightFraction
                val fixedTailHeightFraction = if (compactAssistantLayout) .054f else .062f
                val bubbleBodyBottomRatio =
                    (1f - fixedTailHeightFraction / bubbleHeightFraction).coerceIn(.72f, .88f)
                val messageTextStyle = if (compactAssistantLayout) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                }

                Image(
                    painter = painterResource(R.drawable.parent_helper),
                    contentDescription = "Розовый зайчик — помощник родителя",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = -(maxWidth * helperShiftFraction))
                        .fillMaxWidth(.96f)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit,
                )
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * bubbleXFraction, y = maxHeight * bubbleYFraction)
                        .width(maxWidth * bubbleWidthFraction)
                        .height(maxHeight * bubbleHeightFraction)
                        .clearAndSetSemantics {
                            contentDescription = "Помощник говорит: $message"
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val bodyBottom = size.height * bubbleBodyBottomRatio
                        val tailHeight = size.height - bodyBottom
                        val radius = 18.dp.toPx().coerceAtMost(bodyBottom * .28f)
                        val bubble = Path().apply {
                            moveTo(radius, 0f)
                            lineTo(size.width - radius, 0f)
                            quadraticBezierTo(size.width, 0f, size.width, radius)
                            lineTo(size.width, bodyBottom - radius)
                            quadraticBezierTo(size.width, bodyBottom, size.width - radius, bodyBottom)
                            lineTo(size.width * .33f, bodyBottom)
                            cubicTo(
                                size.width * .28f, bodyBottom + tailHeight * .22f,
                                size.width * .18f, bodyBottom + tailHeight * .72f,
                                size.width * .07f, bodyBottom + tailHeight * .90f,
                            )
                            cubicTo(
                                size.width * .16f, bodyBottom + tailHeight * .70f,
                                size.width * .15f, bodyBottom + tailHeight * .25f,
                                size.width * .13f, bodyBottom,
                            )
                            lineTo(radius, bodyBottom)
                            quadraticBezierTo(0f, bodyBottom, 0f, bodyBottom - radius)
                            lineTo(0f, radius)
                            quadraticBezierTo(0f, 0f, radius, 0f)
                            close()
                        }
                        drawPath(path = bubble, color = bubbleColor)
                        drawPath(
                            path = bubble,
                            color = Color(0xFF71687A),
                            style = Stroke(width = 1.25.dp.toPx()),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = message,
                            style = messageTextStyle,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentGameTab(
    padding: PaddingValues,
    stars: Int,
    progress: Float,
    onBack: () -> Unit,
) {
    var played by rememberSaveable { mutableStateOf(false) }
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
    val unlocked = progress >= 1f

    LazyColumn(
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ChildTopBar("Игра", stars, onBack, subtitle = "Небольшая награда за заботу о себе") }
        item {
            Card(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEFF)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GameProgressIcon(
                        progress = progress,
                        modifier = Modifier.size(128.dp),
                        contentDescription = "Игра родителя открыта на $percent процентов",
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        if (unlocked) "Игра открыта!" else "Открываем игру",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (unlocked) "Все важные дела выполнены. Можно немного отдохнуть!"
                        else "Выполняйте важные дела — золотой цвет постепенно заполнит значок игры.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                        color = Color(0xFFFFB300),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("$percent%", color = Color(0xFF6E4A00), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { played = true },
                        enabled = unlocked && !played,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(if (played) "Сегодня уже сыграли" else if (unlocked) "Начать игру" else "Пока закрыто")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentAssistantCard(parentName: String, completedCount: Int, totalCount: Int) {
    val message = parentAssistantMessage(parentName, completedCount, totalCount)
    Card(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3DDF4)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.parent_helper),
                contentDescription = "Розовый зайчик — помощник родителя",
                modifier = Modifier.size(92.dp).clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f)) {
                Text("Ваш помощник", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ParentChildrenTab(
    padding: PaddingValues,
    childName: String,
    childCompletedIds: Set<String>,
    onBack: () -> Unit,
    onMarkChildChore: (Chore) -> Unit,
) {
    val requiredChores = chores.filter { it.required }
    val doneCount = requiredChores.count { childCompletedIds.contains(it.id) }
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ChildTopBar("Дела ребёнка", 0, onBack, subtitle = "Проверка и поддержка, а не контроль ради контроля") }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEFF))) {
                Column(Modifier.padding(18.dp)) {
                    Text(childName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("$doneCount из ${requiredChores.size} обязательных дел", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { doneCount.toFloat() / requiredChores.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)))
                }
            }
        }
        item { SectionHeader("Можно засчитать вручную") }
        items(requiredChores, key = { it.id }) { chore ->
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(chore.title, fontWeight = FontWeight.Bold)
                        Text(if (childCompletedIds.contains(chore.id)) "Отмечено ребёнком" else "Пока не отмечено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (childCompletedIds.contains(chore.id)) {
                        Text("Готово", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    } else {
                        OutlinedButton(onClick = { onMarkChildChore(chore) }, modifier = Modifier.height(46.dp)) { Text("Засчитать") }
                    }
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text("Если у дела была уважительная причина, его можно засчитать здесь — без лишнего давления на ребёнка.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun ParentProfileTab(
    padding: PaddingValues,
    stars: Int,
    completedCount: Int,
    parentName: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ChildTopBar("Мой профиль", stars, onBack, subtitle = "Личные привычки и баланс семьи") }
        item {
            EditableNameCard(
                name = parentName,
                subtitle = "Личный кабинет родителя",
                imageRes = R.drawable.app_logo,
                imageDescription = "Профиль родителя",
                containerColor = Color(0xFFD9F5EA),
                onNameChange = onNameChange,
            )
        }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text("Ваш профиль помогает заботиться о себе и показывать ребёнку пример без идеальности.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Готово", completedCount.toString(), Modifier.weight(1f)); StatCard("Звёзды", stars.toString(), Modifier.weight(1f)); StatCard("Детей", "1", Modifier.weight(1f)) } }
    }
}

@Composable
private fun AddParentTaskDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое личное дело") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Добавьте небольшую задачу для себя. Она не влияет на прогресс ребёнка.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = title, onValueChange = { title = it.take(40) }, singleLine = true, label = { Text("Название дела") }, placeholder = { Text("Например, выпить воду") })
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(title.trim()) }, enabled = title.trim().length >= 2) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun ParentBottomBar(tab: ParentTab, gameProgress: Float, onTabSelected: (ParentTab) -> Unit) {
    Surface(shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            NavItem("Обзор", "О", tab == ParentTab.OVERVIEW, Modifier.weight(1f)) { onTabSelected(ParentTab.OVERVIEW) }
            NavItem("Мои дела", "Д", tab == ParentTab.CHORES, Modifier.weight(1f)) { onTabSelected(ParentTab.CHORES) }
            GameProgressNavItem(
                progress = gameProgress,
                selected = tab == ParentTab.GAME,
                modifier = Modifier.weight(1f),
            ) { onTabSelected(ParentTab.GAME) }
            NavItem("Дети", "Р", tab == ParentTab.CHILDREN, Modifier.weight(1f)) { onTabSelected(ParentTab.CHILDREN) }
            NavItem("Профиль", "П", tab == ParentTab.PROFILE, Modifier.weight(1f)) { onTabSelected(ParentTab.PROFILE) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScaffoldScreenPreview() { MyHomeChoresTheme { ScaffoldScreen(environment = BuildConfig.APP_ENVIRONMENT) } }
