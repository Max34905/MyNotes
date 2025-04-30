package com.example.notes.ui.screens.note

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notes.R
import com.example.notes.model.Note
import com.example.notes.ui.theme.NotesTheme
import kotlinx.coroutines.launch

@Composable
fun NoteScreenWithViewModel(
    modifier: Modifier = Modifier,
    noteId: String,
    viewModel: NoteScreenViewModel = hiltViewModel(),
    navigateBackToList: () -> Unit = {}
) {
    val note = viewModel.note.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        viewModel.getNoteById(noteId)
    }

    if (note.value != null) {
        NoteScreen(
            note = note.value!!,
            updateNote = { note, taskLines, noteTextField, title ->
                viewModel.updateNote(note, taskLines, noteTextField, title)
                navigateBackToList()
            },
            deleteNote = { noteId ->
                scope.launch {
                    viewModel.deleteNote(noteId)
                    navigateBackToList()
                }
            },
            splitContent = { content ->
                viewModel.splitContent(content)
            }
        )
    }
}

@Composable
fun NoteScreen(
    note: Note,
    modifier: Modifier = Modifier,
    updateNote: (Note, List<String>, TextFieldValue, String) -> Unit,
    splitContent: (String) -> Pair<String, String>,
    deleteNote: (String) -> Unit = {}
) {
    val content by remember { mutableStateOf(note.content) }
    var title by remember { mutableStateOf(note.title) }
    val (textFieldLines, listLines) = remember {
        splitContent(content)
    }
    var noteTextFieldContent by remember { mutableStateOf(TextFieldValue(textFieldLines)) }
    var taskLines by remember { mutableStateOf(listLines.split("\n")) }
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    Scaffold (
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = {
            BottomAppBar (
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .height(40.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            modifier = Modifier
                                .sizeIn(minWidth = 10.dp, minHeight = 10.dp, maxWidth = 18.dp, maxHeight = 18.dp),
                            painter = painterResource(R.drawable.bold),
                            contentDescription = stringResource(R.string.bold)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {}) {
                        Icon(
                            modifier = Modifier
                                .sizeIn(minWidth = 10.dp, minHeight = 10.dp, maxWidth = 18.dp, maxHeight = 18.dp),
                            painter = painterResource(R.drawable.italic),
                            contentDescription = stringResource(R.string.italic)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {}) {
                        Icon(
                            modifier = Modifier
                                .sizeIn(minWidth = 10.dp, minHeight = 10.dp, maxWidth = 18.dp, maxHeight = 18.dp),
                            painter = painterResource(R.drawable.underline),
                            contentDescription = stringResource(R.string.underline)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            taskLines = taskLines.toMutableList().apply {
                                add("[ ] ")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_new_task)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row {
                IconButton(
                    onClick = {
                        updateNote(note, taskLines, noteTextFieldContent, title)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = stringResource(id = R.string.close_note)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        deleteNote(note.id)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(id = R.string.delete_note)
                    )
                }
            }
            Text(
                text = note.date,
                color = MaterialTheme.colorScheme.onBackground,
            )
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                maxLines = 1,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.TopStart) {
                        innerTextField()
                        if (title.isEmpty()) {
                            Text(
                                text = stringResource(R.string.note_title_placeholder),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = noteTextFieldContent,
                    onValueChange = { textFieldValue ->
                        noteTextFieldContent = textFieldValue
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.TopStart) {
                            innerTextField()
                            if (noteTextFieldContent.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.note_content_placeholder),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                )
                taskLines.forEachIndexed { index, line ->
                    if (line.startsWith("[x]") || line.startsWith("[ ]")) {
                        val text = line.slice(3 until line.length)
                        var checked by remember { mutableStateOf(line.startsWith("[x]")) }

                        if (!focusRequesters.containsKey(index)) {
                            focusRequesters[index] = FocusRequester()
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    val newTaskLines = taskLines.toMutableList()
                                    newTaskLines.set(index, if (it) "[x]$text" else "[ ]$text")
                                    taskLines = newTaskLines
                                    checked = !checked
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            BasicTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequesters[index] ?: FocusRequester()),
                                value = TextFieldValue(
                                    text = text,
                                    selection = TextRange(text.length)
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                onValueChange = {
                                    val newText = it.text
                                    val newTaskLines = taskLines.toMutableList()
                                    if (newText.contains("\n")) {
                                        val textBeforeNewline = if (newText.substringBefore("\n").isNotEmpty()) {
                                            newText.substringBefore("\n")
                                        } else {
                                            " "
                                        }
                                        val textAfterNewline = if (newText.substringAfter("\n").isNotEmpty()) {
                                            newText.substringAfter("\n")
                                        } else {
                                            " "
                                        }
                                        newTaskLines.set(
                                            index,
                                            if (checked) "[x]$textBeforeNewline" else "[ ]$textBeforeNewline"
                                        )
                                        newTaskLines.add(index + 1, "[ ]$textAfterNewline")
                                        taskLines = newTaskLines
                                    } else if (newText.isEmpty()) {
                                        if (taskLines.size > index) {
                                            newTaskLines.removeAt(index)
                                            taskLines = newTaskLines
                                        }
                                    } else {
                                        newTaskLines.set(
                                            index,
                                            if (checked) "[x]$newText" else "[ ]$newText"
                                        )
                                        taskLines = newTaskLines
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                                )
                            )
                            LaunchedEffect(taskLines.size) {
                                focusRequesters[index]?.requestFocus()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun NoteScreenPreview() {
    NotesTheme {
        NoteScreen(
            note = Note(id = "1", date = "2024-01-01", title = "Sample Note", content = "This is\na sample note"),
            updateNote = {_, _, _, _ ->},
            deleteNote = {},
            splitContent = { content -> Pair("", "")},
        )
    }
}