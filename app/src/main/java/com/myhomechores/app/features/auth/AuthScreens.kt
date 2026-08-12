package com.myhomechores.app.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myhomechores.app.R

data class AuthCallbacks(
    val onBack: () -> Unit,
    val onTab: (AuthTab) -> Unit,
    val onSignIn: (String, String) -> Unit,
    val onSignUp: (String, String, String, String) -> Unit,
    val onShowReset: () -> Unit,
    val onHideReset: () -> Unit,
    val onReset: (String) -> Unit,
    val onSaveNewPassword: (String, String) -> Unit,
)

@Composable
fun ParentAuthRoute(
    gateway: AuthGateway,
    onBack: () -> Unit,
    passwordRecoveryRequested: Boolean = false,
    onPasswordRecoveryHandled: () -> Unit = {},
    parentContent: @Composable (onSignOut: () -> Unit) -> Unit,
) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(gateway))
    val state by authViewModel.state.collectAsState()
    LaunchedEffect(passwordRecoveryRequested, state.stage) {
        if (passwordRecoveryRequested && state.stage == AuthStage.AUTHENTICATED) {
            authViewModel.enterPasswordRecovery()
            onPasswordRecoveryHandled()
        }
    }
    when (state.stage) {
        AuthStage.INITIALIZING -> AuthLoadingScreen()
        AuthStage.AUTHENTICATED -> parentContent(authViewModel::signOut)
        AuthStage.PASSWORD_RECOVERY -> PasswordRecoveryContent(
            state = state,
            onBack = {
                authViewModel.cancelPasswordRecovery()
                onBack()
            },
            onSave = authViewModel::saveNewPassword,
        )
        else -> ParentAuthContent(
            state = state,
            callbacks = AuthCallbacks(
                onBack = onBack,
                onTab = authViewModel::selectTab,
                onSignIn = authViewModel::signIn,
                onSignUp = authViewModel::signUp,
                onShowReset = authViewModel::showResetDialog,
                onHideReset = authViewModel::hideResetDialog,
                onReset = authViewModel::requestPasswordReset,
                onSaveNewPassword = authViewModel::saveNewPassword,
            ),
        )
    }
}

@Composable
private fun AuthLoadingScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(42.dp))
        }
    }
}

@Composable
fun ParentAuthContent(state: AuthUiState, callbacks: AuthCallbacks) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var repeat by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val submitting = state.stage == AuthStage.SUBMITTING

    if (state.resetDialogVisible) {
        PasswordResetDialog(
            email = email,
            emailError = state.errors.email,
            submitting = submitting,
            onEmailChange = { email = it },
            onDismiss = callbacks.onHideReset,
            onSend = { callbacks.onReset(email) },
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Row(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = callbacks.onBack, modifier = Modifier.height(48.dp)) { Text("Назад") }
                }
            }
            item {
                Image(
                    painter = painterResource(R.drawable.parent_auth_helper),
                    contentDescription = "Помощник родителя",
                    modifier = Modifier.size(196.dp).clip(RoundedCornerShape(34.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Кабинет родителя", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Войди, чтобы управлять делами семьи",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFE8DEFF)).padding(4.dp),
                ) {
                    AuthTabButton("Вход", state.tab == AuthTab.SIGN_IN, Modifier.weight(1f)) { callbacks.onTab(AuthTab.SIGN_IN) }
                    AuthTabButton("Регистрация", state.tab == AuthTab.REGISTRATION, Modifier.weight(1f)) { callbacks.onTab(AuthTab.REGISTRATION) }
                }
            }
            if (state.stage == AuthStage.AWAITING_EMAIL_CONFIRMATION) {
                item { NoticeCard(state.notice.orEmpty()) }
            } else {
                if (state.tab == AuthTab.REGISTRATION) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Имя") },
                            isError = state.errors.displayName != null,
                            supportingText = state.errors.displayName?.let { message -> ({ Text(message) }) },
                            singleLine = true,
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Электронная почта") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        isError = state.errors.email != null,
                        supportingText = state.errors.email?.let { message -> ({ Text(message) }) },
                        singleLine = true,
                    )
                }
                item {
                    AuthPasswordField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Пароль",
                        error = state.errors.password,
                        visible = passwordVisible,
                        onVisibleChange = { passwordVisible = !passwordVisible },
                    )
                }
                if (state.tab == AuthTab.REGISTRATION) {
                    item {
                        AuthPasswordField(
                            value = repeat,
                            onValueChange = { repeat = it },
                            label = "Повтори пароль",
                            error = state.errors.passwordRepeat,
                            visible = passwordVisible,
                            onVisibleChange = { passwordVisible = !passwordVisible },
                        )
                    }
                }
                state.errors.general?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                state.notice?.let { message -> item { NoticeCard(message) } }
                item {
                    Button(
                        onClick = {
                            if (state.tab == AuthTab.SIGN_IN) callbacks.onSignIn(email, password)
                            else callbacks.onSignUp(name, email, password, repeat)
                        },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(if (state.tab == AuthTab.SIGN_IN) "Войти" else "Создать аккаунт")
                            if (submitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.CenterEnd).size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
                if (state.tab == AuthTab.SIGN_IN) {
                    item { TextButton(onClick = callbacks.onShowReset, modifier = Modifier.height(48.dp)) { Text("Забыли пароль?") } }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun AuthTabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(15.dp)) { Text(text) }
    } else {
        TextButton(onClick = onClick, modifier = modifier.height(48.dp)) { Text(text) }
    }
}

@Composable
private fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    visible: Boolean,
    onVisibleChange: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        trailingIcon = {
            IconButton(onClick = onVisibleChange) { Text(if (visible) "Скрыть" else "Показать") }
        },
        isError = error != null,
        supportingText = error?.let { message -> ({ Text(message) }) },
        singleLine = true,
    )
}

@Composable
private fun PasswordResetDialog(
    email: String,
    emailError: String?,
    submitting: Boolean,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Восстановить пароль") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Электронная почта") },
                isError = emailError != null,
                supportingText = emailError?.let { message -> ({ Text(message) }) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = onSend, enabled = !submitting) { Text("Отправить письмо") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun NoticeCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
