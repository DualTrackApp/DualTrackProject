package com.dualtrack.app.ui.updates

import com.google.firebase.Timestamp

data class TeamUpdate(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val createdAt: Timestamp?
)