package com.example.notes.ui.screens.authentification

import android.content.Context
import android.os.Build
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notes.MainActivity
import com.example.notes.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInScreenViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context
): ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val googleIdOption: GetGoogleIdOption? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(applicationContext.getString(R.string.server_client_id))
            .setAutoSelectEnabled(true)
            .build()
    } else null

    private val request: GetCredentialRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption!!)
            .build()
    } else null

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _signInState.value = SignInState.Loading
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                signInWithCredentialManager(context)
            } else {
                signInWithGoogleSignInSDK(context)
            }
        }
    }

    private suspend fun signInWithCredentialManager(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request!!)
            handleSignInResultWithCredentials(result)
        } catch (e: GetCredentialException) {
            _signInState.value = SignInState.Error(e.message ?: "Failed to get credentials")
        }
    }

    private fun signInWithGoogleSignInSDK(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.server_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val intent = googleSignInClient.signInIntent
                intent.putExtra("prompt", "select_account")
                (context as? MainActivity)?.signInLauncher?.launch(intent)
            }
        }
    }

    private fun handleSignInResultWithCredentials(result: GetCredentialResponse) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            _signInState.value = SignInState.Success
                        } else {
                            _signInState.value = SignInState.Error(task.exception?.message ?: "Failed to sign in")
                        }
                    }
            }
        } catch (e: Exception) {
            _signInState.value = SignInState.Error(e.message ?: "Failed to process credentials")
        }
    }

    fun handleSignInResultFromActivity(result: GoogleSignInAccount?) {
        try {
            if (result != null) {
                val idToken = result.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            _signInState.value = SignInState.Success
                        } else {
                            _signInState.value = SignInState.Error(task.exception?.message ?: "Failed to sign in")
                        }
                    }
            }
        } catch (e: Exception) {
            _signInState.value = SignInState.Error(e.message ?: "Failed to process credentials")
        }
    }

    fun resetState() {
        _signInState.value = SignInState.Idle
    }
}

sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    object Success : SignInState()
    data class Error(val message: String) : SignInState()
}