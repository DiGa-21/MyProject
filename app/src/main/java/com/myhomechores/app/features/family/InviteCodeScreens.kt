package com.myhomechores.app.features.family

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
fun InviteCodeScreen(repository: SupabaseRepository, modifier: Modifier = Modifier, onLinked: () -> Unit = {}) {
    val vm: InviteCodeViewModel = viewModel(factory = InviteCodeViewModel.Factory(repository))
    val state by vm.state.collectAsState()
    var code by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Код подключения", style = MaterialTheme.typography.headlineSmall)
        Text("Попроси родителя назвать одноразовый код.")
        OutlinedTextField(code, { code = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Код") })
        Button(onClick = { vm.consume(code) }, enabled = code.isNotBlank() && !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Подключиться") }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.linkedChild?.let {
            Text("Профиль ${it.display_name} подключён")
            Button(onClick = onLinked, modifier = Modifier.fillMaxWidth()) { Text("Продолжить") }
        }
    }
}
