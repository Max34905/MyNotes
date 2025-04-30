package com.example.notes.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.notes.ui.screens.note.NoteScreenWithViewModel
import com.example.notes.ui.screens.list.NotesListScreenWithViewModel
import com.example.notes.ui.screens.authentification.SignInScreenViewModel
import com.example.notes.ui.screens.authentification.SignInScreenWithViewModel
import kotlinx.serialization.Serializable

@Serializable
object SignInScreen
@Serializable
object NotesListScreen
@Serializable
data class NoteScreen(val note: String)

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    signInViewModel: SignInScreenViewModel,
    navigationViewModel: NavigationViewModel = hiltViewModel()
) {
    val navigationState = navigationViewModel.navigationState.collectAsState().value

    when(navigationState) {
        is NavigationState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is NavigationState.Ready -> {
            NavHost(navController = navController, startDestination = navigationState.startDestination) {
                composable<SignInScreen> {
                    SignInScreenWithViewModel(
                        modifier = modifier,
                        viewModel = signInViewModel,
                        navigateToNotesList = {
                            navController.navigate(NotesListScreen) {
                                popUpTo(SignInScreen) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
                composable<NotesListScreen> {
                    NotesListScreenWithViewModel(
                        modifier = modifier,
                        onNoteClick = { noteId ->
                            navController.navigate(NoteScreen(noteId))
                        },
                        navigateToSignInScreen = {
                            navController.navigate(SignInScreen) {
                                popUpTo(NotesListScreen) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
                composable<NoteScreen> { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getString("note")
                    NoteScreenWithViewModel(
                        noteId = noteId ?: "",
                        navigateBackToList = {
                            navController.navigateUp()
                        },
                        modifier = modifier
                    )
                }
            }
        }
    }
}