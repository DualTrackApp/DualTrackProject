package com.dualtrack.app.ui.home

data class HomeCard(
    val title: String,
    val subtitle: String? = null,
    val imageResId: Int? = null,
    val iconResId: Int? = null,
    val docId: String? = null,
    val formType: String? = null
)

