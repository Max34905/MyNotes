package com.example.notes.ui.screens.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notes.data.NotesRepository
import com.example.notes.model.Note
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesListScreenViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _isNavigatingToNote = MutableStateFlow<String?>(null)
    val isNavigatingToNote: StateFlow<String?> = _isNavigatingToNote.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var isListening = false

    init {
        notesRepository.setupRealtimeSync()
        getNotes()
    }

    fun getNotes() {
        if(isListening) return
        isListening = true
        viewModelScope.launch {
            notesRepository.getNotesFlow().collect { notes ->
                _notes.value = notes
                Log.d("NotesListScreenViewModel", "getNotes: $notes")
            }
        }
    }

    fun addNote() {
        viewModelScope.launch {
            val noteId = notesRepository.addNote()
            Log.d("NotesListScreenViewModel", "addNote: $noteId")
            if (noteId.isNotEmpty()) {
                _isNavigatingToNote.value = noteId
            }
        }
    }

    fun onNavigatedToNote() {
        _isNavigatingToNote.value = null
    }

    fun signOut() {
        _notes.value = emptyList()
        isListening = false
        viewModelScope.launch {
            notesRepository.cleanUp()
            notesRepository.clearLocalData()
            auth.signOut()
        }
    }
}