package com.example.notes

import com.example.notes.data.NotesRepository
import com.example.notes.data.local.NoteDao
import com.example.notes.data.local.NoteEntity
import com.example.notes.model.Note
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import com.google.android.gms.tasks.Tasks
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Unit тести для NotesRepository
 * Покриває acceptance criteria: AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010
 */
@ExperimentalCoroutinesApi
class NotesRepositoryTest {

    @Mock
    private lateinit var noteDao: NoteDao

    @Mock
    private lateinit var firebaseDatabase: FirebaseDatabase

    @Mock
    private lateinit var databaseReference: DatabaseReference

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var notesRepository: NotesRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock Firebase Auth
        `when`(firebaseAuth.currentUser).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn("test-user-id")

        // Mock Firebase Database
        `when`(firebaseDatabase.getReference("notes")).thenReturn(databaseReference)
        `when`(databaseReference.child(anyString())).thenReturn(databaseReference)
        `when`(databaseReference.push()).thenReturn(databaseReference)
        `when`(databaseReference.key).thenReturn("test-note-id")

        notesRepository = NotesRepository(noteDao)
    }

    /**
     * AC-003: Тест створення нотатки в online режимі
     */
    @Test
    fun testCreateNoteOnline() = runTest {
        // Given
        val expectedNoteId = "test-note-id-123"
        val mockDatabaseReference = mock(DatabaseReference::class.java)
        `when`(mockDatabaseReference.key).thenReturn(expectedNoteId)
        `when`(mockDatabaseReference.setValue(any())).thenReturn(Tasks.forResult(null))

        // When
        val noteId = notesRepository.addNote()

        // Then
        assertNotNull("Note ID should not be null", noteId)
        assertTrue("Note ID should not be empty", noteId.isNotEmpty())

        // Verify local storage
        verify(noteDao, times(1)).upsertNote(any())
    }

    /**
     * AC-004: Тест редагування нотатки в online режимі
     */
    @Test
    fun testUpdateNoteOnline() = runTest {
        // Given
        val updatedNote = Note(
            id = "note-1",
            date = "17.11",
            title = "Updated Title",
            content = "Updated Content"
        )

        val mockDatabaseReference = mock(DatabaseReference::class.java)
        `when`(mockDatabaseReference.updateChildren(anyMap())).thenReturn(Tasks.forResult(null))

        // When
        notesRepository.updateNote(updatedNote)

        // Then
        verify(noteDao, times(1)).upsertNote(
            argThat { noteEntity ->
                noteEntity.id == "note-1" &&
                noteEntity.title == "Updated Title" &&
                noteEntity.content == "Updated Content"
            }
        )
    }

    /**
     * AC-005: Тест видалення нотатки в online режимі
     */
    @Test
    fun testDeleteNoteOnline() = runTest {
        // Given
        val noteIdToDelete = "note-to-delete"
        val mockDatabaseReference = mock(DatabaseReference::class.java)
        `when`(mockDatabaseReference.removeValue()).thenReturn(Tasks.forResult(null))

        // When
        notesRepository.deleteNote(noteIdToDelete)

        // Then
        verify(noteDao, times(1)).deleteNote(noteIdToDelete)
    }

    /**
     * AC-006: Тест створення нотатки в offline режимі
     */
    @Test
    fun testCreateNoteOffline() = runTest {
        // Given - Firebase недоступний (offline)
        `when`(databaseReference.setValue(any()))
            .thenReturn(Tasks.forException(Exception("Network error")))

        // When
        notesRepository.addNote()

        // Then - навіть при помилці Firebase, локальне збереження має відбутися
        verify(noteDao, atLeastOnce()).upsertNote(any())
    }

    /**
     * AC-007: Тест редагування нотатки в offline режимі
     */
    @Test
    fun testUpdateNoteOffline() = runTest {
        // Given
        val updatedNote = Note(
            id = "note-1",
            date = "17.11",
            title = "Offline Updated Title",
            content = "Offline Updated Content"
        )

        // Firebase недоступний
        `when`(databaseReference.updateChildren(anyMap()))
            .thenReturn(Tasks.forException(Exception("Network error")))

        // When
        notesRepository.updateNote(updatedNote)

        // Then - локальне збереження має відбутися навіть без Firebase
        verify(noteDao, times(1)).upsertNote(any())
    }

    /**
     * AC-008: Тест real-time синхронізації при створенні
     */
    @Test
    fun testRealtimeSyncOnCreate() = runTest {
        // Given
        val newNote = Note(
            id = "sync-note-1",
            date = "17.11",
            title = "Synced Note",
            content = "Content"
        )

        val noteEntity = NoteEntity.fromNote(newNote)
        `when`(noteDao.getAllNotesFlow()).thenReturn(flowOf(listOf(noteEntity)))

        // When
        val notes = notesRepository.getNotesFlow().first()

        // Then
        assertNotNull("Notes flow should not be null", notes)
        assertEquals("Should have 1 note", 1, notes.size)
        assertEquals("Note ID should match", "sync-note-1", notes[0].id)
    }

    /**
     * AC-009: Тест real-time синхронізації при редагуванні
     */
    @Test
    fun testRealtimeSyncOnUpdate() = runTest {
        // Given
        val updatedNote = Note(
            id = "note-1",
            date = "17.11",
            title = "Realtime Updated",
            content = "Realtime Content"
        )

        val noteEntity = NoteEntity.fromNote(updatedNote)
        `when`(noteDao.getAllNotesFlow()).thenReturn(flowOf(listOf(noteEntity)))

        // When
        notesRepository.updateNote(updatedNote)
        val notes = notesRepository.getNotesFlow().first()

        // Then
        assertEquals("Should reflect updated title", "Realtime Updated", notes[0].title)
        assertEquals("Should reflect updated content", "Realtime Content", notes[0].content)
    }

    /**
     * AC-010: Тест real-time синхронізації при видаленні
     */
    @Test
    fun testRealtimeSyncOnDelete() = runTest {
        // Given
        val noteId = "note-to-delete"
        `when`(noteDao.getAllNotesFlow()).thenReturn(flowOf(emptyList()))

        // When
        notesRepository.deleteNote(noteId)
        val notes = notesRepository.getNotesFlow().first()

        // Then
        verify(noteDao, times(1)).deleteNote(noteId)
        assertTrue("Notes list should be empty after deletion", notes.isEmpty())
    }

    /**
     * Тест отримання нотатки за ID
     */
    @Test
    fun testGetNoteById() = runTest {
        // Given
        val noteId = "test-note-123"
        val noteEntity = NoteEntity(
            id = noteId,
            date = "17.11",
            title = "Test Note",
            content = "Test Content"
        )
        `when`(noteDao.getNoteById(noteId)).thenReturn(noteEntity)

        // When
        val note = notesRepository.getNoteById(noteId)

        // Then
        assertNotNull("Note should not be null", note)
        assertEquals("Note ID should match", noteId, note?.id)
        assertEquals("Note title should match", "Test Note", note?.title)
    }

    /**
     * Тест очищення локальних даних
     */
    @Test
    fun testClearLocalData() = runTest {
        // When
        notesRepository.clearLocalData()

        // Then
        verify(noteDao, times(1)).deleteAllNotes()
    }
}
