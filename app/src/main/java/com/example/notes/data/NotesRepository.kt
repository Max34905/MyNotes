package com.example.notes.data

import android.util.Log
import com.example.notes.data.local.NoteDao
import com.example.notes.data.local.NoteEntity
import com.example.notes.model.Note
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class NotesRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    val database: FirebaseDatabase = Firebase.database("https://notes-fd75d-default-rtdb.europe-west1.firebasedatabase.app/")
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var listener: ValueEventListener? = null

    init {
        try {
            getNotesRef().keepSynced(true)
            setupRealtimeSync()
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error setting up persistence: ${e.message}", e)
        }
    }

    private fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    }
    private fun getNotesRef() = database.getReference("notes").child(getUserId())

    fun setupRealtimeSync() {
        val notesRef = getNotesRef()
        listener?.let { notesRef.removeEventListener(it) }
        Log.d("NotesRepository", "Setting up realtime sync for $notesRef")
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("NotesRepository", "Data changed: $snapshot")
                coroutineScope.launch {
                    processSnapshot(snapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotesRepository", "Database error: ${error.message}", error.toException())
            }
        }
        notesRef.addValueEventListener(listener!!)
        Log.d("NotesRepository", "Listener attached successfully")
    }

    private suspend fun processSnapshot(snapshot: DataSnapshot) {
        try {
            val newNotes = snapshot.children.mapNotNull { dataSnapshot ->
                try {
                    Log.d("NotesRepository", "Successfully converted data")
                    dataSnapshot.getValue(Note::class.java)
                } catch (e: Exception) {
                    Log.e("NotesRepository", "Error converting data: ${e.message}", e)
                    try {
                        val noteId = dataSnapshot.key ?: return@mapNotNull null
                        val content = dataSnapshot.getValue(String::class.java) ?: ""
                        Note(id = noteId, content = content)
                    } catch (e2: Exception) {
                        Log.e("NotesRepository", "Failed fallback conversion: ${e2.message}", e2)
                        null
                    }
                }
            }
            noteDao.getAllNotesList().forEach { note ->
                if (!newNotes.contains(note.toNote())) {
                    noteDao.deleteNote(note.id)
                }
            }
            newNotes.forEach { note ->
                noteDao.upsertNote(NoteEntity.fromNote(note))
            }
        } catch (error: Exception) {
            Log.e("NotesRepository", "Database error: ${error.message}", error)
        }
    }
    
    fun getNotesFlow(): Flow<List<Note>> {
        return noteDao.getAllNotesFlow().map { noteEntities ->
            noteEntities.map { it.toNote() }
        }
    }

    suspend fun addNote(): String {
        return try {
            val notesRef = getNotesRef()
            val key = notesRef.push().key ?: return ""
            val calendar = Calendar.getInstance()
            calendar.time = Date()
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val noteWithId = Note(
                id = key,
                date = if (month < 10) "$day.0$month" else "$day.$month"
            )
            val noteMap = mapOf(
                "id" to noteWithId.id,
                "date" to noteWithId.date,
                "title" to noteWithId.title,
                "content" to noteWithId.content
            )
            notesRef.child(key).setValue(noteMap).await()
            noteDao.upsertNote(NoteEntity.fromNote(noteWithId))
            noteWithId.id
        } catch (e: Exception) {
            Log.d("Firebase", "Failed to add note: ${e.message}")
            ""
        }
    }

    suspend fun updateNote(note: Note) {
        try {
            val notesRef = getNotesRef()
            Log.d("Firebase", "Updating note: ${note.id}")

            val noteMap = mapOf(
                "id" to note.id,
                "date" to note.date,
                "title" to note.title,
                "content" to note.content
            )
            
            notesRef.child(note.id).updateChildren(noteMap).await()
            noteDao.upsertNote(NoteEntity.fromNote(note))
        } catch (e: Exception) {
            Log.e("Firebase", "Failed to update note: ${e.message}", e)
        }
    }

    suspend fun deleteNote(noteId: String) {
        try {
            val notesRef = getNotesRef()
            notesRef.child(noteId).removeValue().await()
            Log.d("NoteId", "deleteNote: $noteId")
            noteDao.deleteNote(noteId)
        } catch (e: Exception) {
            Log.e("Firebase", "Failed to delete note: ${e.message}", e)
        }
    }

    suspend fun getNoteById(noteId: String): Note? {
        return noteDao.getNoteById(noteId)?.toNote()
    }

    fun cleanUp() {
        listener?.let {
            Log.d("NotesRepository", "Removing listener...")
            getNotesRef().removeEventListener(it)
        }
        listener = null
    }
    
    suspend fun clearLocalData() {
        noteDao.deleteAllNotes()
    }
}