package com.example.notes.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String = "",
    val date: String = "",
    val title: String = "",
    val content: String = ""
) {
    constructor() : this(
        id = "",
        date = "",
        title = "",
        content = ""
    )
}