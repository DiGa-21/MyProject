package com.myhomechores.app.features.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ParentChildLinkCard(
    state: ParentChildLinkUiState,
    onCreateChild: (String, String?) -> Unit,
    onCreateInvite: (String) -> Unit,
    onDisconnect: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var disconnectId by rememberSaveable { mutableStateOf<String?>(null) }

    disconnectId?.let { childId ->
        AlertDialog(
            onDismissRequest = { disconnectId = null },
            title = { Text("Отключить устройство?") },
            text = { Text("На устройстве ребёнка снова потребуется одноразовый код.") },
            confirmButton = {
                TextButton(onClick = { onDisconnect(childId); disconnectId = null }) { Text("Отключить") }
            },
            dismissButton = { TextButton(onClick = { disconnectId = null }) { Text("Отмена") } },
        )
    }

    Card(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Подключение ребёнка", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (state.children.isNotEmpty()) {
                state.children.forEach { child ->
                    Text(child.display_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    child.parent_label?.let { Text("Ваша пометка: $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(if (child.deviceConnected) "Устройство подключено" else "Устройство ещё не подключено", color = MaterialTheme.colorScheme.primary)
                    if (!child.deviceConnected) {
                        Button(onClick = { onCreateInvite(child.id) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Подключить устройство ребёнка") }
                    } else {
                        OutlinedButton(onClick = { disconnectId = child.id }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Отключить устройство") }
                    }
                }
                Text("Добавить ещё ребёнка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            run {
                OutlinedTextField(name, { name = it.take(30) }, Modifier.fillMaxWidth(), label = { Text("Имя ребёнка") }, singleLine = true)
                OutlinedTextField(label, { label = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("Пометка только для родителя") }, singleLine = true)
                Text("Ребёнок сможет изменить видимое имя. Ваша пометка останется только в кабинете родителя.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { onCreateChild(name, label) }, enabled = name.trim().isNotEmpty() && !state.loading, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Создать профиль") }
            }
            state.invite?.let { invite ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(invite.code.chunked(3).joinToString(" "), Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        Text("Действует 15 минут. Каждый код можно использовать один раз.", textAlign = TextAlign.Center)
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
