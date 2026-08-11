package com.myhomechores.app.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myhomechores.app.data.remote.SupabaseRepository

@Composable
fun ParentAuthScreen(repository: SupabaseRepository, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val vm: AuthViewModel = viewModel(factory = AuthViewModel.Factory(repository))
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Вход родителя", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(email, { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Email") })
        OutlinedTextField(password, { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Пароль") })
        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Имя") })
        Button(onClick = { vm.signIn(email, password) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Войти") }
        Button(onClick = { vm.signUp(email, password, name) }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Создать аккаунт") }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.signedIn) Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Продолжить") }
    }
}
