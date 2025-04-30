package com.example.notes.ui.screens.note

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notes.data.NotesRepository
import com.example.notes.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteScreenViewModel @Inject constructor(
    private val noteRepository: NotesRepository
): ViewModel() {
    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note

    fun getNoteById(noteId: String) {
        viewModelScope.launch {
           _note.value = noteRepository.getNoteById(noteId)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            Log.d("NoteId", "deleteNote: $noteId")
            noteRepository.deleteNote(noteId)
        }
    }

    fun updateNote(note: Note, taskLines: List<String>, noteTextField: TextFieldValue, title: String) {
        val taskContent = taskLines.joinToString("\n")
        val finalContent = if (noteTextField.text.isNotEmpty() && taskContent.isNotEmpty()) {
            "${noteTextField.text}\n$taskContent"
        } else {
            noteTextField.text + taskContent
        }

        val updatedNote = Note(
            id = note.id,
            date = note.date,
            title = title,
            content = finalContent
        )
        viewModelScope.launch {
            noteRepository.updateNote(updatedNote)
        }
    }

    fun splitContent(content: String): Pair<String, String> {
        val textLines = StringBuilder()
        val taskLines = StringBuilder()

        content.split("\n").forEach { line ->
            if (line.startsWith("[ ]") || line.startsWith("[x]")) {
                if (taskLines.isNotEmpty()) {
                    taskLines.append("\n")
                }
                taskLines.append(line)
            } else {
                if (textLines.isNotEmpty()) {
                    textLines.append("\n")
                }
                textLines.append(line)
            }
        }

        return Pair(textLines.toString(), taskLines.toString())
    }

}