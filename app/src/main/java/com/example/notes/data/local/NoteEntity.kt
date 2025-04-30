package com.example.notes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notes.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = "",
    val date: String = "",
    val title: String = "",
    val content: String = ""
) {
    fun toNote(): Note = Note(id, date, title, content)
    companion object {
        fun fromNote(note: Note) = NoteEntity(note.id, note.date, note.title, note.content)
    }
}