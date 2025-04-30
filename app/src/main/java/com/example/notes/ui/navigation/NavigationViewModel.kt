package com.example.notes.ui.navigation

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

sealed class NavigationState {
    object Loading: NavigationState()
    data class Ready(val startDestination: Any): NavigationState()
}

@HiltViewModel
class NavigationViewModel @Inject constructor(): ViewModel() {
    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Loading)
    val navigationState: StateFlow<NavigationState> = _navigationState

    init {
        checkUserAuth()
    }

    private fun checkUserAuth() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            _navigationState.value = NavigationState.Ready(NotesListScreen)
        } else {
            _navigationState.value = NavigationState.Ready(SignInScreen)
        }
    }
}