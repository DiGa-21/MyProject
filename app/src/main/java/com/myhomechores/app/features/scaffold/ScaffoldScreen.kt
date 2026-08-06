package com.myhomechores.app.features.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhomechores.app.BuildConfig
import com.myhomechores.app.R
import com.myhomechores.app.core.AppConfig
import com.myhomechores.app.domain.model.AppRole
import com.myhomechores.app.ui.theme.MyHomeChoresTheme

private enum class ChildTab { ROOM, CHORES, SHOP, PROFILE }

private enum class Hero(val id: String, val displayName: String, val description: String, val imageRes: Int) {
    BOY("boy", "Бирюзовый дракончик", "Любит исследовать и пробовать новое", R.drawable.dragon_boy),
    GIRL("girl", "Лавандовый дракончик", "Поддерживает и замечает твои успехи", R.drawable.dragon_girl),
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

@Composable
fun ScaffoldScreen(
    environment: String,
    modifier: Modifier = Modifier,
) {
    var selectedRole by rememberSaveable { mutableStateOf<AppRole?>(null) }

    when (selectedRole) {
        null -> ModeSelectionScreen(
            environment = environment,
            modifier = modifier,
            onChildClick = { selectedRole = AppRole.CHILD },
            onParentClick = { selectedRole = AppRole.PARENT },
        )

        AppRole.CHILD -> ChildModeScreen(modifier = modifier, onBack = { selectedRole = null })
        AppRole.PARENT -> ParentModeScreen(modifier = modifier, onBack = { selectedRole = null })
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
            Box(
                modifier = Modifier.size(104.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text("М", color = MaterialTheme.colorScheme.primary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(22.dp))
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
private fun ChildModeScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var selectedHeroId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedHero = Hero.values().firstOrNull { it.id == selectedHeroId }
    if (selectedHero == null) {
        HeroSelectionScreen(modifier = modifier, onBack = onBack) { selectedHeroId = it.id }
        return
    }

    var tab by rememberSaveable { mutableStateOf(ChildTab.CHORES) }
    var completedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedOptionalIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var stars by rememberSaveable { mutableStateOf(24) }
    var category by rememberSaveable { mutableStateOf("Все") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { ChildBottomBar(tab = tab, onTabSelected = { tab = it }) },
    ) { padding ->
        when (tab) {
            ChildTab.ROOM -> RoomTab(padding, stars, selectedHero, onBack)
            ChildTab.CHORES -> ChoresTab(
                padding = padding,
                stars = stars,
                completedIds = completedIds,
                selectedOptionalIds = selectedOptionalIds,
                category = category,
                hero = selectedHero,
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
                if (!completedIds.contains(chore.id)) {
                    completedIds = completedIds + chore.id
                    stars += chore.reward
                }
            }
            ChildTab.SHOP -> ShopTab(padding, stars, selectedHero) { price -> if (stars >= price) stars -= price }
            ChildTab.PROFILE -> ProfileTab(padding, stars, completedIds.size, selectedHero)
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
private fun ChildTopBar(title: String, stars: Int, onBack: (() -> Unit)? = null, hero: Hero? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            OutlinedButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(44.dp)) { Text("Назад") }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Сегодня, ${if (title == "Мои дела") "ты справишься" else "твой маленький мир"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        if (hero != null) {
            Image(painter = painterResource(hero.imageRes), contentDescription = hero.displayName, modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(8.dp))
        }
        Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text("$stars звёзд", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
        item { ChildTopBar("Мои дела", stars, onBack, hero) }
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
            Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = .8f)), contentAlignment = Alignment.Center) {
                Text(chore.category.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(chore.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(chore.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("+${chore.reward} звёзд", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Button(onClick = { if (!done) onToggle(chore) }, enabled = !done, modifier = Modifier.height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)) {
                Text(if (done) "Готово" else "Сделать", color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
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
            Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = .8f)), contentAlignment = Alignment.Center) {
                Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(chore.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("+${chore.reward} звёзд · ${chore.hint}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (done) {
                Button(onClick = { }, enabled = false, modifier = Modifier.height(48.dp)) { Text("Готово") }
            } else if (selected) {
                Button(onClick = { onToggle(chore) }, modifier = Modifier.height(48.dp)) { Text("Сделать") }
            } else {
                OutlinedButton(onClick = { onSelect(chore) }, enabled = canSelect, modifier = Modifier.height(48.dp)) { Text("Выбрать") }
            }
        }
    }
}

@Composable
private fun RoomTab(padding: PaddingValues, stars: Int, hero: Hero, onBack: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ChildTopBar("Моя комната", stars, onBack, hero) }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE6DFFF))) {
                Column(Modifier.padding(20.dp)) {
                    Text("Комната Алекса", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFC8B8F6))) {
                        Image(painter = painterResource(hero.imageRes), contentDescription = hero.displayName, modifier = Modifier.align(Alignment.BottomCenter).size(150.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                        Box(Modifier.align(Alignment.BottomStart).padding(18.dp).size(62.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFE49B)))
                        Box(Modifier.align(Alignment.TopEnd).padding(18.dp).size(54.dp).clip(CircleShape).background(Color(0xFF9BE4CE)))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Продолжай выполнять дела, чтобы открыть новые предметы для комнаты.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionHeader("Сегодня можно") }
        item { QuickPreview("Дела", "${chores.size} задания ждут", "Д") }
        item { QuickPreview("Сюрприз", "Новая награда после игры", "С") }
    }
}

@Composable
private fun SectionHeader(text: String) { Text(text, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

@Composable
private fun QuickPreview(title: String, subtitle: String, marker: String) {
    Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text(marker, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer) }
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ShopTab(padding: PaddingValues, stars: Int, hero: Hero, onBuy: (Int) -> Unit) {
    val items = listOf("Драконья пицца" to 12, "Крутые очки" to 20, "Космическая лампа" to 30, "Мягкая подушка" to 16)
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ChildTopBar("Магазин", stars, hero = hero) }
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
private fun ProfileTab(padding: PaddingValues, stars: Int, completed: Int, hero: Hero) {
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ChildTopBar("Профиль", stars, hero = hero) }
        item {
            Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFD9F5EA))) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Image(painter = painterResource(hero.imageRes), contentDescription = hero.displayName, modifier = Modifier.size(72.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    Column { Text(hero.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Уровень 2 · Исследователь", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { .62f }, modifier = Modifier.width(160.dp).height(8.dp).clip(RoundedCornerShape(50))) }
                }
            }
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
private fun StatCard(title: String, value: String, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) } } }

@Composable
private fun Badge(title: String, marker: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text(marker, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer) }; Spacer(Modifier.height(4.dp)); Text(title, style = MaterialTheme.typography.labelSmall) } }

@Composable
private fun ChildBottomBar(tab: ChildTab, onTabSelected: (ChildTab) -> Unit) {
    Surface(shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            NavItem("Комната", "К", tab == ChildTab.ROOM) { onTabSelected(ChildTab.ROOM) }
            NavItem("Дела", "Д", tab == ChildTab.CHORES) { onTabSelected(ChildTab.CHORES) }
            NavItem("Магазин", "М", tab == ChildTab.SHOP) { onTabSelected(ChildTab.SHOP) }
            NavItem("Профиль", "П", tab == ChildTab.PROFILE) { onTabSelected(ChildTab.PROFILE) }
        }
    }
}

@Composable
private fun NavItem(label: String, marker: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(82.dp).clip(RoundedCornerShape(16.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onClick, modifier = Modifier.size(48.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)) { Text(marker, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ParentModeScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var extraTaskAdded by rememberSaveable { mutableStateOf(false) }
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { ChildTopBar("Панель родителя", 0, onBack) }
            item {
                Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Дети", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFFFC7A6)), contentAlignment = Alignment.Center) { Text("А", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B4D9F)) }
                            Column { Text("Алекс", fontWeight = FontWeight.Bold); Text("2 из 4 дел выполнено", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { .5f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)))
                    }
                }
            }
            item {
                Button(onClick = { extraTaskAdded = !extraTaskAdded }, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp)) { Text(if (extraTaskAdded) "Дополнительное дело добавлено" else "Добавить дополнительное дело") }
            }
            item {
                Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) { Text("Подтверждение результатов", fontWeight = FontWeight.Bold); Text("Здесь родитель сможет засчитать дело без фото или отметить уважительную причину.", color = MaterialTheme.colorScheme.onPrimaryContainer) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScaffoldScreenPreview() { MyHomeChoresTheme { ScaffoldScreen(environment = BuildConfig.APP_ENVIRONMENT) } }
