# Service Contracts: Patient Management System (Solo Doctor, Single Machine)

There is no network API in this application (constitution v3.0.0 — no
Spring Boot, no REST, no browser). JavaFX controllers call these Java
service interfaces directly, in-process. This document replaces the former
`contracts/api.md` (Spring Boot REST contract), which is superseded by the
JavaFX pivot.

## AuthService

- `boolean isInitialized()` — true once the single `DoctorCredential` row
  exists (used to route to Setup vs. Login on startup).
- `void createInitialCredential(String username, String rawPassword)` —
  first-run only (FR-021); throws if already initialized.
- `boolean login(String username, String rawPassword)` — validates
  credentials (BCrypt-hashed comparison); returns `false` uniformly for a
  wrong username OR password (acceptance scenario US1.2), never revealing
  which field was wrong.
- `boolean resetPassword(String username, String recoveryAnswer,
  String newPassword)` — verifies the recovery answer against the stored
  BCrypt hash, replaces only the password hash and update timestamp, and
  returns `false` for an unknown user, incorrect answer, or blank new password.

## PatientService

- `Patient create(PatientData data)` — FR-001; validates required patient
  fields before persistence. Phone must contain exactly 10 digits, email may
  be blank but must be valid when present, date of birth must be more than one
  year ago, and gender must be `Male`, `Female`, or `Undisclosed`; invalid
  data throws `BadRequestException`.
- `Patient getById(long id)` — throws `NotFoundException` if missing.
- `Patient update(long id, PatientData data)` — FR-002; applies the same
  validation. If the stored patient has a legacy gender outside the supported
  choices, an unchanged legacy value may be preserved, but any changed value
  must be one of the three supported choices.
- `List<Patient> search(String query, String phone, LocalDate fromDate, LocalDate toDate)`
  — partial name/phone match + optional history date-range filter; always
  excludes `isDeleted=true` rows (FR-003).
- `List<Patient> search(null, null, today.minusDays(6), today)` — the
  landing-page default invocation after login; returns patients with
  non-deleted history entries in the last 7 days or an empty list for the
  explicit no-results state (FR-003a).
- `void softDelete(long id)` — sets `isDeleted=true`/`deletedAt=now`; never
  physically deletes the patient or its history/prescriptions/note (FR-020).
- `List<Patient> listDeleted()` — soft-deleted patients only (FR-020a).
- `Patient restore(long id)` — clears `isDeleted`/`deletedAt`; throws if the
  patient is not currently deleted (FR-020a).

## MedicalHistoryService

- `List<MedicalHistoryEntry> listForPatient(long patientId)` — ordered by
  `entryDate` descending and excluding `isDeleted=true` entries (FR-004).
- `MedicalHistoryEntry add(long patientId, LocalDate entryDate, String diagnosis, String prescription, String notes)` — creates a visit entry from the Medical History tab (FR-004, FR-005, FR-006).
- `MedicalHistoryEntry getForEdit(long patientId, long entryId)` — returns
  the selected non-deleted entry for the double-click edit popup; throws
  `NotFoundException` if the entry does not belong to the patient or has
  been deleted (FR-006a).
- `MedicalHistoryEntry update(long patientId, long entryId, LocalDate entryDate, String diagnosis, String prescription, String notes)` — saves edits from the popup and refreshes the history list (FR-006a).
- `void softDelete(long patientId, long entryId)` — after confirmation, sets
  `isDeleted=true`/`deletedAt=now` and hides the entry from normal history
  lists and printing while retaining it for backups (FR-006b).
- `String getCurrentPrescriptionText(long patientId)` — returns the
  prescription text from the most recent non-deleted history entry with
  non-blank prescription text, or an empty string if none exists (FR-014).

## ClinicProfileService

- `ClinicProfile get()` — throws `NotFoundException` if setup hasn't run yet.
- `void saveInitialProfile(ClinicProfileData data)` — first-run only (FR-021).
- `ClinicProfile update(ClinicProfileData data)`.

## BackupService

- `String exportSql()` — generates the full ANSI-compatible SQL INSERT dump
  (FR-007, including soft-deleted patients); updates `BackupMetadata`'s
  `lastBackupAt` on success. The calling `BackupRestoreController` writes
  the returned string to the file chosen via `FileChooser` (FR-008).
- `int restoreFromSql(byte[] fileContent)` — validates the header
  and statement shape (only `INSERT INTO` statements accepted), clears
  existing domain tables, replays the statements inside one transaction,
  rolling back entirely on any failure (FR-009, FR-011, FR-012). Returns the
  number of restored patient rows.

## H2NativeBackupService

- `void exportScript(Path destination)` — writes an H2-specific full
  schema-and-data script using `SCRIPT NOSETTINGS`, prefixes the distinct
  `PMS_H2_BACKUP_V1` marker, removes H2 security statements, rejects
  destructive output, and replaces the destination only after successful
  generation.
- `void restoreScript(Path source)` — accepts only the H2 marker, creates the
  current database's ANSI safety backup, runs `RUNSCRIPT` in a fresh temporary
  database, validates the restored schema, and swaps the database into place
  only after success. Failed restore leaves the live database unchanged.

The H2-native format is intentionally H2-version-specific and must not be
treated as a replacement for the portable `BackupService` format.

## BackupReminderService

- `LocalDateTime getLastBackupAt()` — nullable.
- `boolean isReminderDue()` — true if never backed up or more than 5 days
  since the last successful backup (FR-009a, FR-019a).

## UI Call Flow (replaces the former HTTP request/response contract)

Each JavaFX FXML controller is constructed with references to the service(s)
it needs (simple constructor injection wired in `PatientManagementApp`).
Controllers translate UI events (button clicks, form submits, history-row
double-clicks) into direct Java method calls on these services, and translate
return values/exceptions into UI feedback (success/failure alerts, per
FR-017). The Add Patient controller performs field-level validation before
submitting to the service: it renders all applicable errors as red labels
beneath their fields, consumes the submit action, and preserves entered
values when validation fails. Unexpected failures may still use an alert.
There is no serialization, HTTP status code, or network round-trip involved
anywhere in this application.

## JavaFX Windowing Contract

- `PatientManagementApp` opens the primary landing stage in full-screen or
  maximized mode after successful login (FR-022).
- `PatientListController` initializes `fromDate=today.minusDays(6)` and
  `toDate=today`, then calls `PatientService.search(null, null, fromDate,
  toDate)` before the doctor types search criteria (FR-003a).
- Opening a patient from `PatientListController` uses an owned modal stage for
  `PatientProfile.fxml`; it must not replace the landing scene (FR-023).
- Opening print from `PatientProfileController` uses an owned modal stage for
  `PrintPreview.fxml`; it must not replace the patient-profile window
  (FR-024).
- Modal windows are application/window-modal, block interaction with their
  owners, and are sized/positioned so the owner window remains visible behind
  the active modal (FR-025).

## Print Preview Contract

- Optional print controls are Patient History, Current Prescription, and
  Include History Notes (FR-014).
- Include History Notes applies only when Patient History is selected; it
  prints `MedicalHistoryEntry.notes` inline with printed history entries and
  never creates a separate Personal Notes section.
- Current Prescription continues to use
  `MedicalHistoryService.getCurrentPrescriptionText(long patientId)` and
  ignores soft-deleted history entries.

## Visual Styling Contract

- All FXML buttons use action-category style classes defined in
  `app/src/main/resources/css/application.css` instead of default-only
  styling (FR-026).
- Medical History Date, Diagnosis, Prescription, and Notes controls have
  visible `Label` elements in `PatientProfile.fxml` and the edit dialog;
  prompt text may remain as a secondary aid but is not the only label.
