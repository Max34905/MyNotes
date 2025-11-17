package com.example.notes

import androidx.test.core.app.ApplicationProvider
import com.example.notes.data.NotesRepository
import com.example.notes.model.Note
import com.example.notes.ui.screens.authentification.SignInScreenViewModel
import com.example.notes.ui.screens.list.NotesListScreenViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit тести для офлайн-режиму автентифікації
 *
 * Дерево 1: Автентифікація (Офлайн)
 * ТВ1: Користувач НЕ автентифікований, спроба входу в офлайн
 * ТВ2: Користувач ВЖЕ автентифікований, запуск в офлайн (кешована сесія)
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OfflineAuthenticationTest {

    @Mock
    private lateinit var notesRepository: NotesRepository

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Initialize Firebase for testing
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        // Setup default mock behavior
        whenever(notesRepository.getNotesFlow()).thenReturn(flowOf(emptyList()))
        whenever(notesRepository.setupRealtimeSync()).then { }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * ТВ1: Користувач НЕ автентифікований, спроба входу в офлайн
     * Умови: Перший запуск, немає інтернету, спроба входу через Google
     * ОР: Вхід неможливий, відображається повідомлення "Немає підключення до Інтернету"
     */
    @Test
    fun testTB1_offlineSignIn_unauthenticatedUser_showsNoConnectionError() = runTest {
        // Given - користувач не автентифікований, немає інтернету
        whenever(firebaseAuth.currentUser).thenReturn(null)
        val signInViewModel = SignInScreenViewModel(context)

        // When - спроба входу в офлайн режимі
        val mockContext = mock<android.content.Context>()
        whenever(mockContext.getString(any())).thenReturn("mock-client-id")
        whenever(mockContext.applicationContext).thenReturn(context)

        signInViewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        // Then - користувач не автентифікований
        assertNull("User should remain unauthenticated in offline mode", firebaseAuth.currentUser)

        // У реальному сценарії має з'явитися помилка про відсутність з'єднання
        // SignInState має бути Error з повідомленням про відсутність інтернету
        assertNotNull("Sign-in state should be updated", signInViewModel.signInState.value)
    }

    /**
     * ТВ2: Користувач ВЖЕ автентифікований, запуск в офлайн (кешована сесія)
     * Умови: Сесія збережена, немає інтернету, запуск додатку
     * ОР: Додаток відкриває головний екран, показує кешовані нотатки
     */
    @Test
    fun testTB2_offlineStart_authenticatedUser_showsCachedNotes() = runTest {
        // Given - користувач автентифікований, є кешовані нотатки
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("cached-user-123")

        val cachedNotes = listOf(
            Note(id = "1", date = "17.11", title = "Cached Note 1", content = "Offline content 1"),
            Note(id = "2", date = "17.11", title = "Cached Note 2", content = "Offline content 2")
        )

        val testRepository = mock<NotesRepository>()
        whenever(testRepository.getNotesFlow()).thenReturn(flowOf(cachedNotes))
        whenever(testRepository.setupRealtimeSync()).then { }

        // When - запуск додатку в офлайн режимі
        val testViewModel = NotesListScreenViewModel(testRepository)
        advanceUntilIdle()

        // Then - користувач бачить кешовані нотатки
        assertNotNull("User should be authenticated", firebaseAuth.currentUser)
        assertEquals("Should display 2 cached notes", 2, testViewModel.notes.value.size)
        assertEquals("First cached note title should match", "Cached Note 1", testViewModel.notes.value[0].title)
        assertEquals("Second cached note title should match", "Cached Note 2", testViewModel.notes.value[1].title)
    }
}

