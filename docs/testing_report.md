# Звіт з тестування MyNotes App

## Дата: 17 листопада 2025

## 1. Визначення тестових варіантів

### Дерево розбиття тестових варіантів

Детальне дерево розбиття з описом тестових варіантів знаходиться в файлі: `docs/test_partition_tree.md`

### Основні категорії тестування:

#### 1.1 Аутентифікація (AC-001, AC-002)
- Відображення екрану входу для неаутентифікованих користувачів
- Успішний вхід через Google аутентифікацію
- Збереження стану аутентифікації

#### 1.2 Керування нотатками - Online режим (AC-003, AC-004, AC-005)
- Створення нотатки з синхронізацією в Firebase
- Редагування нотатки з оновленням в Firebase
- Видалення нотатки з синхронізацією

#### 1.3 Керування нотатками - Offline режим (AC-006, AC-007)
- Створення нотатки в офлайн режимі (збереження локально)
- Редагування нотатки в офлайн режимі

#### 1.4 Real-time синхронізація (AC-008, AC-009, AC-010)
- Синхронізація створення даних в реальному часі
- Синхронізація редагування даних в реальному часі
- Синхронізація видалення даних в реальному часі

---

## 2. Створені тестові класи

### 2.1 NotesRepositoryTest.kt
**Локація:** `app/src/test/java/com/example/notes/NotesRepositoryTest.kt`

**Покриває Acceptance Criteria:** AC-003, AC-004, AC-005, AC-006, AC-007, AC-008, AC-009, AC-010

**Тестові методи:**
1. `testCreateNoteOnline()` - Створення нотатки в online режимі
2. `testUpdateNoteOnline()` - Редагування нотатки в online режимі
3. `testDeleteNoteOnline()` - Видалення нотатки в online режимі
4. `testCreateNoteOffline()` - Створення нотатки в offline режимі
5. `testUpdateNoteOffline()` - Редагування нотатки в offline режимі
6. `testRealtimeSyncOnCreate()` - Real-time синхронізація при створенні
7. `testRealtimeSyncOnUpdate()` - Real-time синхронізація при редагуванні
8. `testRealtimeSyncOnDelete()` - Real-time синхронізація при видаленні
9. `testGetNoteById()` - Отримання нотатки за ID
10. `testClearLocalData()` - Очищення локальних даних

### 2.2 NotesListScreenViewModelTest.kt
**Локація:** `app/src/test/java/com/example/notes/NotesListScreenViewModelTest.kt`

**Покриває Acceptance Criteria:** AC-003, AC-005, AC-008, AC-009, AC-010

**Тестові методи:**
1. `testAddNote_createsNewNoteAndNavigatesToIt()` - Додавання нотатки та навігація
2. `testGetNotes_loadsNotesFromRepository()` - Завантаження нотаток з Repository
3. `testRealtimeUpdate_updatesNotesList()` - Оновлення списку в real-time
4. `testSignOut_clearsDataAndSignsOut()` - Вихід користувача
5. `testOnNavigatedToNote_clearsNavigationState()` - Скидання стану навігації
6. `testRealtimeDelete_removesNoteFromList()` - Видалення через real-time sync
7. `testEmptyNotesList_handledCorrectly()` - Обробка порожнього списку
8. `testAddNote_handlesError()` - Обробка помилки створення

### 2.3 NoteScreenViewModelTest.kt
**Локація:** `app/src/test/java/com/example/notes/NoteScreenViewModelTest.kt`

**Покриває Acceptance Criteria:** AC-004, AC-005, AC-007, AC-009

**Тестові методи:**
1. `testGetNoteById_loadsNoteSuccessfully()` - Завантаження нотатки за ID
2. `testUpdateNote_savesChangesSuccessfully()` - Збереження змін нотатки
3. `testUpdateNote_withTaskLines_combinesContentCorrectly()` - Об'єднання тексту та задач
4. `testDeleteNote_deletesSuccessfully()` - Видалення нотатки
5. `testSplitContent_separatesTextAndTasks()` - Розділення контенту
6. `testSplitContent_onlyText_noTasks()` - Тільки текст без задач
7. `testSplitContent_onlyTasks_noText()` - Тільки задачі без тексту
8. `testUpdateNote_withEmptyContent_savesCorrectly()` - Збереження порожнього контенту
9. `testUpdateNote_withSpecialCharacters_savesCorrectly()` - Спеціальні символи
10. `testUpdateNote_withLongContent_savesCorrectly()` - Довгий контент

### 2.4 SignInScreenViewModelTest.kt
**Локація:** `app/src/test/java/com/example/notes/SignInScreenViewModelTest.kt`

**Покриває Acceptance Criteria:** AC-001, AC-002

**Тестові методи:**
1. `testInitialState_isIdle()` - Початковий стан аутентифікації
2. `testSignInWithGoogle_setsLoadingState()` - Початок процесу входу
3. `testSuccessfulAuthentication_setsSuccessState()` - Успішна аутентифікація
4. `testUnauthenticatedUser_requiresSignIn()` - Неаутентифікований користувач
5. `testAuthenticationError_setsErrorState()` - Помилка аутентифікації
6. `testRetryAfterError_resetsToLoading()` - Повторна спроба після помилки
7. `testAuthenticationPersistence()` - Збереження стану
8. `testSuccessfulGoogleSignIn_navigatesToNotesList()` - Успішний вхід та навігація
9. `testMultipleSignInAttempts()` - Множинні спроби входу
10. `testSignIn_handlesSDKVersionDifferences()` - Підтримка різних версій SDK

---

## 3. Технологічний стек тестування

### Використані бібліотеки:
- **JUnit 4.13.2** - Основний фреймворк для тестування
- **Mockito 5.20.0** - Мокування залежностей
- **Mockito-Kotlin 6.1.0** - Kotlin-friendly API для Mockito
- **Kotlinx Coroutines Test 1.10.2** - Тестування корутин
- **AndroidX Core Testing 2.2.0** - Тестування архітектурних компонентів
- **Turbine 1.2.1** - Тестування Flow (для майбутніх тестів)

### Підхід до тестування:
- **Unit Testing** - Ізольоване тестування компонентів
- **Mocking** - Імітація Firebase та локальної бази даних
- **Coroutine Testing** - Тестування асинхронних операцій
- **Given-When-Then** - Структура тестів

---

## 4. Покриття Acceptance Criteria

| AC ID | Опис | Тестовий клас | Статус |
|-------|------|---------------|--------|
| AC-001 | Відображення екрану входу | SignInScreenViewModelTest | ✅ Покрито |
| AC-002 | Успішний Google Sign-In | SignInScreenViewModelTest | ✅ Покрито |
| AC-003 | Створення нотатки (Online) | NotesRepositoryTest, NotesListScreenViewModelTest | ✅ Покрито |
| AC-004 | Редагування нотатки (Online) | NotesRepositoryTest, NoteScreenViewModelTest | ✅ Покрито |
| AC-005 | Видалення нотатки | NotesRepositoryTest, NoteScreenViewModelTest | ✅ Покрито |
| AC-006 | Створення в офлайн режимі | NotesRepositoryTest | ✅ Покрито |
| AC-007 | Редагування в офлайн режимі | NotesRepositoryTest, NoteScreenViewModelTest | ✅ Покрито |
| AC-008 | Синхронізація створення | NotesRepositoryTest, NotesListScreenViewModelTest | ✅ Покрито |
| AC-009 | Синхронізація редагування | NotesRepositoryTest, NoteScreenViewModelTest | ✅ Покрито |
| AC-010 | Синхронізація видалення | NotesRepositoryTest, NotesListScreenViewModelTest | ✅ Покрито |

**Загальне покриття:** 10/10 (100%)

---

## 5. Як запустити тести

### Через командний рядок:
```bash
# Запустити всі unit тести
gradlew test

# Запустити тести конкретного класу
gradlew test --tests NotesRepositoryTest

# Запустити з детальним виводом
gradlew test --info

# Переглянути звіт
# Звіт буде в: app/build/reports/tests/testDebugUnitTest/index.html
```

### Через Android Studio:
1. Відкрийте тестовий файл
2. Клікніть зелену стрілку біля класу/методу
3. Виберіть "Run 'ClassName'"

---

## 6. Структура тестів (Given-When-Then)

Всі тести слідують структурі Given-When-Then для кращої читабельності:

```kotlin
@Test
fun testCreateNoteOnline() = runTest {
    // Given - Підготовка тестових даних
    val expectedNoteId = "test-note-id-123"
    
    // When - Виконання тестованої дії
    val noteId = notesRepository.addNote()
    
    // Then - Перевірка результатів
    assertNotNull("Note ID should not be null", noteId)
    verify(noteDao, times(1)).upsertNote(any())
}
```

---

## 7. Граничні випадки та edge cases

Тести покривають наступні граничні випадки:

### 7.1 Валідація даних:
- ✅ Порожній контент нотатки
- ✅ Дуже довгий контент (10000+ символів)
- ✅ Спеціальні символи у title та content
- ✅ Контент тільки з текстом
- ✅ Контент тільки з задачами

### 7.2 Мережеві помилки:
- ✅ Створення нотатки без інтернету
- ✅ Редагування нотатки без інтернету
- ✅ Помилка при створенні нотатки

### 7.3 Стани:
- ✅ Порожній список нотаток
- ✅ Множинні спроби аутентифікації
- ✅ Різні версії Android SDK

---

## 8. Висновки

### Виконано:
✅ Визначено тестові варіанти та створено дерево розбиття  
✅ Створено 4 тестових класи  
✅ Реалізовано 38+ тестових методів  
✅ Покрито всі 10 Acceptance Criteria (100%)  
✅ Додано тестування граничних випадків  
✅ Використано кращі практики тестування (Given-When-Then, Mocking)  

### Переваги створеної тестової стратегії:
- **Повне покриття** всіх acceptance criteria
- **Ізольовані тести** - кожен тест незалежний від інших
- **Швидкі тести** - використання моків замість реальних залежностей
- **Зрозуміла структура** - Given-When-Then для легкого читання
- **Граничні випадки** - тестування edge cases та помилок

### Наступні кроки:
1. Запустити тести та переконатися, що всі проходять
2. Інтегрувати тести в CI/CD pipeline
3. Додати інструментальні тести для UI (якщо потрібно)
4. Налаштувати code coverage звіти

---

**Автор:** AI Testing Assistant  
**Проект:** MyNotes App  
**Версія:** 1.0

