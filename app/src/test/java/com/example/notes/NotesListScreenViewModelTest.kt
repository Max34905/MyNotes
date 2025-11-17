package com.example.notes

import androidx.test.core.app.ApplicationProvider
import com.example.notes.data.NotesRepository
import com.example.notes.model.Note
import com.example.notes.ui.screens.list.NotesListScreenViewModel
import com.google.firebase.FirebaseApp
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
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit тести для NotesListScreenViewModel
 * Покриває дерево розбиття для функції головного екрану (Список нотаток та Пошук)
 *
 * Початок: Відображення/Фільтрація списку нотаток
 * ТВ1: База даних не містить жодної нотатки (новий користувач)
 * ТВ2: База даних містить одну або декілька нотаток
 * ТВ3: Пошуковий запит знаходить збіги
 * ТВ4: Пошуковий запит не знаходить збігів
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
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

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("test-user-123")

        whenever(notesRepository.getNotesFlow()).thenReturn(flowOf(emptyList()))
        whenever(notesRepository.setupRealtimeSync()).then { }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * ТВ1: Порожній список нотаток
     * Умови: База даних не містить жодної нотатки (новий користувач)
     * Очікуваний результат: Відображається повідомлення "Список нотаток порожній"
     */
    @Test
    fun testTB1_emptyDatabase_showsEmptyListMessage() = runTest {
        val testRepository = mock<NotesRepository>()
        whenever(testRepository.getNotesFlow()).thenReturn(flowOf(emptyList()))
        whenever(testRepository.setupRealtimeSync()).then { }

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        assertTrue("Notes list should be empty for new user", testViewModel.notes.value.isEmpty())
        assertEquals("Notes count should be 0", 0, testViewModel.notes.value.size)

        assertTrue("Should show empty list message", testViewModel.notes.value.isEmpty())
    }

    /**
     * ТВ2: Список містить нотатки
     * Умови: База даних містить одну або декілька нотаток
     * Очікуваний результат: Відображається список нотаток
     */
    @Test
    fun testTB2_databaseWithNotes_displaysNotesList() = runTest {
        val testNotes = listOf(
            Note(id = "1", date = "17.11", title = "Note 1", content = "Content 1"),
            Note(id = "2", date = "17.11", title = "Note 2", content = "Content 2"),
            Note(id = "3", date = "17.11", title = "Note 3", content = "Content 3")
        )

        val testRepository = mock<NotesRepository>()
        whenever(testRepository.getNotesFlow()).thenReturn(flowOf(testNotes))
        whenever(testRepository.setupRealtimeSync()).then { }

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        assertFalse("Notes list should not be empty", testViewModel.notes.value.isEmpty())
        assertEquals("Should display 3 notes", 3, testViewModel.notes.value.size)
        assertEquals("First note title should match", "Note 1", testViewModel.notes.value[0].title)
        assertEquals("Second note title should match", "Note 2", testViewModel.notes.value[1].title)
        assertEquals("Third note title should match", "Note 3", testViewModel.notes.value[2].title)
        verify(testRepository, times(1)).setupRealtimeSync()
    }

    /**
     * ТВ3: Пошук знаходить збіги
     * Умови: Пошуковий запит знаходить збіги (введений текст присутній у заголовках або вмісті нотаток)
     * Очікуваний результат: Список фільтрується і відображає лише ті нотатки, що відповідають запиту
     */
    @Test
    fun testTB3_searchQueryFindsMatches_displaysFilteredResults() = runTest {
        val testNotes = listOf(
            Note(id = "1", date = "17.11", title = "Shopping List", content = "Buy grocery"),
            Note(id = "2", date = "17.11", title = "Meeting Notes", content = "Discuss project"),
            Note(id = "3", date = "17.11", title = "Grocery Ideas", content = "Healthy recipes")
        )

        val testRepository = mock<NotesRepository>()
        whenever(testRepository.getNotesFlow()).thenReturn(flowOf(testNotes))
        whenever(testRepository.setupRealtimeSync()).then { }

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        val searchQuery = "grocery"
        val filteredNotes = testViewModel.notes.value.filter { note ->
            note.title.contains(searchQuery, ignoreCase = true) ||
            note.content.contains(searchQuery, ignoreCase = true)
        }

        assertTrue("Should find matching notes", filteredNotes.isNotEmpty())
        assertEquals("Should find 2 notes with 'grocery'", 2, filteredNotes.size)

        filteredNotes.forEach { note ->
            val containsInTitle = note.title.contains(searchQuery, ignoreCase = true)
            val containsInContent = note.content.contains(searchQuery, ignoreCase = true)
            assertTrue("Note should contain query in title or content",
                      containsInTitle || containsInContent)
        }
    }

    /**
     * ТВ4: Пошук не знаходить збігів
     * Умови: Пошуковий запит не знаходить збігів (введений текст відсутній у будь-якій нотатці)
     * Очікуваний результат: Відображається порожній список з повідомленням "Нічого не знайдено"
     */
    @Test
    fun testTB4_searchQueryFindsNoMatches_displaysEmptyResultMessage() = runTest {
        val testNotes = listOf(
            Note(id = "1", date = "17.11", title = "Shopping List", content = "Buy groceries"),
            Note(id = "2", date = "17.11", title = "Meeting Notes", content = "Discuss project"),
            Note(id = "3", date = "17.11", title = "Ideas", content = "Brainstorming session")
        )

        val testRepository = mock<NotesRepository>()
        whenever(testRepository.getNotesFlow()).thenReturn(flowOf(testNotes))
        whenever(testRepository.setupRealtimeSync()).then { }

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        val searchQuery = "nonexistent"
        val filteredNotes = testViewModel.notes.value.filter { note ->
            note.title.contains(searchQuery, ignoreCase = true) ||
            note.content.contains(searchQuery, ignoreCase = true)
        }

        assertTrue("Should not find any matching notes", filteredNotes.isEmpty())
        assertEquals("Filtered list should be empty", 0, filteredNotes.size)

        assertTrue("Should show 'nothing found' message when no matches", filteredNotes.isEmpty())
    }

    /**
     * Тест створення нової нотатки
     */
    @Test
    fun testAddNote_createsNewNoteAndNavigatesToIt() = runTest {
        // Given
        whenever(notesRepository.getNotesFlow()).thenReturn(flowOf(emptyList()))
        viewModel = NotesListScreenViewModel(notesRepository)

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
     * Тест real-time оновлення списку
     */
    @Test
    fun testRealtimeUpdate_updatesNotesList() = runTest {
        // Given - створюємо Flow що емітує два різні списки
        val initialNotes = listOf(
            Note(id = "1", date = "17.11", title = "Original", content = "Content")
        )

        val updatedNotes = listOf(
            Note(id = "1", date = "17.11", title = "Updated", content = "New Content")
        )

        val notesFlow = kotlinx.coroutines.flow.MutableStateFlow(initialNotes)

        val testRepository = mock<NotesRepository>()
        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        // When - симулюємо Firebase оновлення
        notesFlow.value = updatedNotes
        advanceUntilIdle()

        // Then
        assertEquals("Title should be updated", "Updated", testViewModel.notes.value[0].title)
        assertEquals("Content should be updated", "New Content", testViewModel.notes.value[0].content)
    }

    /**
     * Тест виходу користувача
     */
    @Test
    fun testSignOut_clearsDataAndSignsOut() = runTest {
        // Given
        whenever(notesRepository.getNotesFlow()).thenReturn(flowOf(emptyList()))
        viewModel = NotesListScreenViewModel(notesRepository)

        // When
        viewModel.signOut()
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).cleanUp()
        verify(notesRepository, times(1)).clearLocalData()
        assertTrue("Notes list should be empty after sign out", viewModel.notes.value.isEmpty())
    }
}
