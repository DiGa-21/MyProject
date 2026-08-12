package com.myhomechores.app.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PasswordRecoveryContent(
    state: AuthUiState,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var repeat by rememberSaveable { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.height(48.dp)) { Text("Назад") }
            Text("Новый пароль", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Придумай новый пароль длиной не менее 6 символов")
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Новый пароль") },
                visualTransformation = PasswordVisualTransformation(),
                isError = state.errors.password != null,
                supportingText = state.errors.password?.let { message -> ({ Text(message) }) },
                singleLine = true,
            )
            OutlinedTextField(
                value = repeat,
                onValueChange = { repeat = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Повтори пароль") },
                visualTransformation = PasswordVisualTransformation(),
                isError = state.errors.passwordRepeat != null,
                supportingText = state.errors.passwordRepeat?.let { message -> ({ Text(message) }) },
                singleLine = true,
            )
            state.errors.general?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { onSave(password, repeat) },
                enabled = state.stage != AuthStage.SUBMITTING,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Сохранить пароль") }
        }
    }
}
