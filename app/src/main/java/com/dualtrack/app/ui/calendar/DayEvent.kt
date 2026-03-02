package com.dualtrack.app.ui.calendar

data class DayEvent(
    val id: String = "",
    val dayMillis: Long = 0L,
    val title: String = "",
    val details: String = "",
    val createdAt: Long = 0L
)
