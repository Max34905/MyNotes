package com.example.notes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.notes.model.Note
import com.example.notes.ui.theme.NotesTheme

@Composable
fun NoteItem(
    note: Note,
    onNoteClick: (String) -> Unit
) {
    val content by remember { mutableStateOf(note.content) }
    val (textFieldLines, listLines) = remember {
        val textLines = StringBuilder()
        val taskLines = StringBuilder()

        content.split("\n").forEach { line ->
            if (line.startsWith("[ ]") || line.startsWith("[x]")) {
                if (taskLines.isNotEmpty()) {
                    taskLines.append("\n")
                }
                taskLines.append(line)
            } else {
                if (textLines.isNotEmpty()) {
                    textLines.append("\n")
                }
                textLines.append(line)
            }
        }

        Pair(textLines.toString(), taskLines.toString())
    }
    val textLines by remember { mutableStateOf(textFieldLines.split("\n")) }
    val taskLines by remember { mutableStateOf(listLines.split("\n")) }

    Card (
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp, max = 250.dp)
            .clickable(
                onClick = { onNoteClick(note.id) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = note.date,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = textLines.joinToString("\n"),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = if (taskLines[0].isNotEmpty()) 1 else 6,
                overflow = TextOverflow.Ellipsis
            )
            Box (
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Column {
                    for (i in 0 until 2) {
                        val line = taskLines.getOrNull(i) ?: break
                        if (line.isNotEmpty()) {
                            val isChecked = line.startsWith("[x]")
                            val taskText = line.slice(3 until line.length)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {},
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = taskText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (taskLines.size > 2) {
                        Text(
                            text = "...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewNoteItem() {
    NotesTheme {
        NoteItem(
            note = Note(
                id = "1",
                date = "2024-01-01",
                title = "Example Note",
                content = "This is an example note\nSome task\nCompleted task\n[ ] Incomplete task")
        ) {}
    }
}