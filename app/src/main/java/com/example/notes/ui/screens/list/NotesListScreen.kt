package com.example.notes.ui.screens.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notes.R
import com.example.notes.model.Note
import com.example.notes.ui.components.NoteItem
import com.example.notes.ui.theme.NotesTheme

@Composable
fun NotesListScreenWithViewModel(
    modifier: Modifier = Modifier,
    viewModel: NotesListScreenViewModel = hiltViewModel(),
    onNoteClick: (String) -> Unit = {},
    navigateToSignInScreen: () -> Unit = {}
) {
    val notes by viewModel.notes.collectAsState()
    val isNavigatingToNote by viewModel.isNavigatingToNote.collectAsState()

    isNavigatingToNote?.let { noteId ->
        onNoteClick(noteId)
        viewModel.onNavigatedToNote()
    }

    NotesListScreen(
        modifier = modifier,
        notes = notes,
        onNoteClick = onNoteClick,
        onSignOut = {
            viewModel.signOut()
            navigateToSignInScreen()
        },
        addNote = {viewModel.addNote()}
    )
}

@Composable
fun NotesListScreen(
    modifier: Modifier = Modifier,
    notes: List<Note>,
    onNoteClick: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
    addNote: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isNotEmpty()) {
            notes.filter { note ->
                note.title.contains(searchQuery, ignoreCase = true)
            }
        } else {
            notes
        }
    }
    Scaffold (
        floatingActionButton = {
            IconButton (
                onClick = {
                    addNote()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = stringResource(R.string.add_note_button),
                    modifier = Modifier.size(50.dp)
                )
            }
        }
    ) { innerPadding ->
        Column (
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(innerPadding)
        ) {
            Row (modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.notes_list_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onSignOut
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.sign_out_button)
                    )
                }
            }
            OutlinedTextField(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth(),
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_icon_description)
                    )
                },
                maxLines = 1,
                colors = TextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true
            )
            LazyColumn (
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredNotes,
                    key = {note -> note.id}
                ) { note ->
                    NoteItem(
                        note = note,
                        onNoteClick = onNoteClick
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewNotesListScreen() {
    NotesTheme {
        NotesListScreen(modifier = Modifier, notes = listOf(
            Note(id = "1", date = "2024-01-01", title = "Note 1", content = "This is the content of note 1"),
            Note(id = "2", date = "2024-01-02", title = "Note 2", content = "This is the content of note 2"),
            Note(id = "3", date = "2024-01-03", title = "Note 3", content = "This is the content of note 3")
        )) {}
    }
}