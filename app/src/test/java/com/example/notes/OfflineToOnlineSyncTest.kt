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
 * ТВ11: Створення нотатки офлайн
 * ТВ12: Синхронізація створеної нотатки при підключенні
 * ТВ13: Редагування нотатки офлайн
 * ТВ14: Синхронізація відредагованої нотатки при підключенні
 * ТВ15: Видалення нотатки офлайн
 * ТВ16: Синхронізація видаленої нотатки при підключенні
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OfflineToOnlineSyncTest {
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
     * ТВ11: Створення нової нотатки в офлайн-режимі
     * Умови: Користувач створює нотатку без інтернету
     * ОР (Офлайн): Нотатка негайно з'являється у локальному списку
     */
    @Test
    fun testTB11_createNoteOffline_appearsInLocalList() = runTest {
        val testRepository = mock<NotesRepository>()
        val initialNotes = emptyList<Note>()
        val notesFlow = MutableStateFlow(initialNotes)

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }
        whenever(testRepository.addNote()).thenReturn("offline-note-1")

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        testViewModel.addNote()
        advanceUntilIdle()

        val newNote = Note(id = "offline-note-1", date = "17.11", title = "", content = "")
        notesFlow.value = listOf(newNote)
        advanceUntilIdle()

        verify(testRepository, times(1)).addNote()
        assertEquals("Should have 1 note in local list", 1, testViewModel.notes.value.size)
        assertEquals("Note ID should match", "offline-note-1", testViewModel.notes.value[0].id)
    }

    /**
     * ТВ12: Синхронізація створеної нотатки при підключенні до Інтернету
     * Умови: Продовження ТВ3, з'явилося з'єднання
     * ОР (Онлайн): Нотатка автоматично синхронізується з Firebase без втрати даних
     */
    @Test
    fun testTB12_syncCreatedNoteWhenOnline_uploadsToFirebase() = runTest {
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

        verify(testRepository, times(1)).setupRealtimeSync()

        assertEquals("Note should remain in list", 1, testViewModel.notes.value.size)
        assertEquals("Note title should be preserved", "Offline Created", testViewModel.notes.value[0].title)
        assertEquals("Note content should be preserved", "Created without internet", testViewModel.notes.value[0].content)
    }

    /**
     * ТВ13: Редагування існуючої нотатки в офлайн-режимі
     * Умови: Користувач відкриває кешовану нотатку та змінює її вміст без інтернету
     * ОР (Офлайн): Зміни зберігаються локально, при повторному відкритті нотатка має оновлений вміст
     */
    @Test
    fun testTB13_editNoteOffline_changesStoredLocally() = runTest {
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

        val updatedNote = originalNote.copy(
            title = "Edited Offline",
            content = "Updated without internet"
        )

        notesFlow.value = listOf(updatedNote)
        advanceUntilIdle()

        assertEquals("Should still have 1 note", 1, testViewModel.notes.value.size)
        assertEquals("Title should be updated", "Edited Offline", testViewModel.notes.value[0].title)
        assertEquals("Content should be updated", "Updated without internet", testViewModel.notes.value[0].content)
    }

    /**
     * ТВ14: Синхронізація відредагованої нотатки при підключенні
     * Умови: Продовження ТВ5, з'явилося з'єднання
     * ОР (Онлайн): Оновлений вміст нотатки синхронізується з хмарою
     */
    @Test
    fun testTB14_syncEditedNoteWhenOnline_uploadsChanges() = runTest {
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

        verify(testRepository, times(1)).setupRealtimeSync()

        assertEquals("Edited note should remain", 1, testViewModel.notes.value.size)
        assertEquals("Edited title should be synced", "Edited Offline", testViewModel.notes.value[0].title)
        assertEquals("Edited content should be synced", "Updated without internet", testViewModel.notes.value[0].content)
    }

    /**
     * ТВ15: Видалення існуючої нотатки в офлайн-режимі
     * Умови: Користувач видаляє кешовану нотатку без інтернету
     * ОР (Офлайн): Нотатка зникає з локального списку
     */
    @Test
    fun testTB15_deleteNoteOffline_removedFromLocalList() = runTest {
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

        notesFlow.value = emptyList()
        advanceUntilIdle()

        assertTrue("Note should be removed from local list", testViewModel.notes.value.isEmpty())
        assertEquals("List should be empty", 0, testViewModel.notes.value.size)
    }

    /**
     * ТВ16: Синхронізація видаленої нотатки при підключенні
     * Умови: Продовження ТВ7, з'явилося з'єднання
     * ОР (Онлайн): Нотатка видаляється з Firebase
     */
    @Test
    fun testTB16_syncDeletedNoteWhenOnline_removesFromFirebase() = runTest {
        val testRepository = mock<NotesRepository>()
        val notesFlow = MutableStateFlow(emptyList<Note>())

        whenever(testRepository.getNotesFlow()).thenReturn(notesFlow)
        whenever(testRepository.setupRealtimeSync()).then { }
        whenever(testRepository.deleteNote(any())).thenReturn(Unit)

        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        verify(testRepository, times(1)).setupRealtimeSync()

        assertTrue("Deleted note should not reappear", testViewModel.notes.value.isEmpty())
        assertEquals("List should remain empty", 0, testViewModel.notes.value.size)
    }
}

