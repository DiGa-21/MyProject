package com.myhomechores.app.data.remote

fun authMessage(error: Throwable): String {
    val source = error.message.orEmpty().lowercase()
    return when {
        "invalid login credentials" in source || "invalid credentials" in source ->
            "Неверная почта или пароль"
        "already registered" in source || "already exists" in source ->
            "Аккаунт с такой почтой уже существует"
        "too many requests" in source || "rate limit" in source ->
            "Слишком много попыток. Попробуй немного позже"
        "unable to resolve host" in source ||
            "network" in source ||
            "timeout" in source ||
            "failed to connect" in source ->
            "Нет связи с интернетом. Попробуй ещё раз"
        else -> "Не удалось выполнить действие. Попробуй ещё раз"
    }
}
