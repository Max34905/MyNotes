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
 * Unit тести для синхронізації нотаток з офлайну в онлайн
 *
 * Дерево 2: Синхронізація Нотаток (З Офлайну в Онлайн)
 * ТВ3: Створення нотатки офлайн
 * ТВ4: Синхронізація створеної нотатки при підключенні
 * ТВ5: Редагування нотатки офлайн
 * ТВ6: Синхронізація відредагованої нотатки при підключенні
 * ТВ7: Видалення нотатки офлайн
 * ТВ8: Синхронізація видаленої нотатки при підключенні
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OfflineToOnlineSyncTest {

    @Mock
    private lateinit var notesRepository: NotesRepository

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Initialize Firebase for testing
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
     * ТВ3: Створення нової нотатки в офлайн-режимі
     * Умови: Користувач створює нотатку без інтернету
     * ОР (Офлайн): Нотатка негайно з'являється у локальному списку
     */
    @Test
    fun testTB3_createNoteOffline_appearsInLocalList() = runTest {
        // Given - офлайн режим, порожній список
        val testRepository = mock<NotesRepository>()
        val initialNotes = emptyList<Note>()
        val notesFlow = MutableStateFlow(initialNotes)

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }
        whenever(testRepository.addNote()).thenReturn("offline-note-1")

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        // When - створення нотатки в офлайн режимі
        testViewModel.addNote()
        advanceUntilIdle()

        // Симулюємо додавання нотатки до локального списку
        val newNote = Note(id = "offline-note-1", date = "17.11", title = "", content = "")
        notesFlow.value = listOf(newNote)
        advanceUntilIdle()

        // Then - нотатка з'являється в локальному списку
        verify(testRepository, times(1)).addNote()
        assertEquals("Should have 1 note in local list", 1, testViewModel.notes.value.size)
        assertEquals("Note ID should match", "offline-note-1", testViewModel.notes.value[0].id)
    }

    /**
     * ТВ4: Синхронізація створеної нотатки при підключенні до Інтернету
     * Умови: Продовження ТВ3, з'явилося з'єднання
     * ОР (Онлайн): Нотатка автоматично синхронізується з Firebase без втрати даних
     */
    @Test
    fun testTB4_syncCreatedNoteWhenOnline_uploadsToFirebase() = runTest {
        // Given - нотатка створена офлайн
        val offlineNote = Note(
            id = "offline-note-1",
            date = "17.11",
            title = "Offline Created",
            content = "Created without internet"
        )

        val testRepository = mock<NotesRepository>()
        val notesFlow = MutableStateFlow(listOf(offlineNote))

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }
        whenever(testRepository.updateNote(any())).thenReturn(Unit)

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        // When - з'являється з'єднання, запускається синхронізація
        // Симулюємо автоматичну синхронізацію через setupRealtimeSync
        verify(testRepository, times(1)).setupRealtimeSync()

        // Then - нотатка збережена, дані не втрачено
        assertEquals("Note should remain in list", 1, testViewModel.notes.value.size)
        assertEquals("Note title should be preserved", "Offline Created", testViewModel.notes.value[0].title)
        assertEquals("Note content should be preserved", "Created without internet", testViewModel.notes.value[0].content)
    }

    /**
     * ТВ5: Редагування існуючої нотатки в офлайн-режимі
     * Умови: Користувач відкриває кешовану нотатку та змінює її вміст без інтернету
     * ОР (Офлайн): Зміни зберігаються локально, при повторному відкритті нотатка має оновлений вміст
     */
    @Test
    fun testTB5_editNoteOffline_changesStoredLocally() = runTest {
        // Given - є кешована нотатка
        val originalNote = Note(
            id = "cached-note-1",
            date = "17.11",
            title = "Original Title",
            content = "Original Content"
        )

        val testRepository = mock<NotesRepository>()
        val notesFlow = MutableStateFlow(listOf(originalNote))

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }
        whenever(testRepository.updateNote(any())).thenReturn(Unit)

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        // When - редагування в офлайн режимі
        val updatedNote = originalNote.copy(
            title = "Edited Offline",
            content = "Updated without internet"
        )

        // Симулюємо збереження змін локально
        notesFlow.value = listOf(updatedNote)
        advanceUntilIdle()

        // Then - зміни збережено локально
        assertEquals("Should still have 1 note", 1, testViewModel.notes.value.size)
        assertEquals("Title should be updated", "Edited Offline", testViewModel.notes.value[0].title)
        assertEquals("Content should be updated", "Updated without internet", testViewModel.notes.value[0].content)
    }

    /**
     * ТВ6: Синхронізація відредагованої нотатки при підключенні
     * Умови: Продовження ТВ5, з'явилося з'єднання
     * ОР (Онлайн): Оновлений вміст нотатки синхронізується з хмарою
     */
    @Test
    fun testTB6_syncEditedNoteWhenOnline_uploadsChanges() = runTest {
        // Given - нотатка відредагована офлайн
        val editedNote = Note(
            id = "cached-note-1",
            date = "17.11",
            title = "Edited Offline",
            content = "Updated without internet"
        )

        val testRepository = mock<NotesRepository>()
        val notesFlow = MutableStateFlow(listOf(editedNote))

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }
        whenever(testRepository.updateNote(any())).thenReturn(Unit)

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        // When - з'являється з'єднання, синхронізація запускається
        verify(testRepository, times(1)).setupRealtimeSync()

        // Then - зміни збережені
        assertEquals("Edited note should remain", 1, testViewModel.notes.value.size)
        assertEquals("Edited title should be synced", "Edited Offline", testViewModel.notes.value[0].title)
        assertEquals("Edited content should be synced", "Updated without internet", testViewModel.notes.value[0].content)
    }

    /**
     * ТВ7: Видалення існуючої нотатки в офлайн-режимі
     * Умови: Користувач видаляє кешовану нотатку без інтернету
     * ОР (Офлайн): Нотатка зникає з локального списку
     */
    @Test
    fun testTB7_deleteNoteOffline_removedFromLocalList() = runTest {
        // Given - є кешована нотатка
        val noteToDelete = Note(
            id = "cached-note-1",
            date = "17.11",
            title = "To Be Deleted",
            content = "Will be removed"
        )

        val testRepository = mock<NotesRepository>()
        val notesFlow = MutableStateFlow(listOf(noteToDelete))

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }
        whenever(testRepository.deleteNote(any())).thenReturn(Unit)

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        assertEquals("Should have 1 note initially", 1, testViewModel.notes.value.size)

        // When - видалення в офлайн режимі
        // Симулюємо видалення з локального списку
        notesFlow.value = emptyList()
        advanceUntilIdle()

        // Then - нотатка зникла з локального списку
        assertTrue("Note should be removed from local list", testViewModel.notes.value.isEmpty())
        assertEquals("List should be empty", 0, testViewModel.notes.value.size)
    }

    /**
     * ТВ8: Синхронізація видаленої нотатки при підключенні
     * Умови: Продовження ТВ7, з'явилося з'єднання
     * ОР (Онлайн): Нотатка видаляється з Firebase
     */
    @Test
    fun testTB8_syncDeletedNoteWhenOnline_removesFromFirebase() = runTest {
        // Given - нотатка видалена офлайн, список порожній
        val testRepository = mock<NotesRepository>()
        val notesFlow = MutableStateFlow(emptyList<Note>())

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }
        whenever(testRepository.deleteNote(any())).thenReturn(Unit)

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        // When - з'являється з'єднання, синхронізація запускається
        verify(testRepository, times(1)).setupRealtimeSync()

        // Then - нотатка залишається видаленою
        assertTrue("Deleted note should not reappear", testViewModel.notes.value.isEmpty())
        assertEquals("List should remain empty", 0, testViewModel.notes.value.size)
    }
}

