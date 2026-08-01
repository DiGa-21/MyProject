package com.myhomechores.app.features.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhomechores.app.R
import com.myhomechores.app.core.AppConfig
import com.myhomechores.app.domain.model.AppRole
import com.myhomechores.app.ui.theme.MyHomeChoresTheme

@Composable
fun ScaffoldScreen(
    environment: String,
    modifier: Modifier = Modifier,
) {
    var selectedRole by remember { mutableStateOf<AppRole?>(null) }

    when (selectedRole) {
        null -> ModeSelectionScreen(
            environment = environment,
            modifier = modifier,
            onChildClick = { selectedRole = AppRole.CHILD },
            onParentClick = { selectedRole = AppRole.PARENT },
        )

        AppRole.CHILD -> ChildModeScreen(
            modifier = modifier,
            onBack = { selectedRole = null },
        )

        AppRole.PARENT -> ParentModeScreen(
            modifier = modifier,
            onBack = { selectedRole = null },
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
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "★",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 58.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = AppConfig.WORKING_NAME,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.scaffold_ready),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))

            StatusCard(
                title = stringResource(R.string.child_mode),
                description = stringResource(R.string.child_mode_description),
                marker = "✓",
                onClick = onChildClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            StatusCard(
                title = stringResource(R.string.parent_mode),
                description = stringResource(R.string.parent_mode_description),
                marker = "★",
                onClick = onParentClick,
            )
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "${AppConfig.BUILD_STAGE} · $environment",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChildModeScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    var teethMorningDone by remember { mutableStateOf(false) }
    var homeworkDone by remember { mutableStateOf(false) }
    var readingDone by remember { mutableStateOf(false) }
    val completedCount = listOf(teethMorningDone, homeworkDone, readingDone).count { it }

    AppScreenFrame(
        title = "Режим ребёнка",
        subtitle = "Дела на сегодня",
        modifier = modifier,
        onBack = onBack,
    ) {
        Text(
            text = "Выполнено: $completedCount из 3    ★ ${completedCount * 2}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TaskRow("Почистить зубы утром", teethMorningDone) { teethMorningDone = it }
        TaskRow("Сделать уроки", homeworkDone) { homeworkDone = it }
        TaskRow("Почитать 15 минут", readingDone) { readingDone = it }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Помощник: Отличное начало! Выбери дело, которое удобно выполнить сейчас.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ParentModeScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    var extraTaskAdded by remember { mutableStateOf(false) }

    AppScreenFrame(
        title = "Режим родителя",
        subtitle = "Управление делами",
        modifier = modifier,
        onBack = onBack,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Дети", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Пока добавьте первого ребёнка в следующей версии.")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { extraTaskAdded = !extraTaskAdded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (extraTaskAdded) "Дополнительное дело добавлено" else "Добавить дополнительное дело")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Здесь будут расписание, подтверждение дел, звёзды и награды.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun AppScreenFrame(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
        ) {
            OutlinedButton(onClick = onBack) { Text("Назад") }
            Spacer(modifier = Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
private fun TaskRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    description: String,
    marker: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = marker,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScaffoldScreenPreview() {
    MyHomeChoresTheme {
        ScaffoldScreen(environment = "preview")
    }
}
