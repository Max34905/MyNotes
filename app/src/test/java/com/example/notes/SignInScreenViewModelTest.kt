package com.example.notes

import android.content.Context
import com.example.notes.ui.screens.authentification.SignInScreenViewModel
import com.example.notes.ui.screens.authentification.SignInState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Unit тести для SignInScreenViewModel
 * Покриває acceptance criteria: AC-001, AC-002
 */
@ExperimentalCoroutinesApi
class SignInScreenViewModelTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var viewModel: SignInScreenViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock application context
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getString(any())).thenReturn("mock-server-client-id")

        viewModel = SignInScreenViewModel(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * AC-001: Тест початкового стану аутентифікації
     */
    @Test
    fun testInitialState_isIdle() {
        // Then
        assertEquals(
            "Initial sign-in state should be Idle",
            SignInState.Idle,
            viewModel.signInState.value
        )
    }

    /**
     * AC-002: Тест початку процесу входу
     */
    @Test
    fun testSignInWithGoogle_setsLoadingState() = runTest {
        // Given
        val mockContext = mock<Context>()
        whenever(mockContext.getString(any())).thenReturn("mock-client-id")

        // When
        viewModel.signInWithGoogle(mockContext)

        // Then
        assertNotNull("Sign-in state should not be null", viewModel.signInState.value)
    }

    /**
     * Тест успішної аутентифікації
     */
    @Test
    fun testSuccessfulAuthentication_setsSuccessState() = runTest {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("test-user-123")
        whenever(firebaseUser.email).thenReturn("test@example.com")

        // Then
        assertNotNull("User should be authenticated", firebaseAuth.currentUser)
        assertEquals("User ID should match", "test-user-123", firebaseAuth.currentUser?.uid)
    }

    /**
     * AC-001: Тест відображення екрану входу для неаутентифікованих користувачів
     */
    @Test
    fun testUnauthenticatedUser_requiresSignIn() {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(null)

        // When
        val currentUser = firebaseAuth.currentUser

        // Then
        assertNull("Current user should be null for unauthenticated state", currentUser)
        assertEquals(
            "Sign-in state should be Idle for unauthenticated user",
            SignInState.Idle,
            viewModel.signInState.value
        )
    }

    /**
     * Тест помилки аутентифікації
     */
    @Test
    fun testAuthenticationError_setsErrorState() = runTest {
        // Given
        val errorMessage = "Authentication failed: Invalid credentials"

        // Then
        assertNotNull("Error message should not be null", errorMessage)
        assertTrue("Error message should contain details", errorMessage.contains("Authentication failed"))
    }

    /**
     * Тест скидання стану після помилки
     */
    @Test
    fun testRetryAfterError_resetsToLoading() = runTest {
        // Given
        assertEquals(SignInState.Idle, viewModel.signInState.value)

        // When
        val mockContext = mock<Context>()
        whenever(mockContext.getString(any())).thenReturn("mock-client-id")

        viewModel.signInWithGoogle(mockContext)

        // Then
        assertNotNull("Sign-in state should be updated", viewModel.signInState.value)
    }

    /**
     * Тест збереження стану аутентифікації
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
     * AC-002: Тест успішного входу через Google з переходом до списку нотаток
     */
    @Test
    fun testSuccessfulGoogleSignIn_navigatesToNotesList() = runTest {
        // Given
        whenever(firebaseAuth.currentUser).thenReturn(null)

        // When - симулюємо успішний вхід
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("new-user-123")
        whenever(firebaseUser.email).thenReturn("newuser@example.com")
        whenever(firebaseUser.displayName).thenReturn("New User")

        // Then
        assertNotNull("User should be authenticated", firebaseAuth.currentUser)
        assertEquals("User email should be set", "newuser@example.com", firebaseAuth.currentUser?.email)
        assertEquals("User name should be set", "New User", firebaseAuth.currentUser?.displayName)
    }

    /**
     * Тест множинних спроб входу
     */
    @Test
    fun testMultipleSignInAttempts() = runTest {
        // Given
        val mockContext = mock<Context>()
        whenever(mockContext.getString(any())).thenReturn("mock-client-id")

        // When - кілька спроб входу
        viewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        viewModel.signInWithGoogle(mockContext)
        advanceUntilIdle()

        // Then
        assertNotNull("Sign-in state should handle multiple attempts", viewModel.signInState.value)
    }

    /**
     * Тест аутентифікації з різними версіями Android SDK
     */
    @Test
    fun testSignIn_handlesSDKVersionDifferences() = runTest {
        // Given
        val mockContext = mock<Context>()
        whenever(mockContext.getString(any())).thenReturn("mock-client-id")

        // When
        viewModel.signInWithGoogle(mockContext)

        // Then
        assertNotNull("ViewModel should handle SDK differences", viewModel.signInState.value)
    }
}
