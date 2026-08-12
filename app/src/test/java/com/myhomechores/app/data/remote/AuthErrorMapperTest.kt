package com.myhomechores.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMapperTest {
    @Test
    fun maps_invalid_credentials() {
        assertEquals(
            "Неверная почта или пароль",
            authMessage(IllegalStateException("Invalid login credentials")),
        )
    }

    @Test
    fun maps_network_failure() {
        assertEquals(
            "Нет связи с интернетом. Попробуй ещё раз",
            authMessage(IllegalStateException("Unable to resolve host")),
        )
    }

    @Test
    fun never_returns_tokens_or_raw_server_text() {
        assertEquals(
            "Не удалось выполнить действие. Попробуй ещё раз",
            authMessage(IllegalStateException("refresh_token=secret")),
        )
    }
}
