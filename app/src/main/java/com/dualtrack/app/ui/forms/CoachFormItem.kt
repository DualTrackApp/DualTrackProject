package com.dualtrack.app.ui.forms



data class CoachFormItem(

    val id: String,

    val athleteEmail: String,

    val formType: String,

    val status: String,

    val dueDate: String = "",

    val requestInstructions: String = "",

    val section: String = "",

    val createdAtLabel: String = ""

)

