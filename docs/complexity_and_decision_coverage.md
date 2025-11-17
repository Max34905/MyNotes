# Complexity and Decision-Coverage Test Plan

This document derives McCabe (cyclomatic) complexity and an approximate Halstead metric from `docs/cfg.puml`, then provides a practical test plan to achieve decision coverage aligned with the Given–When–Then acceptance criteria.

---

## 1) McCabe Cyclomatic Complexity (from CFG)

Count predicate/decision nodes in the control-flow graph:

- D1: if (X?)
- D2: while (Z)
- D3: if (W)
- D4: elseif (L)
- D5: elseif (DD)
- D6: elseif (NN)
- D7: if (R?)
- D8: if (U?)
- D9: if (GG?)
- D10: if (WW?)

Assuming a single connected component (P = 1):

- Cyclomatic complexity V(G) = D + 1 = 10 + 1 = 11
- Interpretation: There are 11 linearly independent paths. A basis-path suite would need at least 11 tests; decision coverage typically needs fewer but should still be guided by these branch points.

---

## 2) Halstead Metrics (coarse CFG-based approximation)

Halstead metrics are normally computed from source code operators/operands. Because only a CFG is available, we use a conservative approximation:

- Treat each decision as an operator occurrence; treat each action node (`:A;`, `:B;`, etc.) as an operand occurrence.
- Distinct operators (n1): {if/elseif, while} → n1 = 2 (merging if/elseif)
- Distinct operands (n2): all unique action nodes → n2 = 39
- Total operator occurrences (N1): number of decisions → N1 = 10
- Total operand occurrences (N2): number of action nodes → N2 = 39

Derived values:

- Vocabulary n = n1 + n2 = 2 + 39 = 41
- Length N = N1 + N2 = 10 + 39 = 49
- Volume V = N · log2(n) ≈ 49 · log2(41) ≈ 49 · 5.36 ≈ 262.6
- Difficulty D = (n1 / 2) · (N2 / n2) = (2 / 2) · (39 / 39) = 1.0
- Effort E = D · V ≈ 262.6
- Estimated Bugs B = V / 3000 ≈ 0.088

Notes and limitations:
- This is not a canonical Halstead calculation (which requires parsing operators like function calls, assignments, variables). Use it only as a relative, CFG-level proxy. For accurate Halstead metrics, recompute from source after instrumentation.

---

## 3) Decision-Coverage Test Plan (maps to acceptance criteria)

Goal: For every decision in the CFG, execute both True and False outcomes at least once across the suite, while satisfying or extending the provided GWT acceptance tests.

Decision → high-level meaning (assumptions from the app domain):
- X?: user is authenticated
- Z: user continues performing note operations (loop), else exits
- W | L | DD | NN: user chooses operation in the main screen (create, edit, delete, other)
- R?: online vs offline sub-flow during edit
- U?: nested choice within the online edit flow (e.g., extra option/formatting path)
- GG?: delete confirmation
- WW?: sign-in success

Acceptance Criteria (AC) references in parentheses. “Extra” indicates tests added only to complete decision coverage.

Test cases (minimal but covering all decision outcomes):

1) Unauthenticated start shows Sign-In (covers X? = False)
- Given app launched and user not authenticated (AC1)
- Then Sign-In screen is shown

2) Successful Sign-In navigates to notes list (covers WW? = True)
- Given on Sign-In screen (AC2)
- When user signs in with valid Google account
- Then navigate to notes list

3) Failed Sign-In stays on Sign-In (covers WW? = False) [Extra]
- Given on Sign-In screen
- When user attempts sign-in with invalid/denied account
- Then remain on Sign-In screen with error

4) Create note online (covers W = True, Z = True at least once; later Z = False)
- Given notes list and device online (AC3, AC8)
- When tap “+” and create a note
- Then note appears in list and syncs to Firebase
- And exit main screen to force one loop end (Z = False)

5) Create note offline with later sync (ensures loop path diversity)
- Given notes list and device offline (AC6)
- When tap “+” and create a note
- Then note saved locally
- When reconnects, it syncs to Firebase

6) Edit note online – variant A (covers L = True, R? = True, U? = True)
- Given viewing a note online (AC4, AC9)
- When edit and save with an extra option toggled (e.g., bold)
- Then changes appear in list and sync to Firebase

7) Edit note online – variant B (covers U? = False)
- Given viewing a note online
- When edit and save without extra option
- Then changes appear in list and sync to Firebase

8) Edit note offline with later sync (covers R? = False)
- Given viewing a note offline (AC7)
- When edit and save
- Then changes saved locally
- When reconnects, they sync to Firebase

9) Delete note with confirmation Yes (covers DD = True, GG? = True)
- Given viewing a note online (AC5, AC10)
- When tap delete and confirm
- Then note disappears from list and change syncs to Firebase

10) Delete note with confirmation No (covers GG? = False) [Extra]
- Given viewing a note
- When tap delete and cancel in confirmation
- Then note remains

11) Execute an “other” operation (covers NN = True) [Extra, aligns with AC11]
- Given viewing/using the app
- When apply a UI action not covered above (e.g., toggle italic/underline)
- Then UI behaves per Material Design (AC11)

Decision-outcome coverage matrix (aggregate across tests):
- X?: T (by any test after successful sign-in), F (Test 1)
- Z: T (Tests 4–9), F (explicit exit after operations in Test 4)
- W: T (Test 4/5), F (Tests 6–11)
- L: T (Tests 6–8), F (others)
- DD: T (Tests 9–10), F (others)
- NN: T (Test 11), F (others)
- R?: T (Tests 6–7), F (Test 8)
- U?: T (Test 6), F (Test 7)
- GG?: T (Test 9), F (Test 10)
- WW?: T (Test 2), F (Test 3)

This suite exercises both outcomes of every decision at least once while satisfying all stated ACs; the three marked “Extra” are minimal additions needed solely to flip branch outcomes absent from the ACs (failed login, delete cancel, and an “other” UI action) and can be framed as negative/UX consistency checks.

---

## 4) Optional: Measuring decision/branch coverage during execution

If you want tooling evidence:
- Add JaCoCo to the Android app module to collect coverage from `connectedDebugAndroidTest` UI tests.
- Run the UI tests that implement the above scenarios (Espresso/Compose testing APIs) and inspect branch coverage in the report.
- For Compose, prefer using the `androidx.compose.ui.test` APIs to navigate and assert UI states; mock network (online/offline) using dependency injection or a connectivity abstraction.

High-level steps:
1) Enable JaCoCo in `app/build.gradle.kts` and generate coverage reports for androidTest.
2) Write tests in `app/src/androidTest/...` that implement Tests 1–11.
3) Execute on an emulator/device and review the branch coverage report to ensure both outcomes per decision were hit.

Note: The CFG-level mapping above is your ground truth for ensuring decision coverage even if tooling lacks precise branch mapping for some high-level flows.

---

## 5) Traceability to Acceptance Criteria

- AC1 → Test 1
- AC2 → Test 2
- AC3 → Test 4
- AC4 → Tests 6–7
- AC5 → Test 9
- AC6 → Test 5
- AC7 → Test 8
- AC8 → Test 4
- AC9 → Tests 6–7
- AC10 → Test 9
- AC11 → Test 11

Additional tests (3, 10) are justified by the decision-coverage mandate to exercise both outcomes of WW? and GG?.

