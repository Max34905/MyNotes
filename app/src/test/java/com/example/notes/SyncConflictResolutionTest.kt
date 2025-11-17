package com.example.notes

import androidx.test.core.app.ApplicationProvider
import com.example.notes.data.NotesRepository
import com.example.notes.model.Note
import com.example.notes.ui.screens.list.NotesListScreenViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Unit тести для обробки конфліктів синхронізації
 *
 * Дерево 3: Обробка Конфліктів Синхронізації (Advanced)
 * ТВ20: Конфлікт редагування vs. видалення
 * ТВ21: Конфлікт редагування vs. редагування
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SyncConflictResolutionTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * ТВ20: Конфлікт редагування vs. видалення
     * Умови: Користувач редагує нотатку A офлайн, у цей же час нотатка A видаляється на іншому пристрої
     * ОР: Перевіряємо стратегію вирішення (видалення "перемагає" АБО нотатка відновлюється)
     */
    @Test
    fun testTB20_conflictEditVsDelete_deletionWins() = runTest {
        val localEditedNote = Note(
            id = "conflict-note-1",
            date = "17.11",
            title = "Edited Offline",
            content = "Local changes"
        )

        val testRepository = mock<NotesRepository>()
        val notesFlow = MutableStateFlow(listOf(localEditedNote))

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        assertEquals("Should have edited note locally", 1, testViewModel.notes.value.size)

        notesFlow.value = emptyList()
        advanceUntilIdle()

        assertTrue("Deleted note should not appear after sync", testViewModel.notes.value.isEmpty())
        assertEquals("List should be empty after conflict resolution", 0, testViewModel.notes.value.size)
    }

    /**
     * ТВ10: Конфлікт редагування vs. редагування
     * Умови: Користувач редагує нотатку A офлайн, у цей же час нотатка A редагується на іншому пристрої
     * ОР: Перевіряємо стратегію (серверна версія "перемагає" АБО останні зміни)
     */
    @Test
    fun testTB121_conflictEditVsEdit_serverVersionWins() = runTest {
        val localEditedNote = Note(
            id = "conflict-note-2",
            date = "17.11",
            title = "Local Edit",
            content = "Edited on device"
        )

        val testRepository = mock<NotesRepository>()
        val notesFlow = MutableStateFlow(listOf(localEditedNote))

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        assertEquals("Should have local edited version", "Local Edit", testViewModel.notes.value[0].title)

        val serverEditedNote = Note(
            id = "conflict-note-2",
            date = "17.11",
            title = "Server Edit",
            content = "Edited on another device"
        )

        notesFlow.value = listOf(serverEditedNote)
        advanceUntilIdle()

        assertEquals("Should have 1 note after conflict resolution", 1, testViewModel.notes.value.size)
        assertEquals("Server version should win", "Server Edit", testViewModel.notes.value[0].title)
        assertEquals("Server content should be applied", "Edited on another device", testViewModel.notes.value[0].content)
    }
}

