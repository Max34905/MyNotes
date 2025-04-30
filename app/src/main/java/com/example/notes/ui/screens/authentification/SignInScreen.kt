package com.example.notes.ui.screens.authentification

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notes.R
import com.example.notes.ui.theme.NotesTheme

@Composable
fun SignInScreenWithViewModel(
    modifier: Modifier = Modifier,
    viewModel: SignInScreenViewModel = hiltViewModel(),
    navigateToNotesList: () -> Unit = { }
) {
    val signInState = viewModel.signInState.collectAsState()

    SignInScreen(
        modifier = modifier,
        signInState = signInState.value,
        navigateToNotesList = {
            navigateToNotesList()
            viewModel.resetState()
        },
        onSignIn = { context ->
            viewModel.signInWithGoogle(context)
        }
    )
}

@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    signInState: SignInState = SignInState.Idle,
    navigateToNotesList: () -> Unit = { },
    onSignIn: (Context) -> Unit = { }
) {
    val context = LocalContext.current

    LaunchedEffect(signInState) {
        if (signInState is SignInState.Success) {
            navigateToNotesList()
        }
    }

    Column (
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.width(200.dp),
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
            )
        )
        Text(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .width(200.dp),
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        )
        Button(
            modifier = Modifier
                .width(300.dp)
                .align(Alignment.CenterHorizontally),
            onClick = {
                onSignIn(context)
            },
        ) {
            Row (
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.width(24.dp),
                    painter = painterResource(R.drawable.google_logo), contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.log_in_via_google)
                )
            }
        }
        if (signInState is SignInState.Error) {
            Text(text = signInState.message)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun LogInScreenPreview() {
    NotesTheme {
        SignInScreen()
    }
}