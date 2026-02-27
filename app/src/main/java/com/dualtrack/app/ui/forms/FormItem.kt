package com.dualtrack.app.ui.forms

import com.google.firebase.Timestamp

data class FormItem(
    val id: String,
    val formType: String,
    val status: String,
    val createdAt: Timestamp?
)