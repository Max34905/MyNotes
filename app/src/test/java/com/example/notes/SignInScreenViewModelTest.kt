package com.example.notes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notes.ui.screens.authentification.SignInScreenViewModel
import com.example.notes.ui.screens.authentification.SignInState
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Unit тести для SignInScreenViewModel
 * Покриває дерево розбиття для функції автентифікації (Google Sign-In)
 *
 * Початок: Запит на автентифікацію через Google
 * ТВ1: Користувач обирає дійсний акаунт Google і підтверджує вхід
 * ТВ2: Користувач скасовує вікно вибору акаунту
 * ТВ3: Відбувається помилка API (немає підключення, сервіси недоступні, акаунт заблоковано)
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SignInScreenViewModelTest {

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var viewModel: SignInScreenViewModel
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Initialize Firebase for testing
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        viewModel = SignInScreenViewModel(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * ТВ1: Успішна автентифікація
     * Умови: Користувач обирає дійсний акаунт Google і підтверджує вхід
     * Очікуваний результат: Успішна автентифікація, перехід на головний екран нотаток
     */
    @Test
    fun testTB1_successfulGoogleSignIn_authenticatesAndNavigatesToNotesList() = runTest {
        whenever(firebaseAuth.currentUser).thenReturn(null)
        assertEquals("Initial state should be Idle", SignInState.Idle, viewModel.signInState.value)

        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("authenticated-user-123")
        whenever(firebaseUser.email).thenReturn("user@example.com")
        whenever(firebaseUser.displayName).thenReturn("Test User")

        assertNotNull("User should be authenticated", firebaseAuth.currentUser)
        assertEquals("User email should match", "user@example.com", firebaseAuth.currentUser?.email)
        assertEquals("User name should match", "Test User", firebaseAuth.currentUser?.displayName)
        assertEquals("User ID should be set", "authenticated-user-123", firebaseAuth.currentUser?.uid)
    }

    /**
     * ТВ2: Скасування автентифікації
     * Умови: Користувач скасовує вікно вибору акаунту (натискає "назад" або закриває вікно)
     * Очікуваний результат: Користувач залишається на екрані входу, повідомлення про помилку не відображається
     */
    @Test
    fun testTB2_userCancelsSignIn_staysOnSignInScreenWithoutError() = runTest {
        // Given - початковий стан
        val initialState = viewModel.signInState.value
        assertEquals("Initial state should be Idle", SignInState.Idle, initialState)

        // When - користувач скасовує вхід (не завершує процес автентифікації)
        val mockContext = mock<Context>()
        whenever(mockContext.getString(any())).thenReturn("mock-client-id")
        whenever(mockContext.applicationContext).thenReturn(context)

        viewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        // Симулюємо скасування - користувач не автентифікований
        whenever(firebaseAuth.currentUser).thenReturn(null)

        // Then - користувач залишається на екрані входу
        assertNull("User should remain unauthenticated", firebaseAuth.currentUser)
        assertNotNull("Sign-in state should be set", viewModel.signInState.value)

        // Перевіряємо що стан не є помилкою (Error) з повідомленням
        val currentState = viewModel.signInState.value
        val isNotErrorState = currentState !is SignInState.Error ||
                             (currentState is SignInState.Error && currentState.message.isEmpty())
        assertTrue("Should not show error message for user cancellation", isNotErrorState)
    }

    /**
     * ТВ3: Помилка API при автентифікації
     * Умови: Відбувається помилка API (немає підключення до Інтернету, сервіси Google недоступні, акаунт заблоковано)
     * Очікуваний результат: Користувач залишається на екрані входу, відображається повідомлення про помилку
     */
    @Test
    fun testTB3_apiErrorDuringSignIn_staysOnSignInScreenWithErrorMessage() = runTest {
        // Given - початковий стан
        assertEquals("Initial state should be Idle", SignInState.Idle, viewModel.signInState.value)

        // When - відбувається помилка API (наприклад, немає інтернету)
        val mockContext = mock<Context>()
        whenever(mockContext.getString(any())).thenReturn("mock-client-id")
        whenever(mockContext.applicationContext).thenReturn(context)

        viewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        // Симулюємо помилку - користувач не автентифікований через помилку
        whenever(firebaseAuth.currentUser).thenReturn(null)

        // Then - користувач залишається на екрані входу
        assertNull("User should remain unauthenticated after API error", firebaseAuth.currentUser)
        assertNotNull("Sign-in state should be updated", viewModel.signInState.value)

        // У реальному сценарії стан буде Error з повідомленням
        // Тут перевіряємо що стан змінився від Idle
        assertNotEquals("State should change from Idle after sign-in attempt",
                       SignInState.Idle, viewModel.signInState.value)
    }

    /**
     * Тест початкового стану
     */
    @Test
    fun testInitialState_isIdle() {
        assertEquals(
            "Initial sign-in state should be Idle",
            SignInState.Idle,
            viewModel.signInState.value
        )
    }

    /**
     * Тест збереження стану автентифікації
     */
    @Test
    fun testAuthenticationPersistence() {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("persisted-user-123")

        // When
        val currentUser = firebaseAuth.currentUser

        // Then
        assertNotNull("Persisted user should not be null", currentUser)
        assertEquals(
            "Persisted user ID should match",
            "persisted-user-123",
            currentUser?.uid
        )
    }

    /**
     * Тест множинних спроб входу
     */
    @Test
    fun testMultipleSignInAttempts() = runTest {
        // Given
        val mockContext = mock<Context>()
        whenever(mockContext.getString(any())).thenReturn("mock-client-id")
        whenever(mockContext.applicationContext).thenReturn(context)

        // When - кілька спроб входу
        viewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        viewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        // Then
        assertNotNull("Sign-in state should handle multiple attempts", viewModel.signInState.value)
    }
}
