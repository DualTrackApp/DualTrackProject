package com.dualtrack.app.ui.calendar

data class CalendarEvent(
    val id: String,
    val title: String,
    val details: String,
    val time: String,
    val dayMillis: Long
)