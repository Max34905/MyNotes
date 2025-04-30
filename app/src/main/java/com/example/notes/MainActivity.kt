package com.example.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.notes.ui.navigation.Navigation
import com.example.notes.ui.screens.authentification.SignInScreenViewModel
import com.example.notes.ui.theme.NotesTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val signInViewModel: SignInScreenViewModel by viewModels()

    val signInLauncher = registerForActivityResult (
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                signInViewModel.handleSignInResultFromActivity(account)
            } catch (e: ApiException) {
                signInViewModel.handleSignInResultFromActivity(null)
            }
        } else {
            signInViewModel.handleSignInResultFromActivity(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotesTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Navigation(
                        modifier = Modifier.padding(innerPadding),
                        navController = rememberNavController(),
                        signInViewModel = signInViewModel
                    )
                }
            }
        }
    }
}