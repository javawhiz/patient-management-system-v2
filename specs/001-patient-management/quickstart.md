# Quickstart: Patient Management System (Solo Doctor, Single Machine)

This guide validates the feature end-to-end after implementation. It does
not include implementation code — see [data-model.md](./data-model.md) and
[contracts/service-contracts.md](./contracts/service-contracts.md) for
structural details, and `tasks.md` for the implementation breakdown.

## Prerequisites

- JDK 21 installed (for local dev; the packaged `.exe` bundles its own JVM
  and JavaFX runtime)
- JavaFX 21 SDK (or the `javafx-controls`/`javafx-fxml` Maven dependencies,
  which pull the platform-specific runtime automatically)
- Maven (or the included Maven Wrapper)
- Windows machine (or Windows VM) for final `jpackage` validation; local
  dev/testing can run on any OS supported by JDK 21 + JavaFX

## Local Development Run

```bash
cd app
mvn clean javafx:run
```

Expected: a native JavaFX window opens directly showing the first-run Setup
screen (or Login screen if already set up) — no browser, no URL, no server
process involved (FR/Principle VII).

## Validation Scenarios

Each scenario maps to an acceptance scenario in [spec.md](./spec.md).

### 1. First-run credential setup + login/logout (User Story 1)

1. On first run (no `DoctorCredential` row yet), complete the first-run
   setup screen with a username/password.
2. Restart the app; log in with those credentials → access granted.
3. Confirm the landing patient list opens full-screen/maximized, with From
   pre-filled to today minus 6 days, To pre-filled to today, and matching
   patients loaded automatically or an explicit no-results state shown.
4. Log in with an incorrect password → access denied, generic error shown.
5. Select **Forgot Password?**, provide the configured recovery answer and a
   matching new password, then log in with the new password.
6. Repeat with an incorrect recovery answer → reset is rejected and the old
   password remains valid.
7. Log out with no backup ever taken → dismissible backup reminder appears;
   dismiss it → logout completes.

### 2. Manage patient records (User Story 2)

1. Open Add Patient and confirm Gender is a dropdown with Male, Female, and
   Undisclosed, and Address is a wrapping field showing four visible rows.
2. Enter an invalid phone, invalid email, and too-recent date of birth, then
   submit → the dialog remains open, all entered values remain present, and
   each invalid field shows a red inline error beneath it.
3. Correct the invalid fields, choose a supported gender, and submit → the
   patient is created successfully. Blank email remains valid.
4. Search by partial name → patient appears in results.
5. Search by phone number → same patient appears.
6. Edit the patient's phone number → reopen profile → updated value shown.
7. Open the patient from the landing list → patient profile opens in a new
   modal window above the landing page; the landing page remains visible
   behind it and cannot be interacted with until the patient window closes.
8. Search for a name with no matches → UI shows an explicit "no results"
   state (not an error).

### 3. Medical history entry form and editing (User Story 3)

1. Open a patient profile and confirm there is no separate Prescription tab
   and no separate Personal Notes tab.
2. On the Medical History tab, confirm Date is compact and Diagnosis is a
   labeled multiline field beside it, while labeled Prescription and Notes
   are full-width multiline text areas below them; the history list appears
   below the entry fields.
3. Add a medical history entry (date, diagnosis, prescription, notes) → it
   appears in the patient's history, most recent first.
4. Double-click the new history row → an edit popup opens with date,
   diagnosis, prescription, and notes pre-populated.
5. Change diagnosis/prescription/notes and save → popup closes and the
   refreshed history list shows the updated values.
6. Double-click the same row again, choose soft-delete, and confirm → the
   entry no longer appears in the normal history list.

### 4. Backup & restore (User Story 4)

1. With patients on file, trigger backup export → native `FileChooser` save
   dialog appears; save the `.sql` file.
2. Open the saved `.sql` file in a text editor → confirm it contains
   plain, ANSI-compatible `INSERT INTO ...` statements (no H2-specific
   syntax).
3. On a fresh instance (or after clearing data), trigger restore and select
   the saved file via the native `FileChooser` open dialog → confirm a
   warning appears that existing data will be replaced; confirm → all
   patients and history entries (including prescription and notes content)
   reappear exactly as backed up.
4. Attempt to restore a corrupted/invalid file → operation is rejected with
   a clear error; existing data is unchanged.
5. Export an H2 Native Backup → save the `.h2.sql` file and confirm it has the
   `PMS_H2_BACKUP_V1` marker, schema statements, and no `DROP TABLE` statement.
6. Restore the H2 backup → confirm the app creates an ANSI safety backup,
   shows the in-progress bar, validates a fresh database, and only then
   replaces the live database.
7. Start a large backup or restore → confirm the JavaFX window remains
   responsive and the progress bar remains visible until completion.

### 5. Print a patient handout (User Story 5)

1. Open a patient with medical history entries, including prescription text.
2. Start printing → checklist of optional sections (History, Prescription)
   plus Include History Notes is shown, all unchecked by default.
3. Select "History" only → open native print dialog (`PrinterJob`) →
   preview shows patient info + clinic info + history dates/diagnoses, but
   no notes and no standalone current prescription section.
4. Select "History" and "Include History Notes" → preview shows notes inline
   with the printed history entries.
5. Select "Prescription" only → preview shows patient info + clinic info +
   the prescription text from the most recent non-deleted history entry with
   non-blank prescription text.
6. Confirm print preview opens as a modal window above the patient profile,
   keeps the patient profile visible behind it, and blocks interaction with
   the patient profile until closed.
7. Confirm the native print dialog offers printer, paper size, and copies
   selection.

### 7. Button colors and stacked windows

1. Scan the landing, patient profile, medical history edit, backup/restore,
   deleted-patient, and print preview windows → every button has a relevant
   non-default action color.
2. From landing, open a patient; from patient profile, open print; from
   patient profile, double-click a history row → each child opens as a modal
   stacked window and the window behind remains visible but inactive.

### 6. Backup reminder on logout (5-day threshold)

1. Perform a backup export (sets `lastBackupAt` to now).
2. Log out immediately → no backup reminder shown.
3. Simulate/advance time past 5 days without a new backup (e.g., adjust
   `BackupMetadata.lastBackupAt` in a test environment) → log out → backup
   reminder shown; cancel it → logout still completes.

## Packaging Validation (Windows)

```bash
# From app/, after `mvn clean package` produces the runnable JAR/libs:
jpackage --type exe \
  --input target/ \
  --main-jar patient-management-system.jar \
  --name "Patient Management System" \
  --win-shortcut \
  --win-menu
```

Expected: a Windows `.exe` installer is produced; installing and launching
it opens the native JavaFX window directly, with no separate Java or
JavaFX installation required on the target machine.
