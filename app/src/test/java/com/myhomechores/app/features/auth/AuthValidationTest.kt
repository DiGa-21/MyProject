package com.myhomechores.app.features.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {
    @Test
    fun registration_trims_fields_and_accepts_six_characters() {
        val result = validateRegistration("  Мама  ", " parent@example.com ", "123456", "123456")

        assertEquals("Мама", result.value?.displayName)
        assertEquals("parent@example.com", result.value?.email)
        assertNull(result.errors.general)
    }

    @Test
    fun registration_rejects_short_and_different_passwords() {
        val result = validateRegistration("Мама", "parent@example.com", "12345", "54321")

        assertEquals("Минимум 6 символов", result.errors.password)
        assertEquals("Пароли не совпадают", result.errors.passwordRepeat)
    }

    @Test
    fun sign_in_rejects_invalid_email_before_network() {
        val result = validateSignIn("not-an-email", "123456")

        assertEquals("Проверь адрес электронной почты", result.errors.email)
        assertNull(result.value)
    }

    @Test
    fun new_password_requires_matching_six_character_values() {
        val result = validateNewPassword("abcdef", "abcdef")

        assertEquals("abcdef", result.value)
    }
}
