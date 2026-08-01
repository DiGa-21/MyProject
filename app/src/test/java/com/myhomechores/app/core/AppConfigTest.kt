package com.myhomechores.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigTest {
    @Test
    fun workingName_isDefined() {
        assertEquals("Мои домашние дела", AppConfig.WORKING_NAME)
    }
}

