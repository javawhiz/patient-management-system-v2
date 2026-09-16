# Research: Patient Management System (Solo Doctor, Single Machine)

All items below reflect the JavaFX pivot (constitution v3.0.0). No open
`NEEDS CLARIFICATION` markers remain.

## 1. UI framework

- **Decision**: JavaFX 21 (`javafx-controls`, `javafx-fxml`), views defined
  in FXML, one controller class per screen.
- **Rationale**: Explicit project requirement (replacing the earlier React +
  Spring Boot plan); JavaFX is a mature, actively maintained desktop UI
  toolkit for Java 21 with native look-and-feel, built-in `FileChooser` and
  `PrinterJob` APIs matching constitution Principle VI directly.
- **Alternatives considered**: Swing — rejected because it's the toolkit
  this project is explicitly replacing; a plain React+Spring Boot web app —
  rejected per this pivot (no browser/HTTP layer wanted).

## 2. Application shell / no HTTP layer

- **Decision**: A single JavaFX `Application` subclass (`PatientManagementApp`)
  is the entry point; views call services directly in-process. No embedded
  HTTP server, no REST controllers, no browser.
- **Rationale**: Removes an entire unnecessary layer (network stack, CSRF,
  sessions, JSON serialization) for a single-process desktop app, per
  constitution Principle II (renamed to View/Controller/Service/Repository).
- **Alternatives considered**: Keeping a local embedded HTTP server (e.g.,
  Spring Boot) purely as a backend for a JavaFX shell via WebView — rejected
  as unnecessary complexity with no benefit once there's no browser-based UI.

## 3. Persistence access (no Spring Data)

- **Decision**: Plain JPA/Hibernate with a manually configured
  `EntityManagerFactory` (via `persistence.xml` or programmatic
  `Configuration`), and simple DAO-style repository classes wrapping
  `EntityManager` calls in explicit transactions.
- **Rationale**: Spring Data JPA's repository magic isn't needed without
  Spring Boot's DI container; plain JPA keeps the dependency footprint small
  and the persistence layer easy to reason about in a desktop app.
- **Alternatives considered**: Pulling in the full Spring Framework (non-Boot)
  purely for dependency injection — rejected as unnecessary weight; a
  hand-rolled lightweight DI (simple constructor wiring in the `Application`
  class) is sufficient for this app's size.

## 4. H2 version pinning

- **Decision**: Pin the exact H2 version as a Maven property in `pom.xml`
  (e.g., `<h2.version>2.2.224</h2.version>`) with an inline comment stating
  it must not be bumped without a deliberate migration plan and a
  compatibility check against existing `.mv.db` files (constitution
  Principle IV). Unchanged from the original plan.
- **Rationale**: Same reasoning as before — schema/version stability is
  independent of UI framework choice.

## 5. Backup export format (avoiding H2-specific SQL)

- **Decision**: Unchanged in approach — a `BackupService` reads all rows via
  JDBC/JPA and hand-generates ANSI-compatible `INSERT INTO table (...)
  VALUES (...)` statements in FK-safe table order, rather than using H2's
  `SCRIPT` command.
- **Rationale**: Guarantees the dump stays portable regardless of H2
  internals or UI framework (constitution Principle V).

## 6. Restore transaction strategy

- **Decision**: Unchanged — restore runs inside a single JPA transaction:
  validate the file header, clear existing domain tables, replay the
  INSERT statements; any failure rolls back entirely, leaving prior data
  intact (FR-011, FR-012).

## 7. Backup save dialog / restore file picker

- **Decision**: Use `javafx.stage.FileChooser` for both export (save
  dialog, default filename `PatientBackup_YYYY-MM-DD.sql`, `.sql` extension
  filter) and restore (open dialog, `.sql` extension filter). Both are
  real native OS dialogs on every platform JavaFX supports — no browser API
  or fallback logic needed (simpler than the earlier
  `showSaveFilePicker`/download-fallback approach).
- **Rationale**: `FileChooser` is a first-class native dialog in JavaFX,
  directly satisfying constitution Principle VI without any
  browser-compatibility caveats.
- **Alternatives considered**: A custom in-app file browser — explicitly
  disallowed by constitution Principle VI.

## 8. Tracking "last successful backup" for the logout reminder

- **Decision**: Unchanged — a single-row `backup_metadata` table (via
  Flyway migration) storing `last_backup_at` (nullable timestamp), updated
  only after a backup export completes successfully; read by the
  logout/close-warning logic.

## 9. H2-native backup alongside portable SQL

- **Decision**: Keep the hand-generated ANSI INSERT export as the portable
  default and add a separate `H2NativeBackupService` using H2 2.2.224's
  `SCRIPT NOSETTINGS` command. The generated file carries a
  `PMS_H2_BACKUP_V1` marker and excludes H2 security statements.
- **Restore safety**: H2-native restore creates the current ANSI export as a
  safety backup, validates the marker and rejects destructive or nested script
  commands, runs `RUNSCRIPT` only in a fresh temporary database, validates that
  database, then swaps its files into the live data path.
- **Rationale**: H2 scripting captures schema, indexes, constraints, identity
  definitions, and data that the portable table list does not. Keeping it
  separate preserves cross-database migration and the existing transactional
  ANSI restore contract.
- **Trade-off**: H2-native files are coupled to the pinned H2 version and are
  not suitable for another database engine. The UI labels the two formats
  separately.

## 10. Password recovery

- **Decision**: Add a local Forgot Password flow using the configured recovery
  question and a BCrypt comparison. Store only a one-way
  `recovery_answer_hash` through Flyway V2; never store the recovery answer in
  plaintext.
- **Rationale**: A fixed answer cannot be made secret through reversible
  encryption inside a standalone app because the decryption key would also
  need to ship with the app. Hash comparison prevents casual recovery of the
  answer by inspecting database contents.
- **Trade-off**: Anyone who knows the recovery answer can reset the local
  account; this is a recovery convenience, not multi-factor authentication.

## 11. Background backup operations

- **Decision**: Run portable and H2 backup/restore work in JavaFX `Task`
  workers and show a shared indeterminate progress bar while the operation is
  active.
- **Rationale**: File export, H2 scripting, and fresh-database validation can
  take long enough to freeze the UI at practice scale. Success and failure
  alerts return to the JavaFX thread.

## 12. Printing

- **Decision**: A `PrintPreviewController` renders the patient handout as a
  JavaFX `Node` (e.g., a `VBox`/`TextFlow` layout built from patient/clinic
  data plus any selected optional sections), then calls
  `Printer.getDefaultPrinter()` / `PrinterJob.createPrinterJob()` and
  `printerJob.printPage(node)` to invoke the OS-native print dialog. No PDF
  library is used.
- **Rationale**: Matches constitution Principle VI directly via the Java
  print API; avoids any PDF-generation dependency.
- **Alternatives considered**: Server-side/embedded PDF generation (e.g.,
  iText) — explicitly out of scope per constitution and original user
  input.

## 13. Packaging (jpackage)

- **Decision**: Maven build produces a fat/shaded JAR (or an exploded
  `app`+`libs` directory via `maven-shade-plugin`/`maven-assembly-plugin`);
  a packaging step runs `jpackage --type exe` with `--input` pointing at the
  built JAR/libs, bundling a JVM **and** the JavaFX runtime modules (via
  `jlink` with the `javafx.controls`/`javafx.fxml` modules, or by including
  the JavaFX SDK jmods in the custom runtime image) so the doctor's machine
  needs neither a separate Java nor a separate JavaFX install.
- **Rationale**: Matches the constitution's Windows `.exe` via `jpackage`
  requirement and the "zero technical knowledge" UX principle.
- **Alternatives considered**: Requiring a pre-installed JDK+JavaFX on the
  target machine — rejected; fails the zero-technical-knowledge bar.

## 14. Application startup (no browser)

- **Decision**: `PatientManagementApp.start(Stage)` shows the primary
  `Stage` directly (Login or first-run Setup screen) — no server starts, no
  URL, no browser involved (constitution Principle VII, amended v3.0.0).
- **Rationale**: A native JavaFX window is the simplest possible
  zero-friction startup for a desktop app; the previous "auto-open browser
  at localhost" step is no longer applicable or needed.

## 15. Keeping a future MySQL migration low-cost

- **Decision**: All persistence goes through JPA entities/repositories using
  standard JPQL/Criteria queries (no H2-specific SQL functions); the JDBC
  URL and Hibernate dialect live in one configuration point (`AppConfig`/
  `persistence.xml`), so a future MySQL migration only requires changing
  that configuration plus a Flyway-compatible schema review — no
  service/repository code changes.
- **Rationale**: Matches constitution's "Future Readiness Without Scope
  Creep" section; still no networking/hosting work performed now.

## 16. Clinical entry surface simplification

- **Decision**: Keep prescription text and clinical notes inside each
  `MedicalHistoryEntry`; remove the separate Prescription and Personal Notes
  tabs/screens from the patient profile.
- **Rationale**: The doctor enters visit-specific clinical data together.
  Collapsing date, diagnosis, prescription, and notes into one Medical
  History workflow reduces navigation, avoids duplicate note-taking surfaces,
  and keeps each prescription tied to the visit where it was written.
- **Alternatives considered**: Retaining a separate Prescription tab with a
  prescription history — rejected because the current requirement explicitly
  says prescriptions should not be a separate screen. Retaining a separate
  Personal Notes tab — rejected because visit notes are sufficient for the
  doctor's workflow.

## 17. Medical history edit and delete behavior

- **Decision**: Double-clicking a history row opens a modal JavaFX edit
  dialog populated with the selected entry. The dialog saves edits through
  `MedicalHistoryService.update(...)` or soft-deletes the entry through
  `MedicalHistoryService.softDelete(...)` after confirmation.
- **Rationale**: Double-click editing matches the doctor's requested table
  interaction, keeps the main Medical History tab focused on quick entry,
  and preserves audit-friendly data retention by hiding deleted entries
  without physically removing them.
- **Alternatives considered**: Inline table editing — rejected because large
  multiline diagnosis/prescription/notes fields are awkward inside table
  cells. Hard delete — rejected because the project already favors soft
  deletion for clinical data retention and full backups.

## 18. Landing page default date range

- **Decision**: After successful login, `PatientListController` pre-fills the
  date range from today minus 6 days through today and immediately runs the
  existing patient search with that range.
- **Rationale**: The doctor lands directly on recent activity without typing
  search criteria, while still using the existing date-range search semantics
  against non-deleted medical history entries.
- **Alternatives considered**: Loading every patient by default — rejected
  because it does not satisfy the requested last-7-days landing view. Adding
  a new appointment/recent-patient table — rejected because appointments are
  out of scope and no new persistence is needed.

## 19. Modal stacked windows

- **Decision**: Keep the landing patient list as the owner window; open
  patient profile, print preview, and medical-history edit views as owned
  modal JavaFX stages using application/window modality. Size and center each
  child window so the owner remains visible behind it.
- **Rationale**: This preserves context while preventing accidental edits in
  a lower window until the active window is closed, matching the doctor's
  requested stacked-window workflow.
- **Alternatives considered**: Replacing scenes in the same primary stage —
  rejected because the landing page must remain visible. Modeless windows —
  rejected because the previous window must not be usable until the top
  window is closed.

## 20. Print history notes option

- **Decision**: Add an Include History Notes checkbox to print preview; it is
  dependent on Patient History and prints notes inline with history entries
  only when selected.
- **Rationale**: The doctor can print concise history without notes by
  default, but include clinical notes when needed. This keeps notes tied to
  history and does not reintroduce a separate Personal Notes section.
- **Alternatives considered**: Always printing notes with history — rejected
  because the doctor asked for a checkbox. Re-adding Personal Notes printing
  — rejected because personal notes were removed from the model.

## 21. Action button color semantics

- **Decision**: Define action-category CSS classes for buttons: primary/save,
  open/edit, print, backup/restore, delete/destructive, restore, and
  cancel/back.
- **Rationale**: Consistent, relevant colors improve scanning and reduce
  accidental destructive actions without changing controller responsibilities.
- **Alternatives considered**: Default JavaFX button styling — rejected by the
  current requirement. Per-button one-off inline styles — rejected because CSS
  classes are easier to keep consistent across FXML views.
