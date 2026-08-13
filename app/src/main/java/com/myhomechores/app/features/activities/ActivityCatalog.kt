package com.myhomechores.app.features.activities

data class TryActivity(
    val id: String,
    val title: String,
    val subtitle: String,
    val marker: String,
    val available: Boolean,
)

val tryActivities = listOf(
    TryActivity("english", "Английский", "Урок «Природа»", "A", true),
    TryActivity("math", "Математика", "Задания появятся позже", "×", false),
    TryActivity("meditation", "Медитация", "Занятие появится позже", "○", false),
    TryActivity("breathing", "Дыхание", "Занятие появится позже", "~", false),
    TryActivity("nature", "Природа", "Задания появятся позже", "Л", false),
    TryActivity("about-me", "Обо мне", "Задания появятся позже", "Я", false),
)
