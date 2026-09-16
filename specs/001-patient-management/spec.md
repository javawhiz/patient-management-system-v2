# Feature Specification: Patient Management System (Solo Doctor, Single Machine)

**Feature Branch**: `[001-patient-management]`

**Created**: 2026-09-15

**Status**: Implemented

**Input**: User description: "Build a Patient Management System for a solo medical doctor, running strictly on one Windows laptop at a time. The system must allow the doctor to manage patient records and dated medical history entries that include diagnosis, prescription text, and notes; export/restore full backups as a portable SQL dump via native OS dialogs to support one-time machine migration; and print patient handouts via the native Windows print dialog with a checklist of optional sections (history and current prescription) while always including patient and clinic information."

## Clarifications

### Session 2026-09-15

- Q: What personal-information fields must every patient record capture? → A: Full name, date of birth, gender, address, emergency contact, phone number (required), email address (optional).
- Q: What validation rules apply to patient personal information? → A: Gender is selected from Male, Female, or Undisclosed; phone number is exactly 10 digits; email is optional but must use a valid email format when provided; date of birth must be more than one year in the past.
- Q: What should happen when patient-form validation fails? → A: Keep the Add Patient form open, preserve all entered values, and show an inline red error message beneath every invalid field; do not replace field-level feedback with a popup.
- Q: What should each medical history entry contain, and can a patient have more than one entry? → A: Multiple dated entries per patient, each with date, diagnosis/complaint, and free-text notes.
- Q: Beyond matching by patient name, what other ways should the doctor be able to search or filter patients? → A: Search by name (partial match) or phone number, with an optional date-range filter on prior history/appointment entries to narrow results.
- Q: Should the app ever prompt/remind the doctor to take a backup, or is backup purely manual? → A: Manual trigger for backup itself, plus the app warns the doctor before closing if no backup has ever been taken.
- Q: Should notes be included in the full backup export, or excluded since they're private? → A: Medical history notes are included in the backup, same as all other patient data — nothing is excluded.
- Q: Should the app require login, and how should logout interact with backups? → A: Yes — a single static username/password (stored in the local database) gates access, with an explicit logout action. If no backup has been taken in the last 5 days, logout shows a dismissible reminder to back up; the doctor can cancel the reminder and still proceed with logout/closing.
- Q: How should patient deletion and initial login setup work? → A: Patient deletion is a soft delete (hidden from search/lists/printing, never physically removed, still included in backups) rather than a hard delete; login credentials are set by the doctor via an interactive first-run setup screen, with no default/pre-seeded credentials shipped.
- Q: Should soft-deleted patients be permanently hidden, or can the doctor view and restore them? → A: Restorable — deleted patients are hidden from all normal views by default (never shown greyed-out or mixed into normal lists), but the doctor can open a separate "Deleted Patients" view/toggle to see them and restore any of them back to normal visibility.

### Session 2026-09-16

- Q: Should prescriptions and personal notes be separate patient-profile screens? → A: No. Prescription must be a text area on the Medical History tab only, and the separate Prescription and Personal Notes tabs must be removed.
- Q: How should the Medical History entry form be laid out? → A: Date remains as-is; Diagnosis sits next to Date as a multiline text area around 4 lines tall and spans the remaining page width. Prescription and Notes are full-width multiline text areas on the next row, each taking roughly half of that row/available entry area so together they occupy about 40% of the window height. The history list appears below them.
- Q: Can medical history entries be edited or removed after creation? → A: Yes. Double-clicking a history row opens an edit popup populated with the selected entry's date, diagnosis, prescription, and notes. The doctor can save changes or soft-delete that history entry from the popup.
- Q: What should the doctor see immediately after login? → A: The landing patient list opens in full-screen/maximized mode, pre-fills the date-range filters for the last 7 days (today and previous 6 days), and automatically loads matching patients.
- Q: How should patient/profile/print windows behave? → A: Opening a patient from the landing page must open a new modal window above the landing page rather than replacing it. Print opens as another modal window above the patient window. A modal window must be closed before returning to the previous window, and the previous window remains visible behind it.
- Q: How should buttons and medical-history fields be presented? → A: All buttons should use relevant, non-default colors, and the Medical History tab must show labels for Date, Diagnosis, Prescription, and Notes fields.
- Q: How should notes be handled when printing patient history? → A: Printing includes a separate checkbox to include history notes only when needed; history can be printed without notes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Log In and Log Out (Priority: P1)

The doctor logs in with a single static username and password (stored in the
local database) before using the app, and can log out when finished. If no
backup has been taken within the last 5 days, logging out shows a
dismissible reminder to back up first, but never blocks logout or closing
the app.

**Why this priority**: This is the gate every other capability sits behind;
without it, the app would be open to anyone with physical access to the
laptop.

**Independent Test**: Can be fully tested by starting the app, logging in
with the stored credentials, then logging out, without any other feature
being built yet.

**Acceptance Scenarios**:

1. **Given** the app is freshly opened, **When** the doctor enters the
   correct static username and password, **Then** the doctor is granted
   access to the app.
2. **Given** the app is freshly opened, **When** the doctor enters an
   incorrect username or password, **Then** access is denied and a clear
   error message is shown, with no indication of which field was wrong.
3. **Given** the doctor is logged in and no backup has been taken in the
   last 5 days, **When** the doctor chooses to log out, **Then** a
   dismissible reminder to back up is shown before logout completes.
4. **Given** the backup reminder is shown at logout, **When** the doctor
   dismisses/cancels it, **Then** logout still proceeds and the app can
   still be closed.
5. **Given** a backup was taken within the last 5 days, **When** the doctor
   logs out, **Then** no backup reminder is shown.
6. **Given** the doctor has forgotten the password, **When** the doctor
  chooses Forgot Password and supplies the configured recovery answer and a
  matching new password, **Then** the password is replaced and the doctor
  can log in with the new password.
7. **Given** the doctor supplies an incorrect recovery answer, **When** the
  doctor attempts password reset, **Then** the password remains unchanged and
  a generic reset failure is shown.

---

### User Story 2 - Manage Patient Records (Priority: P1)

The doctor can add a new patient, view an existing patient's profile, edit
their personal details, and quickly find a patient among all recorded
patients.

**Why this priority**: Without the ability to create and find patient
records, no other capability (history and printing) has
anywhere to attach — this is the foundation the entire app is built on.

**Independent Test**: Can be fully tested by adding a new patient with basic
personal details, then searching for and opening that patient's profile,
without any other feature being built yet.

**Acceptance Scenarios**:

1. **Given** the app is open, **When** the doctor adds a new patient with
   name, age, and contact details, **Then** the patient appears in the
   patient list and can be reopened later.
2. **Given** the doctor enters invalid patient information, **When** the
  doctor attempts to save the Add Patient form, **Then** the form remains
  open with all entered values preserved and a red, field-specific error
  message appears beneath each invalid field.
3. **Given** an existing patient list, **When** the doctor searches by name
   or partial name, **Then** matching patients are shown within the search
   results.
4. **Given** an open patient profile, **When** the doctor edits and saves a
   detail (e.g., updated phone number), **Then** the updated value persists
   and is shown the next time the profile is opened.
5. **Given** an open patient profile, **When** the doctor soft-deletes the
   patient after confirming, **Then** that patient no longer appears in
   the normal patient list, search results, or print flows.
6. **Given** the doctor opens the separate "Deleted Patients" view, **When**
   the view loads, **Then** only soft-deleted patients are shown, and they
   never appear mixed into or greyed out within the normal patient list.
7. **Given** the doctor is viewing a soft-deleted patient in that separate
   view, **When** the doctor restores it, **Then** the patient reappears
   in the normal patient list, search results, and print flows exactly as
   before deletion.
8. **Given** the doctor logs in successfully, **When** the landing patient
  list opens, **Then** the app is full-screen/maximized, the date-range
  filter is filled for the last 7 days, and matching patients are loaded
  automatically.
9. **Given** the doctor double-clicks or opens a patient from the landing
  list, **When** the patient profile appears, **Then** it opens as a new
  modal window above the landing page rather than replacing the landing
  page, and the landing page remains visible behind it.

---

### User Story 3 - Record Medical History Entries (Priority: P2)

The doctor can add dated medical history entries for a patient's visits,
capturing the visit date, a multiline diagnosis/complaint, prescription
text, and multiline clinical notes in one Medical History tab. The doctor
can double-click a prior history row to edit that specific entry in a popup
or soft-delete it so it no longer appears in the normal history list.

**Why this priority**: This is the core clinical record-keeping value of
the app — the reason the doctor is replacing the old system — but it
depends on patient records (User Story 2) already existing.

**Independent Test**: With a patient already on file, add a history entry
with diagnosis, prescription, and notes, then reopen the patient profile and
confirm the entry is visible. Double-click that history row, edit it in the
popup, save, and confirm the updated entry is visible; then soft-delete it
from the popup and confirm it no longer appears in the normal history list.

**Acceptance Scenarios**:

1. **Given** an existing patient, **When** the doctor adds a new medical
  history entry with a date, diagnosis, prescription text, and notes,
  **Then** it appears in that patient's history list in chronological order.
2. **Given** the doctor is entering medical history, **When** the form is
  shown, **Then** Date, Diagnosis, Prescription, and Notes labels are
  visible; Date remains compact, Diagnosis is a multiline text area next to
  Date, Prescription and Notes are full-width multiline text areas below
  them, and the history list appears below those entry fields.
3. **Given** an existing history entry, **When** the doctor double-clicks
  its row, **Then** an edit popup opens with the entry's date, diagnosis,
  prescription, and notes already populated.
4. **Given** the edit popup is open for a history entry, **When** the doctor
  changes fields and saves, **Then** that history entry is updated and the
  refreshed history list shows the new values.
5. **Given** the edit popup is open for a history entry, **When** the doctor
  chooses soft-delete and confirms, **Then** the entry is hidden from the
  normal history list but remains present in the database and backup data.

---

### User Story 4 - Back Up and Restore All Patient Data (Priority: P3)

The doctor can export a full backup of all patient data to a file location
of their choosing (e.g., a USB drive), and later restore that exact data set
on a different installation of the app (typically after replacing their
laptop).

**Why this priority**: This is not needed for daily clinical use, but it is
the doctor's only safety net against data loss and their only supported path
to move to a new machine — critical, but used occasionally rather than
constantly.

**Independent Test**: With some patients already on file, trigger export,
choose a save location, then on a fresh/empty instance of the app trigger
restore and select that same file — confirm all patients and history entries,
including prescription and notes content, are present afterward.

**Acceptance Scenarios**:

1. **Given** the app has patient data, **When** the doctor chooses to back
   up, **Then** a real OS save-location dialog appears, letting the doctor
   pick exactly where the backup file is written, and the resulting file is
   a plain, human-readable SQL dump of all data.
2. **Given** a previously exported backup file, **When** the doctor chooses
   to restore, **Then** a real OS file-picker dialog appears, letting the
   doctor select the file, and the app rebuilds all patient data from it.
3. **Given** a backup or restore operation completes, **When** it finishes
   (successfully or not), **Then** the doctor sees a clear on-screen
   confirmation of success or a clear explanation of failure.
4. **Given** a backup or restore operation is running, **When** it performs
  database work, **Then** the UI remains responsive and shows an in-progress
  indicator until the operation succeeds or fails.
5. **Given** the doctor chooses H2 Native Backup, **When** export completes,
  **Then** the app writes a complete H2-specific schema-and-data script
  without destructive DROP statements.
6. **Given** the doctor chooses H2 Native Restore, **When** the restore is
  confirmed, **Then** the app creates an ANSI safety backup, validates the H2
  script in a fresh temporary database, and replaces the live database only
  after validation succeeds.

---

### User Story 5 - Print a Patient Handout (Priority: P4)

The doctor can print a clean, professional patient handout using the
native Windows print dialog, choosing which optional sections (medical
history and current prescription text from the latest visible history
entry) to include, while patient and clinic information are always included
automatically.

**Why this priority**: Printing is a frequently used convenience for
patients to take away information, but it depends on patient data and
optional history/prescription content already existing, so it is built last.

**Independent Test**: With a patient that has history and prescription text
on file, open print, select a subset of optional sections,
and confirm the native print preview shows only patient/clinic info plus
the chosen sections, formatted as a readable handout rather than a raw data
dump.

**Acceptance Scenarios**:

1. **Given** an open patient profile, **When** the doctor starts printing,
   **Then** a checklist of optional sections (Patient History, Current
  Prescription, Include History Notes) is shown before the native print dialog
   opens.
2. **Given** the doctor selects any combination of optional sections
   (including none), **When** the print dialog opens, **Then** the preview
   always includes patient personal information and the doctor's clinic
   information regardless of selection.
3. **Given** the doctor confirms printing, **When** the native Windows
   print dialog appears, **Then** the doctor can choose printer, paper
   size, and copies exactly as with any other Windows application.
4. **Given** Patient History is selected but Include History Notes is not
  selected, **When** the print preview is rendered, **Then** history dates
  and diagnoses appear without notes text.
5. **Given** Patient History and Include History Notes are both selected,
  **When** the print preview is rendered, **Then** the notes saved on each
  printed history entry are included.
6. **Given** the doctor opens print from a patient profile, **When** the
  print preview appears, **Then** it opens as a modal window above the
  patient profile and the patient profile remains visible behind it.

---

### Edge Cases

- What happens when the doctor tries to restore a backup file that is
  missing, corrupted, or not a valid export from this application? The
  system must reject the restore and show a clear failure message without
  altering existing data.
- What happens when the doctor restores a backup onto a machine that
  already has existing patient data? The doctor must be warned that restore
  will replace current data before it proceeds.
- How does the system handle printing a patient with no history or no
  prescription text in visible history entries? The corresponding optional
  section must be omitted or shown as empty rather than causing an error.
- How does the system handle backup export when there is not enough disk
  space or the chosen location is not writable? The doctor must see a clear
  failure message and no partial/corrupt backup file should be left behind.
- What happens if the doctor searches for a patient with no matches? The
  system must show a clear "no results" state rather than an error or a
  blank screen.
- What happens if the doctor closes the app mid-edit (e.g., while adding a
  history entry)? Unsaved changes for that entry must be discarded without
  corrupting previously saved data.
- What happens if the doctor closes the history edit popup without saving?
  Unsaved edits must be discarded and the existing history entry must remain
  unchanged.
- What happens if a history entry is soft-deleted? It must be hidden from
  the normal medical history list and printing, but retained in the
  database and full SQL backups.
- What happens if the doctor tries to close the app and no backup has ever
  been taken? The app must show a warning recommending a backup before
  closing, but must still allow the doctor to close without one if they
  choose to.
- What happens if the doctor enters wrong login credentials repeatedly?
  The system must keep showing a clear error and denying access; it must
  not lock the doctor out permanently, since there is no administrator to
  unlock the single account.
- What happens if the doctor logs out and no backup has been taken within
  the last 5 days? The system must show a dismissible reminder to back up,
  which the doctor can cancel to proceed with logout/closing anyway.
- What happens if the doctor restores a patient whose phone number or other
  identifying detail now matches a different, newer patient? The system
  must still restore the record as-is and let the doctor resolve any
  duplication manually; it must not block or silently merge the restore.
- What happens if patient form fields fail validation? The Add Patient form
  must remain open, preserve all entered values, and show an inline red error
  beneath each invalid field. Invalid phone values must be rejected unless
  they contain exactly 10 digits; blank email is allowed, but a non-blank
  email must be valid; the date of birth must be more than one year ago.
- What happens if an existing patient has a gender value outside the current
  three choices? The existing value must remain selectable during editing so
  an unchanged record can be saved; new records and changed values must use
  Male, Female, or Undisclosed.
- What happens if no patients match the default last-7-days landing filter?
  The landing page must still show the filled date range and an explicit no
  results state rather than appearing empty or broken.
- What happens if a modal patient, print, or history-edit window is open and
  the doctor clicks the window behind it? The modal window must keep focus
  and block interaction with the previous window until closed, while the
  previous window stays visible behind it.
- What happens if Include History Notes is selected without Patient History?
  The notes checkbox must be disabled, ignored, or treated as dependent on
  Patient History so notes never print as an orphan section.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow the doctor to create a new patient record
  with the following personal information: full name, date of birth,
  gender, address, emergency contact, phone number (required), and email
  address (optional).
- **FR-001a**: The Add Patient form MUST present gender as a dropdown with
  exactly these choices: Male, Female, and Undisclosed.
- **FR-001b**: The system MUST reject a patient phone number unless it
  contains exactly 10 digits, MUST allow a blank optional email address but
  reject a non-blank email with invalid format, and MUST reject a date of
  birth that is null, today, in the future, or no more than one year ago.
- **FR-001c**: The Add Patient form MUST display validation errors inline in
  red text beneath each invalid field, keep the form open, and preserve all
  entered values when validation fails. A validation attempt MUST be able to
  show multiple field errors at the same time.
- **FR-002**: System MUST allow the doctor to view and edit an existing
  patient's personal information.
- **FR-003**: System MUST allow the doctor to search/find patients by name
  (partial match) or phone number, and MUST allow the doctor to optionally
  narrow search results by a date range applied to patients' prior medical
  history entries.
- **FR-003a**: After successful login, the landing patient list MUST pre-fill
  the date-range filter with the last 7 days (today and previous 6 days),
  automatically execute the patient search using that range, and display
  matching patients or an explicit no-results state.
- **FR-004**: System MUST allow the doctor to add a dated medical history
  entry to a patient's record, each entry capturing a date, a multiline
  diagnosis/complaint, prescription text, and multiline free-text notes, and
  MUST allow the doctor to view all non-deleted prior history entries for
  that patient in chronological order.
- **FR-004a**: The Medical History tab MUST show visible field labels for
  Date, Diagnosis, Prescription, and Notes, rather than relying only on
  placeholder/prompt text.
- **FR-005**: System MUST present prescription as a multiline text field on
  the Medical History tab only, stored with the corresponding medical
  history entry; the separate Prescription tab/screen MUST NOT exist.
- **FR-006**: System MUST NOT provide a separate Personal Notes tab/screen;
  clinical notes are captured as the notes field on each medical history
  entry.
- **FR-006a**: System MUST allow the doctor to double-click a medical
  history row to open an edit popup populated with that entry's date,
  diagnosis, prescription, and notes, and MUST allow the doctor to save
  edits or soft-delete that specific entry from the popup.
- **FR-006b**: System MUST soft-delete medical history entries by hiding
  them from the normal history list and printing while retaining them in the
  database and full backup exports.
- **FR-007**: System MUST allow the doctor to export a full backup of all
  patient data as a plain SQL dump file (using INSERT statements), triggered
  on demand from within the app; the backup MUST include every patient's
  personal information and medical history entries, including soft-deleted
  history entries and their prescription/notes content, with nothing
  excluded as "private" or optional.
- **FR-008**: System MUST present a real OS-level save-location dialog when
  the doctor triggers a backup export, allowing the doctor to choose the
  exact destination (e.g., a USB drive or folder).
- **FR-009**: System MUST allow the doctor to restore all patient data from
  a previously exported SQL backup file, triggered on demand from within the
  app.
- **FR-009b**: System MUST provide a separate H2-native backup export using
  H2's `SCRIPT NOSETTINGS` behavior, with a distinct H2 backup marker and no
  destructive DROP statements. This format is H2-version-specific and is
  separate from the portable ANSI SQL backup.
- **FR-009c**: System MUST provide a separate H2-native restore action that
  accepts only the H2 backup format, creates an ANSI safety backup before
  changing data, executes `RUNSCRIPT` only against a fresh temporary H2
  database, validates that database, and replaces the live database only
  after successful validation. A failed restore MUST leave the live database
  unchanged.
- **FR-009a**: System MUST warn the doctor if they attempt to close the app
  and no backup has ever been taken, while still allowing the doctor to
  close the app without backing up if they choose to proceed.
- **FR-010**: System MUST present a real OS-level file-picker dialog when
  the doctor triggers a restore, allowing the doctor to select the backup
  file.
- **FR-011**: System MUST warn the doctor before a restore replaces any
  existing patient data, and MUST require explicit confirmation to proceed.
- **FR-012**: System MUST validate that a selected restore file is a
  well-formed backup produced by this application and MUST reject invalid
  or corrupted files with a clear error, leaving existing data unchanged.
- **FR-013**: System MUST allow the doctor to print a patient handout using
  the native Windows print dialog (printer, paper size, and copies selection
  as provided by the operating system).
- **FR-014**: System MUST show the doctor a checklist of optional print
  sections (Patient History, Current Prescription, Include History Notes)
  before printing, letting the doctor choose which to include. Include
  History Notes MUST apply only to printed Patient History entries and MUST
  NOT create a separate Personal Notes section.
- **FR-015**: System MUST always include patient personal information and
  the doctor's clinic information in every printed handout, regardless of
  which optional sections are selected.
- **FR-016**: System MUST format printed handouts as a clean, readable
  document rather than a raw listing of stored data fields.
- **FR-017**: System MUST give the doctor clear, explicit success or failure
  feedback after every backup, restore, and print action.
- **FR-017a**: System MUST run backup and restore database work away from the
  JavaFX application thread and MUST show a visible progress indicator while
  the operation is active.
- **FR-018**: System MUST operate entirely on a single machine with no
  network, cloud, or multi-machine synchronization capability.
- **FR-019**: System MUST require the doctor to log in with a single static
  username and password (stored in the local database) before accessing any
  other feature, and MUST provide an explicit logout action; no multi-user
  accounts, roles, or network-based authentication are permitted.
- **FR-019b**: System MUST provide a Forgot Password flow that verifies the
  configured recovery answer using a one-way BCrypt hash before replacing the
  password hash. The recovery answer MUST NOT be stored as plaintext, and
  resetting the password MUST NOT modify patient or clinic data.
- **FR-019a**: System MUST show a dismissible reminder to back up when the
  doctor logs out if no backup has been taken within the last 5 days, and
  MUST allow the doctor to cancel that reminder and still complete
  logout/close the app.
- **FR-020**: System MUST allow the doctor to soft-delete a patient record
  (e.g., a duplicate or mistaken entry) via an explicit confirmation step;
  a soft-deleted patient MUST be hidden from search, lists, and printing,
  but its data and all medical history entries MUST NEVER be physically
  removed from the database, and MUST still be included in full backups.
- **FR-020a**: System MUST provide a separate, doctor-initiated view for
  browsing soft-deleted patients (e.g., a dedicated "Deleted Patients"
  screen or an explicit toggle), which MUST be hidden by default so
  deleted records never appear alongside, or visually clutter, normal
  patient search/list/print views. From this view, the doctor MUST be able
  to restore a soft-deleted patient, which MUST make that patient and its
  non-deleted medical history entries fully visible again in normal search,
  lists, and printing.
- **FR-021**: System MUST prompt the doctor to set an initial username and
  password, and the clinic profile (doctor name, clinic name, contact
  details, registration number), during first run, before any login is
  possible. No default or pre-seeded credentials MUST ship with the
  application.
- **FR-021a**: First-run credential setup MUST initialize the recovery-answer
  hash for the single credential row so password reset is available after
  setup and after migration of an existing V1 database.
- **FR-022**: After login, the primary landing window MUST open in
  full-screen/maximized mode.
- **FR-023**: Opening a patient from the landing page MUST open a new modal
  patient-profile window owned by the landing window, rather than replacing
  the landing page. The modal window MUST block interaction with the landing
  page until closed and MUST leave the landing page visible behind it.
- **FR-024**: Opening print preview from a patient profile MUST open a new
  modal print window owned by the patient-profile window. The print window
  MUST block interaction with the patient profile until closed and MUST leave
  the patient-profile window visible behind it.
- **FR-025**: Modal windows opened on top of another window, including
  patient profile, print preview, and medical history edit, MUST be stacked
  as owned modal windows and sized/positioned so the previous window remains
  visible behind the active modal.
- **FR-026**: All application buttons MUST use relevant non-default colors
  by action category, such as primary/save, edit/open, print, delete,
  restore, backup, cancel/back, and destructive actions.

### Key Entities

- **Doctor Credential**: The single static username/password pair stored in
  the local database that gates access to the app; it also stores a one-way
  recovery-answer hash used only for password reset. There is exactly one set
  of credentials, with no additional accounts or roles.
- **Patient**: A person under the doctor's care; holds personal information
  (full name, date of birth, gender, address, emergency contact, required
  phone number, optional email address) and is the parent record for medical
  history entries. Includes a soft-delete flag so a deleted patient is hidden
  from normal use but never physically removed. Patient gender values for new
  or changed records are Male, Female, or Undisclosed. Phone numbers contain
  exactly 10 digits; email is optional but must be valid when present; date of
  birth must be more than one year ago.
- **Medical History Entry**: A dated record of a visit or clinical event for
  a patient, containing a date, multiline diagnosis/complaint, prescription
  text, multiline notes, and a soft-delete flag; a patient may have any
  number of history entries.
- **Doctor/Clinic Profile**: The doctor's own static identifying
  information (name, clinic name, contact details, registration number)
  included on every printed handout.
- **Backup File**: A plain SQL dump (INSERT statements) representing a full,
  point-in-time export of all patient and medical history data, including
  soft-deleted rows.
- **H2 Native Backup File**: An H2-version-specific schema-and-data script
  with a distinct application marker, intended only for the controlled H2
  restore flow and not for cross-database migration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A doctor can register a new patient and locate that patient
  again via search in under 1 minute combined, without any training.
- **SC-002**: A doctor can record a new medical history entry with date,
  diagnosis, prescription, and notes for an existing patient in under 30
  seconds.
- **SC-003**: A full backup can be exported and, when restored on a
  different installation, results in 100% of the originally backed-up
  patient records and history entries, including prescription and notes
  content, being present and correct.
- **SC-004**: A doctor can produce a print-ready patient handout, choosing
  optional sections, in under 1 minute from opening the patient's profile.
- **SC-005**: 100% of backup, restore, and print actions display an
  explicit success or failure message to the doctor — none complete
  silently.
- **SC-006**: A doctor with no prior technical training can complete the
  full workflow (log in → add patient → record medical history with
  prescription/notes → back up → log out) unassisted on first attempt.
- **SC-007**: 100% of login attempts with correct credentials succeed, and
  100% of attempts with incorrect credentials are rejected with a clear
  error, with no permanent lockout of the single account.
- **SC-008**: 100% of logouts that occur more than 5 days after the last
  successful backup show a backup reminder that the doctor can dismiss
  without being blocked from logging out.
- **SC-009**: 100% of valid password-reset attempts with the configured
  recovery answer allow login with the new password, and 100% of invalid
  recovery answers leave the existing password unchanged.
- **SC-010**: 100% of backup and restore operations show an in-progress
  indicator while database work is active and leave the JavaFX UI responsive.
- **SC-011**: On every successful login, the landing patient list opens
  full-screen/maximized with the last-7-days date range filled and matching
  patients loaded automatically.
- **SC-012**: 100% of patient-profile, print-preview, and history-edit
  windows opened from another app window behave as modal stacked windows,
  blocking the parent window while keeping it visible behind the active
  window.

## Assumptions

- Exactly one doctor uses the application, on exactly one Windows laptop at
  any given time; the app is never run on two machines simultaneously
  against the same data.
- Machine migration is a manual, occasional action (export on old machine,
  fresh install on new machine, restore); no live sync or concurrent access
  is required or supported.
- Practice size is that of a single solo doctor's patient panel (expected
  up to tens of thousands of patient records); no specific numeric scale
  requirement was given.
- Prescription text is retained as part of each medical history entry; the
  most recent non-deleted history entry with prescription text is treated as
  the current prescription for printing purposes.
- Appointment scheduling, billing/invoicing, and multi-doctor/staff
  accounts are out of scope for this feature.
- The doctor's clinic/profile information (name, clinic, contact,
  registration number) is entered once and reused on every printed handout,
  rather than re-entered per patient.
- Deleting a patient record hides that patient (soft delete) rather than
  physically erasing it; the doctor can browse soft-deleted patients in a
  separate "Deleted Patients" view/toggle (hidden by default so it never
  clutters normal views) and restore any of them back to full visibility,
  even though the data was never physically removed from the database or
  backups. Individual medical history entries can also be soft-deleted from
  the history edit popup; they stay in the database and backups but remain
  hidden from normal history lists and printing.
- The application ships with no default or pre-seeded login credentials;
  the doctor sets the static username and password during an interactive
  first-run setup screen (FR-021), before the first login is possible. The
  recovery answer is a fixed local recovery mechanism, stored only as a
  BCrypt hash; it is not equivalent to multi-factor authentication or an
  administrator-managed account recovery system.
- The landing page's last-7-days default uses the existing medical-history
  date-range filter semantics: it loads patients with non-deleted history
  entries dated from today minus 6 days through today, inclusive. The doctor
  may clear or change the range after landing.
