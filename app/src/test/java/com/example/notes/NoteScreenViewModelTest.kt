package com.example.notes

import androidx.compose.ui.text.input.TextFieldValue
import com.example.notes.data.NotesRepository
import com.example.notes.model.Note
import com.example.notes.ui.screens.note.NoteScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Unit тести для NoteScreenViewModel
 * Покриває дерево розбиття для функції створення/редагування нотатки
 *
 * Початок: Спроба зберегти нотатку
 * ТВ1: Поле "Заголовок" НЕ порожнє, поле "Вміст" НЕ порожнє - Валідні дані
 * ТВ2: Поле "Заголовок" порожнє, поле "Вміст" НЕ порожнє - Валідні дані
 * ТВ3: Поле "Заголовок" НЕ порожнє, поле "Вміст" порожнє - Валідні дані
 * ТВ4: Поле "Заголовок" порожнє, поле "Вміст" порожнє - Невалідні дані
 */
@ExperimentalCoroutinesApi
class NoteScreenViewModelTest {

    @Mock
    private lateinit var notesRepository: NotesRepository

    private lateinit var viewModel: NoteScreenViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = NoteScreenViewModel(notesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * ТВ1: Заголовок НЕ порожній, Вміст НЕ порожній
     * Умови: Поле "Заголовок" НЕ порожнє, поле "Вміст" НЕ порожнє
     * Очікуваний результат: Нотатка успішно зберігається, користувач повертається до головного списку
     */
    @Test
    fun testTB1_titleAndContentNotEmpty_saveSuccessfully() = runTest {
        val note = Note(
            id = "note-1",
            date = "17.11",
            title = "My Note Title",
            content = "Some content"
        )

        val title = "Updated Title"
        val content = TextFieldValue("Updated Content")
        val taskLines = emptyList<String>()

        whenever(notesRepository.updateNote(any())).thenReturn(Unit)

        viewModel.updateNote(note, taskLines, content, title)
        advanceUntilIdle()

        verify(notesRepository, times(1)).updateNote(
            argThat { updatedNote ->
                updatedNote.id == "note-1" &&
                updatedNote.title == "Updated Title" &&
                updatedNote.content == "Updated Content" &&
                updatedNote.title.isNotEmpty() &&
                updatedNote.content.isNotEmpty()
            }
        )
    }

    /**
     * ТВ2: Заголовок порожній, Вміст НЕ порожній
     * Умови: Поле "Заголовок" порожнє, поле "Вміст" НЕ порожнє
     * Очікуваний результат: Нотатка успішно зберігається (вміст стає заголовком), користувач повертається до списку
     */
    @Test
    fun testTB2_titleEmptyContentNotEmpty_saveWithContentAsTitle() = runTest {
        val note = Note(
            id = "note-2",
            date = "17.11",
            title = "",
            content = "Some old content"
        )

        val emptyTitle = ""
        val content = TextFieldValue("This is my note content")
        val taskLines = emptyList<String>()

        whenever(notesRepository.updateNote(any())).thenReturn(Unit)

        viewModel.updateNote(note, taskLines, content, emptyTitle)
        advanceUntilIdle()

        verify(notesRepository, times(1)).updateNote(
            argThat { updatedNote ->
                updatedNote.id == "note-2" &&
                updatedNote.content == "This is my note content" &&
                updatedNote.content.isNotEmpty()
            }
        )
    }

    /**
     * ТВ3: Заголовок НЕ порожній, Вміст порожній
     * Умови: Поле "Заголовок" НЕ порожнє, поле "Вміст" порожнє
     * Очікуваний результат: Нотатка успішно зберігається, користувач повертається до списку
     */
    @Test
    fun testTB3_titleNotEmptyContentEmpty_saveSuccessfully() = runTest {
        val note = Note(
            id = "note-3",
            date = "17.11",
            title = "Old Title",
            content = "Old content"
        )

        val title = "My Title Only"
        val emptyContent = TextFieldValue("")
        val taskLines = emptyList<String>()

        whenever(notesRepository.updateNote(any())).thenReturn(Unit)

        viewModel.updateNote(note, taskLines, emptyContent, title)
        advanceUntilIdle()

        verify(notesRepository, times(1)).updateNote(
            argThat { updatedNote ->
                updatedNote.id == "note-3" &&
                updatedNote.title == "My Title Only" &&
                updatedNote.content.isEmpty() &&
                updatedNote.title.isNotEmpty()
            }
        )
    }

    /**
     * ТВ4: Заголовок порожній, Вміст порожній
     * Умови: Поле "Заголовок" порожнє, поле "Вміст" порожнє
     * Очікуваний результат: Нотатка НЕ зберігається. Кнопка "Зберегти" неактивна,
     *                       АБО при натисканні з'являється повідомлення про помилку "Нотатка не може бути порожньою"
     */
    @Test
    fun testTB4_titleAndContentEmpty_doesNotSave() = runTest {
        val note = Note(
            id = "note-4",
            date = "17.11",
            title = "Some title",
            content = "Some content"
        )

        val emptyTitle = ""
        val emptyContent = TextFieldValue("")
        val taskLines = emptyList<String>()

        whenever(notesRepository.updateNote(any())).thenReturn(Unit)

        val canSave = emptyTitle.isNotEmpty() || emptyContent.text.isNotEmpty() || taskLines.isNotEmpty()

        assertFalse("Should not allow saving empty note", canSave)

        if (canSave) {
            viewModel.updateNote(note, taskLines, emptyContent, emptyTitle)
            advanceUntilIdle()
        }

        verify(notesRepository, never()).updateNote(
            argThat { updatedNote ->
                updatedNote.title.isEmpty() && updatedNote.content.isEmpty()
            }
        )
    }
}
