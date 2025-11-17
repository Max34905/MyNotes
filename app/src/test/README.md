# README - Тестування MyNotes App

## Огляд

Цей проект містить повний набір unit тестів для додатку MyNotes, які покривають всі 10 Acceptance Criteria згідно з вимогами Given-When-Then.

## Структура тестів

```
app/src/test/java/com/example/notes/
├── NotesRepositoryTest.kt              # Тести репозиторію (10 методів)
├── NotesListScreenViewModelTest.kt     # Тести списку нотаток (8 методів)
├── NoteScreenViewModelTest.kt          # Тести екрану нотатки (10 методів)
└── SignInScreenViewModelTest.kt        # Тести аутентифікації (10 методів)
```

## Покриття Acceptance Criteria

### ✅ AC-001: Відображення екрану входу
**Тест:** `SignInScreenViewModelTest.testUnauthenticatedUser_requiresSignIn()`
- Перевіряє, що неаутентифікований користувач бачить екран входу

### ✅ AC-002: Успішний вхід через Google
**Тести:** 
- `SignInScreenViewModelTest.testSignInWithGoogle_setsLoadingState()`
- `SignInScreenViewModelTest.testSuccessfulGoogleSignIn_navigatesToNotesList()`

### ✅ AC-003: Створення нотатки
**Тести:**
- `NotesRepositoryTest.testCreateNoteOnline()`
- `NotesListScreenViewModelTest.testAddNote_createsNewNoteAndNavigatesToIt()`

### ✅ AC-004: Редагування нотатки
**Тести:**
- `NotesRepositoryTest.testUpdateNoteOnline()`
- `NoteScreenViewModelTest.testUpdateNote_savesChangesSuccessfully()`

### ✅ AC-005: Видалення нотатки
**Тести:**
- `NotesRepositoryTest.testDeleteNoteOnline()`
- `NoteScreenViewModelTest.testDeleteNote_deletesSuccessfully()`

### ✅ AC-006: Створення в автономному режимі
**Тест:** `NotesRepositoryTest.testCreateNoteOffline()`

### ✅ AC-007: Редагування в автономному режимі
**Тести:**
- `NotesRepositoryTest.testUpdateNoteOffline()`
- `NoteScreenViewModelTest.testUpdateNote_withTaskLines_combinesContentCorrectly()`

### ✅ AC-008: Синхронізація створення даних
**Тести:**
- `NotesRepositoryTest.testRealtimeSyncOnCreate()`
- `NotesListScreenViewModelTest.testGetNotes_loadsNotesFromRepository()`

### ✅ AC-009: Синхронізація редагування даних
**Тести:**
- `NotesRepositoryTest.testRealtimeSyncOnUpdate()`
- `NotesListScreenViewModelTest.testRealtimeUpdate_updatesNotesList()`

### ✅ AC-010: Синхронізація видалення даних
**Тести:**
- `NotesRepositoryTest.testRealtimeSyncOnDelete()`
- `NotesListScreenViewModelTest.testRealtimeDelete_removesNoteFromList()`

## Запуск тестів

### Командний рядок

```bash
# Всі тести
gradlew test

# Конкретний клас
gradlew test --tests NotesRepositoryTest

# З детальним виводом
gradlew test --info

# Тільки debug варіант
gradlew testDebugUnitTest
```

### Android Studio

1. **Запуск всіх тестів:**
   - Правий клік на папці `test/java/com/example/notes`
   - Виберіть "Run 'Tests in 'com.example.notes'"

2. **Запуск одного класу:**
   - Відкрийте тестовий файл
   - Клікніть зелену стрілку біля назви класу
   - Виберіть "Run 'ClassName'"

3. **Запуск одного методу:**
   - Клікніть зелену стрілку біля назви методу
   - Виберіть "Run 'testMethodName'"

## Звіти

Після виконання тестів звіти генеруються в:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

Відкрийте цей файл в браузері для перегляду детального звіту.

## Технології

- **JUnit 4** - Фреймворк для тестування
- **Mockito 5** - Мокування залежностей
- **Mockito-Kotlin** - Kotlin DSL для Mockito
- **Kotlinx Coroutines Test** - Тестування корутин
- **AndroidX Core Testing** - Lifecycle-aware компоненти

## Приклад тесту

```kotlin
@Test
fun testCreateNoteOnline() = runTest {
    // Given - Підготовка
    val expectedNoteId = "test-note-id-123"
    val mockDatabaseReference = mock(DatabaseReference::class.java)
    `when`(mockDatabaseReference.key).thenReturn(expectedNoteId)
    
    // When - Дія
    val noteId = notesRepository.addNote()
    
    // Then - Перевірка
    assertNotNull("Note ID should not be null", noteId)
    assertTrue("Note ID should not be empty", noteId.isNotEmpty())
    verify(noteDao, times(1)).upsertNote(any())
}
```

## Граничні випадки

Тести покривають:
- ✅ Порожній контент
- ✅ Дуже довгий контент (10000+ символів)
- ✅ Спеціальні символи
- ✅ Мережеві помилки (offline режим)
- ✅ Множинні операції
- ✅ Порожній список нотаток

## Метрики

- **Загальна кількість тестів:** 38+
- **Покриття AC:** 10/10 (100%)
- **Тестових класів:** 4
- **Середній час виконання:** < 5 секунд

## Troubleshooting

### Тести не запускаються
```bash
# Очистити та перезібрати
gradlew clean test
```

### Помилки з Mockito
```bash
# Переконайтеся, що mockito-inline додано
# Це потрібно для мокування final класів Kotlin
```

### Помилки з корутинами
```bash
# Переконайтеся, що Dispatchers.setMain() викликається в @Before
# та Dispatchers.resetMain() в @After
```

## Continuous Integration

Тести можна інтегрувати в CI/CD:

```yaml
# GitHub Actions приклад
- name: Run Unit Tests
  run: ./gradlew test

- name: Upload Test Report
  uses: actions/upload-artifact@v2
  with:
    name: test-results
    path: app/build/reports/tests/
```

## Контакти

Для питань по тестуванню дивіться:
- `docs/testing_report.md` - Детальний звіт
- `docs/test_partition_tree.md` - Дерево розбиття тестів
- `docs/acceptance_criteria.md` - Вимоги

---

**Останнє оновлення:** 17 листопада 2025

