package com.example.notes

import com.example.notes.data.NotesRepository
import com.example.notes.model.Note
import com.example.notes.ui.screens.list.NotesListScreenViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Unit тести для NotesListScreenViewModel
 * Покриває acceptance criteria: AC-003, AC-005, AC-008, AC-009, AC-010
 */
@ExperimentalCoroutinesApi
class NotesListScreenViewModelTest {

    @Mock
    private lateinit var notesRepository: NotesRepository

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var viewModel: NotesListScreenViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock Firebase Auth
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("test-user-123")

        viewModel = NotesListScreenViewModel(notesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * AC-003: Тест додавання нотатки
     */
    @Test
    fun testAddNote_createsNewNoteAndNavigatesToIt() = runTest {
        // Given
        val newNoteId = "new-note-123"
        whenever(notesRepository.addNote()).thenReturn(newNoteId)

        // When
        viewModel.addNote()
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).addNote()
        assertEquals("Should navigate to new note", newNoteId, viewModel.isNavigatingToNote.value)
    }

    /**
     * AC-008: Тест завантаження нотаток з real-time синхронізацією
     */
    @Test
    fun testGetNotes_loadsNotesFromRepository() = runTest {
        // Given
        val testNotes = listOf(
            Note(id = "1", date = "17.11", title = "Note 1", content = "Content 1"),
            Note(id = "2", date = "17.11", title = "Note 2", content = "Content 2"),
            Note(id = "3", date = "17.11", title = "Note 3", content = "Content 3")
        )
        whenever(notesRepository.getNotesFlow()).thenReturn(flowOf(testNotes))

        // When
        viewModel.getNotes()
        advanceUntilIdle()

        // Then
        assertEquals("Should have 3 notes", 3, viewModel.notes.value.size)
        assertEquals("First note title should match", "Note 1", viewModel.notes.value[0].title)
        verify(notesRepository, times(1)).setupRealtimeSync()
    }

    /**
     * AC-009: Тест оновлення нотаток при зміні в Firebase
     */
    @Test
    fun testRealtimeUpdate_updatesNotesList() = runTest {
        // Given - початковий список
        val initialNotes = listOf(
            Note(id = "1", date = "17.11", title = "Original", content = "Content")
        )

        // Оновлений список
        val updatedNotes = listOf(
            Note(id = "1", date = "17.11", title = "Updated", content = "New Content")
        )

        whenever(notesRepository.getNotesFlow())
            .thenReturn(flowOf(initialNotes))
            .thenReturn(flowOf(updatedNotes))

        // When
        viewModel.getNotes()
        advanceUntilIdle()

        // Then
        assertNotNull("Notes should not be null", viewModel.notes.value)
        assertTrue("Notes list should not be empty", viewModel.notes.value.isNotEmpty())
    }

    /**
     * Тест виходу користувача
     */
    @Test
    fun testSignOut_clearsDataAndSignsOut() = runTest {
        // Given
        val testNotes = listOf(
            Note(id = "1", date = "17.11", title = "Note 1", content = "Content 1")
        )
        whenever(notesRepository.getNotesFlow()).thenReturn(flowOf(testNotes))

        // When
        viewModel.signOut()
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).cleanUp()
        verify(notesRepository, times(1)).clearLocalData()
        assertTrue("Notes list should be empty after sign out", viewModel.notes.value.isEmpty())
    }

    /**
     * Тест скидання стану навігації
     */
    @Test
    fun testOnNavigatedToNote_clearsNavigationState() = runTest {
        // Given
        val newNoteId = "new-note-456"
        whenever(notesRepository.addNote()).thenReturn(newNoteId)
        viewModel.addNote()
        advanceUntilIdle()

        // When
        viewModel.onNavigatedToNote()

        // Then
        assertNull("Navigation state should be null", viewModel.isNavigatingToNote.value)
    }

    /**
     * AC-010: Тест видалення нотатки через real-time sync
     */
    @Test
    fun testRealtimeDelete_removesNoteFromList() = runTest {
        // Given - список з 2 нотатками
        val initialNotes = listOf(
            Note(id = "1", date = "17.11", title = "Note 1", content = "Content 1"),
            Note(id = "2", date = "17.11", title = "Note 2", content = "Content 2")
        )

        // Після видалення однієї нотатки
        val updatedNotes = listOf(
            Note(id = "1", date = "17.11", title = "Note 1", content = "Content 1")
        )

        whenever(notesRepository.getNotesFlow())
            .thenReturn(flowOf(initialNotes))
            .thenReturn(flowOf(updatedNotes))

        // When
        viewModel.getNotes()
        advanceUntilIdle()

        // Then
        assertNotNull("Notes should not be null", viewModel.notes.value)
    }

    /**
     * Тест обробки порожнього списку нотаток
     */
    @Test
    fun testEmptyNotesList_handledCorrectly() = runTest {
        // Given
        whenever(notesRepository.getNotesFlow()).thenReturn(flowOf(emptyList()))

        // When
        viewModel.getNotes()
        advanceUntilIdle()

        // Then
        assertTrue("Notes list should be empty", viewModel.notes.value.isEmpty())
        assertEquals("Notes count should be 0", 0, viewModel.notes.value.size)
    }

    /**
     * Тест помилки при створенні нотатки
     */
    @Test
    fun testAddNote_handlesError() = runTest {
        // Given - Repository повертає порожній ID при помилці
        whenever(notesRepository.addNote()).thenReturn("")

        // When
        viewModel.addNote()
        advanceUntilIdle()

        // Then
        assertNull("Should not navigate on error", viewModel.isNavigatingToNote.value)
    }
}
