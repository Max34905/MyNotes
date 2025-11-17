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
 * Покриває acceptance criteria: AC-004, AC-005, AC-007, AC-009
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
     * Тест отримання нотатки за ID
     */
    @Test
    fun testGetNoteById_loadsNoteSuccessfully() = runTest {
        // Given
        val noteId = "test-note-123"
        val expectedNote = Note(
            id = noteId,
            date = "17.11",
            title = "Test Note",
            content = "Test Content"
        )
        whenever(notesRepository.getNoteById(noteId)).thenReturn(expectedNote)

        // When
        viewModel.getNoteById(noteId)
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).getNoteById(noteId)
        assertEquals("Note should be loaded", expectedNote, viewModel.note.value)
        assertEquals("Note title should match", "Test Note", viewModel.note.value?.title)
    }

    /**
     * AC-004: Тест оновлення нотатки (online режим)
     */
    @Test
    fun testUpdateNote_savesChangesSuccessfully() = runTest {
        // Given
        val originalNote = Note(
            id = "note-1",
            date = "17.11",
            title = "Original Title",
            content = "Original Content"
        )

        val newTitle = "Updated Title"
        val newContent = TextFieldValue("Updated Content")
        val taskLines = emptyList<String>()

        // When
        viewModel.updateNote(originalNote, taskLines, newContent, newTitle)
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).updateNote(
            argThat { note ->
                note.id == "note-1" &&
                note.title == "Updated Title" &&
                note.content == "Updated Content"
            }
        )
    }

    /**
     * AC-007: Тест редагування нотатки з задачами
     */
    @Test
    fun testUpdateNote_withTaskLines_combinesContentCorrectly() = runTest {
        // Given
        val note = Note(
            id = "note-2",
            date = "17.11",
            title = "Task Note",
            content = ""
        )

        val noteTextField = TextFieldValue("Regular text content")
        val taskLines = listOf("[ ] Task 1", "[x] Task 2")
        val title = "Updated Task Note"

        // When
        viewModel.updateNote(note, taskLines, noteTextField, title)
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).updateNote(
            argThat { updatedNote ->
                updatedNote.content.contains("Regular text content") &&
                updatedNote.content.contains("[ ] Task 1") &&
                updatedNote.content.contains("[x] Task 2")
            }
        )
    }

    /**
     * AC-005: Тест видалення нотатки
     */
    @Test
    fun testDeleteNote_deletesSuccessfully() = runTest {
        // Given
        val noteIdToDelete = "note-to-delete"

        // When
        viewModel.deleteNote(noteIdToDelete)
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).deleteNote(noteIdToDelete)
    }

    /**
     * Тест розділення контенту на текст та задачі
     */
    @Test
    fun testSplitContent_separatesTextAndTasks() {
        // Given
        val content = """
            Regular text line 1
            Regular text line 2
            [ ] Unchecked task
            [x] Checked task
            More regular text
        """.trimIndent()

        // When
        val (textContent, taskContent) = viewModel.splitContent(content)

        // Then
        assertTrue("Text should contain regular lines", textContent.contains("Regular text line 1"))
        assertTrue("Text should contain regular lines", textContent.contains("More regular text"))
        assertTrue("Tasks should contain unchecked task", taskContent.contains("[ ] Unchecked task"))
        assertTrue("Tasks should contain checked task", taskContent.contains("[x] Checked task"))
    }

    /**
     * Тест розділення контенту тільки з текстом (без задач)
     */
    @Test
    fun testSplitContent_onlyText_noTasks() {
        // Given
        val content = """
            Just regular text
            Another line of text
            No tasks here
        """.trimIndent()

        // When
        val (textContent, taskContent) = viewModel.splitContent(content)

        // Then
        assertTrue("Text should contain all content", textContent.contains("Just regular text"))
        assertTrue("Task content should be empty", taskContent.isEmpty())
    }

    /**
     * Тест розділення контенту тільки з задачами (без тексту)
     */
    @Test
    fun testSplitContent_onlyTasks_noText() {
        // Given
        val content = """
            [ ] Task 1
            [x] Task 2
            [ ] Task 3
        """.trimIndent()

        // When
        val (textContent, taskContent) = viewModel.splitContent(content)

        // Then
        assertTrue("Text content should be empty", textContent.isEmpty())
        assertTrue("Tasks should contain all task lines", taskContent.contains("[ ] Task 1"))
        assertTrue("Tasks should contain all task lines", taskContent.contains("[x] Task 2"))
    }

    /**
     * AC-009: Тест оновлення нотатки з порожнім контентом
     */
    @Test
    fun testUpdateNote_withEmptyContent_savesCorrectly() = runTest {
        // Given
        val note = Note(
            id = "note-3",
            date = "17.11",
            title = "Note with empty content",
            content = "Some content"
        )

        val emptyContent = TextFieldValue("")
        val emptyTaskLines = emptyList<String>()
        val title = "Empty Note"

        // When
        viewModel.updateNote(note, emptyTaskLines, emptyContent, title)
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).updateNote(
            argThat { updatedNote ->
                updatedNote.content.isEmpty()
            }
        )
    }

    /**
     * Тест оновлення нотатки зі спеціальними символами
     */
    @Test
    fun testUpdateNote_withSpecialCharacters_savesCorrectly() = runTest {
        // Given
        val note = Note(
            id = "note-4",
            date = "17.11",
            title = "Special Chars",
            content = ""
        )

        val specialContent = TextFieldValue("Special: @#$%^&*()_+-=[]{}|;':\",./<>?")
        val taskLines = emptyList<String>()
        val title = "Special Title: @#$"

        // When
        viewModel.updateNote(note, taskLines, specialContent, title)
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).updateNote(
            argThat { updatedNote ->
                updatedNote.title.contains("@#$") &&
                updatedNote.content.contains("@#$%^&*")
            }
        )
    }

    /**
     * Тест оновлення дуже довгої нотатки
     */
    @Test
    fun testUpdateNote_withLongContent_savesCorrectly() = runTest {
        // Given
        val note = Note(
            id = "note-5",
            date = "17.11",
            title = "Long Note",
            content = ""
        )

        val longContent = TextFieldValue("A".repeat(10000))
        val taskLines = emptyList<String>()
        val title = "Long Title"
        // When
        viewModel.updateNote(note, taskLines, longContent, title)
        advanceUntilIdle()

        // Then
        verify(notesRepository, times(1)).updateNote(
            argThat { updatedNote ->
                updatedNote.content.length == 10000
            }
        )
    }
}

