package com.myhomechores.app.features.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myhomechores.app.data.AppRepository

@Composable
fun InviteCodeRoute(
    gateway: FamilyGateway,
    localRepository: AppRepository,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onLinked: () -> Unit,
) {
    val viewModel: InviteCodeViewModel = viewModel(
        factory = InviteCodeViewModel.Factory(gateway, localRepository),
    )
    val state by viewModel.state.collectAsState()
    InviteCodeContent(
        state = state,
        modifier = modifier,
        onBack = onBack,
        onCodeChange = viewModel::updateCode,
        onSubmit = viewModel::submit,
        onLinked = {
            viewModel.acknowledgeLinked()
            onLinked()
        },
    )
}

@Composable
fun InviteCodeContent(
    state: InviteCodeUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onLinked: () -> Unit,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.align(Alignment.Start).height(48.dp)) {
                Text("Назад")
            }
            Spacer(Modifier.weight(1f))
            Text("Код подключения", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Попроси родителя открыть твой профиль и назвать 6 цифр",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.code,
                onValueChange = onCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Одноразовый код") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                isError = state.error != null,
                supportingText = state.error?.let { message -> ({ Text(message) }) },
            )
            if (state.retryAfterSeconds > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Попробуй снова через ${state.retryAfterSeconds / 60}:${(state.retryAfterSeconds % 60).toString().padStart(2, '0')}",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = if (state.linkedChild == null) onSubmit else onLinked,
                enabled = state.code.length == 6 && !state.loading && state.retryAfterSeconds == 0,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.height(24.dp), strokeWidth = 2.dp)
                else Text(if (state.linkedChild == null) "Подключиться" else "Выбрать дракона")
            }
            state.linkedChild?.let {
                Spacer(Modifier.height(10.dp))
                Text("Профиль ${it.displayName} подключён", color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.weight(1f))
        }
    }
}
