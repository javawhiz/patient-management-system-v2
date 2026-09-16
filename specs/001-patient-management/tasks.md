---
description: "Task list for feature implementation"
---

# Tasks: Patient Management System (Solo Doctor, Single Machine)

**Input**: Design documents from `/specs/001-patient-management/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/service-contracts.md, quickstart.md

**Tests**: Include JUnit 5 service/repository tests and focused TestFX/controller validation tasks because the specification defines independent test scenarios and the implementation plan names JUnit 5, Mockito, and TestFX.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested independently. Paths target the existing JavaFX app under `app/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files and has no dependency on incomplete tasks in the same phase
- **[Story]**: User-story traceability label, required only for user-story phases
- Every task includes at least one exact file path

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify JavaFX desktop project structure, build tooling, and shared resources.

- [X] T001 Verify `app/pom.xml` uses Java 21, JavaFX 21, Hibernate/JPA, Flyway, H2, jBCrypt, JUnit 5, Mockito, and TestFX dependencies.
- [X] T002 Verify `app/pom.xml` pins H2 in `<h2.version>` and documents that H2 must not be upgraded without a migration plan.
- [X] T003 [P] Verify `app/pom.xml` supports `mvn javafx:run`, runnable JAR packaging, and Windows `jpackage` execution.
- [X] T004 [P] Verify `app/src/main/resources/META-INF/persistence.xml` uses embedded file-mode H2 and `hibernate.hbm2ddl.auto=validate`.
- [X] T005 [P] Verify `app/src/main/resources/logback.xml` and Java code under `app/src/main/java/com/clinic/pms/` do not log patient data.
- [X] T006 [P] Verify `app/src/main/resources/css/application.css` and `app/src/main/resources/css/print.css` are loaded by JavaFX views.
- [X] T007 Run `cd app && mvn clean compile` and fix setup failures in `app/pom.xml` or `app/src/main/resources/META-INF/persistence.xml`.

**Checkpoint**: The app compiles and shared infrastructure is ready.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared persistence, navigation, modal-window helpers, and service wiring used by multiple stories.

- [X] T008 Update `app/src/main/resources/db/migration/V1__init_schema.sql` so fresh installs create current tables including `medical_history_entry` with prescription, notes, `is_deleted`, `deleted_at`, `created_at`, and `updated_at`.
- [X] T009 Update `app/src/main/resources/db/migration/V2__add_recovery_answer.sql` so existing credential rows receive a BCrypt `recovery_answer_hash` without plaintext recovery answers.
- [X] T010 Update `app/src/main/resources/db/migration/V3__merge_prescriptions_notes_into_history.sql` to migrate old `prescription` and `personal_note` data into `medical_history_entry` and tolerate fresh schemas.
- [X] T011 Update `app/src/main/resources/META-INF/persistence.xml` to register only `DoctorCredential`, `DoctorClinicProfile`, `BackupMetadata`, `Patient`, and `MedicalHistoryEntry`.
- [X] T012 [P] Update `app/src/main/java/com/clinic/pms/config/PersistenceConfig.java` and `app/src/main/java/com/clinic/pms/config/AppPaths.java` for file-mode H2, Flyway, and shutdown.
- [X] T013 Update `app/src/main/java/com/clinic/pms/config/AppContext.java` to wire current services only and exclude removed prescription/personal-note services.
- [X] T014 [P] Create `app/src/main/java/com/clinic/pms/view/ModalWindowHelper.java` to open owned modal stages, inject `AppContext`, apply CSS, block owners, and keep owner windows visible behind child windows.
- [X] T015 [P] Update `app/src/main/java/com/clinic/pms/view/SceneNavigator.java` with helpers for primary scene navigation and modal child-window opening.
- [X] T016 [P] Update `app/src/main/java/com/clinic/pms/view/AlertHelper.java` and `app/src/main/java/com/clinic/pms/view/LogoutHelper.java` for clear dialogs without logging patient data.
- [X] T017 [P] Update `app/src/main/resources/css/application.css` with non-default action button classes for primary/save, open/edit, print, backup, restore, delete, cancel, and back actions.
- [X] T018 [P] Update `app/src/main/resources/css/application.css` with stable layout classes for landing filters, modal windows, medical-history labels/text areas, and stacked-window sizing.
- [X] T019 [P] Update `app/src/main/resources/css/print.css` so patient and clinic information are always readable in printed output.
- [X] T020 Add or update `app/src/test/java/com/clinic/pms/service/ServiceSmokeTest.java` to exercise current service wiring without removed prescription/personal-note services.

**Checkpoint**: Foundation supports current data model, modal windows, and shared styling.

---

## Phase 3: User Story 1 - Log In and Log Out (Priority: P1) MVP

**Goal**: The doctor can set up local credentials, log in, reset a forgotten password, open the landing page full-screen/maximized, and log out with a non-blocking backup reminder.

**Independent Test**: Launch with an empty database, complete setup, log in, confirm landing is full-screen/maximized, reject bad login, reset password, and log out with backup reminder behavior.

### Tests for User Story 1

- [X] T021 [P] [US1] Add `app/src/test/java/com/clinic/pms/service/AuthServiceTest.java` covering first-run credential creation, singleton credential behavior, successful login, wrong username, and wrong password.
- [X] T022 [P] [US1] Add `app/src/test/java/com/clinic/pms/service/PasswordRecoveryTest.java` covering successful reset, incorrect recovery answer, blank new password rejection, and no patient/clinic mutation.
- [X] T023 [P] [US1] Add `app/src/test/java/com/clinic/pms/service/BackupReminderServiceTest.java` covering never-backed-up, backed-up within 5 days, and backed-up more than 5 days.
- [X] T024 [P] [US1] Add `app/src/test/java/com/clinic/pms/view/LoginControllerTest.java` or equivalent TestFX/controller coverage for invalid-login messaging and Forgot Password controls.

### Implementation for User Story 1

- [X] T025 [US1] Update `app/src/main/java/com/clinic/pms/entity/DoctorCredential.java` with required `username`, `passwordHash`, `recoveryAnswerHash`, `createdAt`, and `updatedAt` fields.
- [X] T026 [P] [US1] Update `app/src/main/java/com/clinic/pms/repository/DoctorCredentialRepository.java` to enforce a singleton credential row and generic lookup failure behavior.
- [X] T027 [US1] Update `app/src/main/java/com/clinic/pms/service/AuthService.java` with `isInitialized`, `createInitialCredential`, `login`, and `resetPassword` using BCrypt hashes only.
- [X] T028 [US1] Update `app/src/main/java/com/clinic/pms/entity/BackupMetadata.java`, `app/src/main/java/com/clinic/pms/repository/BackupMetadataRepository.java`, and `app/src/main/java/com/clinic/pms/service/BackupReminderService.java` for 5-day reminder rules.
- [X] T029 [P] [US1] Update `app/src/main/resources/fxml/Setup.fxml` and `app/src/main/java/com/clinic/pms/view/SetupController.java` to collect credentials, recovery answer, and clinic profile.
- [X] T030 [P] [US1] Update `app/src/main/resources/fxml/Login.fxml` and `app/src/main/java/com/clinic/pms/view/LoginController.java` for generic invalid-login and Forgot Password behavior.
- [X] T031 [US1] Update `app/src/main/java/com/clinic/pms/view/LogoutHelper.java` so backup reminders are dismissible and logout always continues.
- [X] T032 [US1] Update `app/src/main/java/com/clinic/pms/PatientManagementApp.java`, `app/src/main/java/com/clinic/pms/view/SceneNavigator.java`, and `app/src/main/java/com/clinic/pms/view/LoginController.java` so successful login opens the landing patient list full-screen or maximized.

**Checkpoint**: US1 is independently functional and is the suggested MVP.

---

## Phase 4: User Story 2 - Manage Patient Records (Priority: P1)

**Goal**: The doctor can add, validate, view, edit, search, soft-delete, restore, and open patients from a landing page with default last-7-days filtering.

**Independent Test**: Log in, confirm the last-7-days filter is filled and searched automatically, add a patient, verify inline validation, search, open the patient in a modal profile window, edit, soft-delete, and restore.

### Tests for User Story 2

- [X] T033 [P] [US2] Update `app/src/test/java/com/clinic/pms/service/PatientValidatorTest.java` for phone, email, date of birth, gender, and legacy-gender validation.
- [X] T034 [P] [US2] Add `app/src/test/java/com/clinic/pms/service/PatientServiceTest.java` covering create, update, soft delete, restore, listDeleted, and search with date ranges.
- [ ] T035 [P] [US2] Add `app/src/test/java/com/clinic/pms/view/PatientFormDialogTest.java` or equivalent TestFX coverage for inline red validation errors and preserved values.
- [ ] T036 [P] [US2] Add `app/src/test/java/com/clinic/pms/view/PatientListControllerTest.java` or equivalent coverage for default last-7-days filters, automatic landing search, no-results state, and modal patient opening.

### Implementation for User Story 2

- [X] T037 [US2] Update `app/src/main/java/com/clinic/pms/entity/Patient.java` with required demographics, soft-delete fields, and validation-compatible personal information fields.
- [X] T038 [P] [US2] Update `app/src/main/java/com/clinic/pms/repository/PatientRepository.java` with partial-name search, phone search, non-deleted history date filtering, deleted-patient exclusion, and stable ordering.
- [X] T039 [US2] Update `app/src/main/java/com/clinic/pms/service/PatientValidator.java` to return all validation errors at once for phone, email, date of birth, and gender.
- [X] T040 [US2] Update `app/src/main/java/com/clinic/pms/service/PatientService.java` so create, update, search, softDelete, listDeleted, and restore match `specs/001-patient-management/contracts/service-contracts.md`.
- [X] T041 [US2] Update `app/src/main/resources/fxml/PatientList.fxml` and `app/src/main/java/com/clinic/pms/view/PatientListController.java` with name/phone/from/to filters, results, Add Patient, Deleted Patients navigation, and no-results state.
- [X] T042 [US2] Update `app/src/main/java/com/clinic/pms/view/PatientListController.java` to initialize fromDate to today minus 6 days, toDate to today, and automatically run search after login.
- [X] T043 [US2] Update `app/src/main/java/com/clinic/pms/view/PatientListController.java`, `app/src/main/java/com/clinic/pms/view/SceneNavigator.java`, and `app/src/main/java/com/clinic/pms/view/ModalWindowHelper.java` so opening a patient uses an owned modal `PatientProfile.fxml` window.
- [X] T044 [P] [US2] Update `app/src/main/resources/fxml/PatientProfile.fxml` and `app/src/main/java/com/clinic/pms/view/PatientProfileController.java` for patient display/editing and delete confirmation.
- [X] T045 [P] [US2] Update `app/src/main/java/com/clinic/pms/view/PatientFormDialog.java` for gender dropdown, four-row address area, inline errors, and preserved values.
- [X] T046 [US2] Update `app/src/main/resources/fxml/DeletedPatients.fxml` and `app/src/main/java/com/clinic/pms/view/DeletedPatientsController.java` to list and restore only soft-deleted patients.
- [X] T047 [P] [US2] Add action button style classes in `app/src/main/resources/fxml/PatientList.fxml`, `app/src/main/resources/fxml/PatientProfile.fxml`, `app/src/main/resources/fxml/DeletedPatients.fxml`, and `app/src/main/java/com/clinic/pms/view/PatientFormDialog.java`.

**Checkpoint**: US2 is complete and independently testable after foundation.

---

## Phase 5: User Story 3 - Record Medical History Entries (Priority: P2)

**Goal**: The doctor can add labeled medical history entries, edit entries in a modal dialog, and soft-delete individual entries.

**Independent Test**: With a patient open in a modal profile window, add a labeled history entry, verify order, double-click it, edit/save in a modal dialog, then soft-delete it and confirm it is hidden.

### Tests for User Story 3

- [X] T048 [P] [US3] Add `app/src/test/java/com/clinic/pms/service/MedicalHistoryServiceTest.java` covering add, list, update, getForEdit, soft delete, and current-prescription selection.
- [X] T049 [P] [US3] Add `app/src/test/java/com/clinic/pms/repository/MedicalHistoryRepositoryTest.java` covering descending date order and soft-delete exclusion.
- [ ] T050 [P] [US3] Add `app/src/test/java/com/clinic/pms/view/MedicalHistoryEditControllerTest.java` or equivalent TestFX coverage for populated fields, save, cancel, soft-delete, and modal ownership.
- [ ] T051 [P] [US3] Add `app/src/test/java/com/clinic/pms/view/PatientProfileControllerTest.java` or equivalent coverage for Medical History labels, multiline layout, add-entry behavior, and double-click modal opening.

### Implementation for User Story 3

- [X] T052 [US3] Update `app/src/main/java/com/clinic/pms/entity/MedicalHistoryEntry.java` with patientId, entryDate, diagnosis, prescription, notes, soft-delete fields, createdAt, and updatedAt.
- [X] T053 [P] [US3] Update `app/src/main/java/com/clinic/pms/repository/MedicalHistoryRepository.java` with active list, active find by patient/id, save/update, soft-delete support, and current-prescription lookup.
- [X] T054 [US3] Update `app/src/main/java/com/clinic/pms/service/MedicalHistoryService.java` with listForPatient, add, getForEdit, update, softDelete, and getCurrentPrescriptionText.
- [X] T055 [US3] Update `app/src/main/resources/fxml/PatientProfile.fxml` so Medical History has visible Date, Diagnosis, Prescription, and Notes labels plus the requested multiline layout.
- [X] T056 [US3] Update `app/src/main/java/com/clinic/pms/view/PatientProfileController.java` so adding history uses date, diagnosis, prescription, and notes and reloads the history list.
- [X] T057 [P] [US3] Update `app/src/main/resources/fxml/MedicalHistoryEdit.fxml` with visible labels and action-color classes for Save, Cancel, and Soft Delete buttons.
- [X] T058 [P] [US3] Update `app/src/main/java/com/clinic/pms/view/MedicalHistoryEditController.java` to populate selected entries, save edits, discard cancel/close, soft-delete after confirmation, and run as owned modal.
- [X] T059 [US3] Update `app/src/main/java/com/clinic/pms/view/PatientProfileController.java` and `app/src/main/java/com/clinic/pms/view/ModalWindowHelper.java` so double-click opens `MedicalHistoryEdit.fxml` as a stacked owned modal.
- [X] T060 [US3] Remove separate prescription/personal-note code paths from `app/src/main/java/com/clinic/pms/config/AppContext.java`, `app/src/main/resources/fxml/PatientProfile.fxml`, and `app/src/main/java/com/clinic/pms/view/PatientProfileController.java`.
- [X] T061 [P] [US3] Update `app/src/main/resources/css/application.css` so Medical History labels, text areas, and history lists have stable responsive heights.

**Checkpoint**: US3 is complete and independently testable with patient records available.

---

## Phase 6: User Story 4 - Back Up and Restore All Patient Data (Priority: P3)

**Goal**: The doctor can export and restore all patient data via native dialogs, using portable SQL and separate H2-native backup, with background progress feedback.

**Independent Test**: With patient/history data, export/restore portable SQL, reject corrupted files, export/restore H2-native backup safely, and confirm UI progress remains visible.

### Tests for User Story 4

- [X] T062 [P] [US4] Add `app/src/test/java/com/clinic/pms/service/BackupServiceTest.java` covering portable export of current tables, soft-deleted history, prescription text, notes, and metadata.
- [X] T063 [P] [US4] Add `app/src/test/java/com/clinic/pms/service/RestoreServiceTest.java` or extend `BackupServiceTest.java` for restore success, invalid header rejection, and rollback.
- [X] T064 [P] [US4] Add or update `app/src/test/java/com/clinic/pms/service/H2NativeBackupServiceTest.java` for H2 marker, no destructive statements, fresh validation, safety backup, export recency update, and failed-restore preservation.
- [ ] T065 [P] [US4] Add `app/src/test/java/com/clinic/pms/view/BackupRestoreControllerTest.java` or equivalent coverage for progress visibility and action wiring.

### Implementation for User Story 4

- [X] T066 [US4] Update `app/src/main/java/com/clinic/pms/service/BackupService.java` so export writes ANSI `INSERT INTO` statements for current tables only, including soft-deleted history.
- [X] T067 [US4] Update `app/src/main/java/com/clinic/pms/service/BackupService.java` so restore validates the portable header, accepts only insert statements, runs transactionally, rolls back on failure, and returns patient count.
- [X] T068 [US4] Update `app/src/main/java/com/clinic/pms/service/BackupService.java`, `app/src/main/java/com/clinic/pms/service/H2NativeBackupService.java`, and `app/src/main/java/com/clinic/pms/service/BackupReminderService.java` so backup recency updates only after successful exports.
- [X] T069 [P] [US4] Update `app/src/main/java/com/clinic/pms/service/H2NativeBackupService.java` to export with `PMS_H2_BACKUP_V1`, H2 `SCRIPT NOSETTINGS`, security filtering, and destructive-statement rejection.
- [X] T070 [P] [US4] Update `app/src/main/java/com/clinic/pms/service/H2NativeBackupService.java` so H2 restore creates an ANSI safety backup, validates a fresh temporary database, and swaps live files only after success.
- [X] T071 [US4] Update `app/src/main/resources/fxml/BackupRestore.fxml` and `app/src/main/java/com/clinic/pms/view/BackupRestoreController.java` with native `FileChooser` flows for portable and H2 backup/restore.
- [X] T072 [US4] Update `app/src/main/java/com/clinic/pms/view/BackupRestoreController.java` to run backup/restore work off the JavaFX application thread and show progress while active.
- [X] T073 [P] [US4] Update `app/src/main/java/com/clinic/pms/view/BackupRestoreController.java` to show restore confirmation and clear success/failure feedback.
- [X] T074 [P] [US4] Add action button style classes in `app/src/main/resources/fxml/BackupRestore.fxml` for backup, restore, H2 backup, H2 restore, and back actions.
- [X] T075 [P] [US4] Update `app/src/main/java/com/clinic/pms/PatientManagementApp.java` so closing with no backup ever taken shows a dismissible warning and still allows close.

**Checkpoint**: US4 is complete and independently testable after patient/history data exists.

---

## Phase 7: User Story 5 - Print a Patient Handout (Priority: P4)

**Goal**: The doctor can print a clean handout through the native print dialog, selecting Patient History, Current Prescription, and Include History Notes while patient and clinic information always print.

**Independent Test**: Open print from a modal patient profile, confirm print preview opens as stacked modal, select History without notes, select History with notes, select Prescription only, and confirm native printing.

### Tests for User Story 5

- [ ] T076 [P] [US5] Add `app/src/test/java/com/clinic/pms/view/PrintPreviewControllerTest.java` or equivalent coverage for History, Prescription, Include History Notes dependency, mandatory patient/clinic info, and modal ownership.
- [X] T077 [P] [US5] Add `app/src/test/java/com/clinic/pms/service/ClinicProfileServiceTest.java` covering initial save, get, and update of required doctor/clinic profile fields.

### Implementation for User Story 5

- [X] T078 [US5] Update `app/src/main/java/com/clinic/pms/entity/DoctorClinicProfile.java` and `app/src/main/java/com/clinic/pms/repository/DoctorClinicProfileRepository.java` with required singleton clinic profile fields.
- [X] T079 [P] [US5] Update `app/src/main/java/com/clinic/pms/service/ClinicProfileService.java` with get, saveInitialProfile, and update behavior.
- [X] T080 [US5] Update `app/src/main/resources/fxml/PrintPreview.fxml` and `app/src/main/java/com/clinic/pms/view/PrintPreviewController.java` so optional controls are Patient History, Current Prescription, and Include History Notes with no Personal Notes section.
- [X] T081 [US5] Update `app/src/main/java/com/clinic/pms/view/PrintPreviewController.java` so Include History Notes is disabled or ignored unless Patient History is selected and notes print inline only when both are selected.
- [X] T082 [US5] Update `app/src/main/java/com/clinic/pms/view/PrintPreviewController.java` so Current Prescription uses `MedicalHistoryService.getCurrentPrescriptionText(long patientId)` and ignores soft-deleted history entries.
- [X] T083 [US5] Update `app/src/main/java/com/clinic/pms/view/PatientProfileController.java`, `app/src/main/java/com/clinic/pms/view/ModalWindowHelper.java`, and `app/src/main/java/com/clinic/pms/view/PrintPreviewController.java` so print preview opens as an owned modal child window above patient profile.
- [X] T084 [P] [US5] Update `app/src/main/java/com/clinic/pms/view/PrintPreviewController.java` to invoke `PrinterJob.showPrintDialog(stage)` and `PrinterJob.printPage(node)` through Java print APIs only.
- [X] T085 [P] [US5] Add action button style classes in `app/src/main/resources/fxml/PrintPreview.fxml` and ensure `app/src/main/resources/css/print.css` keeps printed output readable.

**Checkpoint**: US5 is complete and independently testable after patient/history data exists.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final UI consistency, documentation, packaging, and end-to-end validation.

- [X] T086 [P] Update `README.md` with current JavaFX build/run, backup/restore, password recovery, modal-window workflow, print-notes option, and Windows packaging instructions.
- [X] T087 [P] Review every FXML file under `app/src/main/resources/fxml/` and ensure all buttons use non-default action color classes from `app/src/main/resources/css/application.css`.
- [X] T088 [P] Review all Java files under `app/src/main/java/com/clinic/pms/` and remove any logging of patient names, contact details, diagnosis, prescription text, notes, or backup contents.
- [X] T089 Run `cd app && mvn clean test` and fix failures only in files touched by this task list.
- [X] T090 Run `cd app && mvn clean compile` and fix compile failures only in files touched by this task list.
- [X] T091 Run `cd app && mvn clean package` and verify `app/target/patient-management-system.jar` is produced.
- [ ] T092 Execute the validation scenarios in `specs/001-patient-management/quickstart.md`, including default landing filters, modal stacked windows, history labels, button colors, and Include History Notes printing.
- [ ] T093 On Windows or a Windows VM, run the `jpackage --type exe` flow documented in `specs/001-patient-management/quickstart.md` and verify the installed app launches without separate Java or JavaFX installation.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies; start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1 and blocks user-story work.
- **Phase 3 (US1)**: Depends on Phase 2 and is the MVP.
- **Phase 4 (US2)**: Depends on Phase 2 and is practically reached through US1 login.
- **Phase 5 (US3)**: Depends on US2 because medical history entries require patients and the patient profile window.
- **Phase 6 (US4)**: Depends on Phase 2; validation is most meaningful after US2 and US3 create data.
- **Phase 7 (US5)**: Depends on US2, US3, and clinic profile data from setup.
- **Phase 8 (Polish)**: Depends on desired story phases.

### User Story Dependencies

- **US1 (P1)**: No dependency on other stories after foundation.
- **US2 (P1)**: No dependency on other stories after foundation; provides patient/profile windows for US3, US4, and US5.
- **US3 (P2)**: Depends on US2.
- **US4 (P3)**: Can be implemented after foundation, but validates best with US2/US3 data.
- **US5 (P4)**: Depends on US2 and US3 for meaningful output.

### Within Each User Story

- Tests first, then entities/migrations, then repositories, then services, then FXML/controllers, then validation.
- Repository/service tasks must complete before controllers that call them.
- `ModalWindowHelper.java` must exist before patient-profile and print-preview modal conversion tasks.

---

## Parallel Execution Examples

### User Story 1

```bash
Task: "T021 [US1] Add AuthServiceTest in app/src/test/java/com/clinic/pms/service/AuthServiceTest.java"
Task: "T022 [US1] Add PasswordRecoveryTest in app/src/test/java/com/clinic/pms/service/PasswordRecoveryTest.java"
Task: "T023 [US1] Add BackupReminderServiceTest in app/src/test/java/com/clinic/pms/service/BackupReminderServiceTest.java"
```

### User Story 2

```bash
Task: "T034 [US2] Add PatientServiceTest in app/src/test/java/com/clinic/pms/service/PatientServiceTest.java"
Task: "T036 [US2] Add PatientListControllerTest in app/src/test/java/com/clinic/pms/view/PatientListControllerTest.java"
Task: "T047 [US2] Add action button style classes in patient FXML files"
```

### User Story 3

```bash
Task: "T048 [US3] Add MedicalHistoryServiceTest in app/src/test/java/com/clinic/pms/service/MedicalHistoryServiceTest.java"
Task: "T057 [US3] Update MedicalHistoryEdit.fxml with labels and action colors"
Task: "T061 [US3] Update application.css for Medical History layout"
```

### User Story 4

```bash
Task: "T062 [US4] Add BackupServiceTest in app/src/test/java/com/clinic/pms/service/BackupServiceTest.java"
Task: "T069 [US4] Update H2NativeBackupService export"
Task: "T074 [US4] Add action button styles in BackupRestore.fxml"
```

### User Story 5

```bash
Task: "T076 [US5] Add PrintPreviewControllerTest in app/src/test/java/com/clinic/pms/view/PrintPreviewControllerTest.java"
Task: "T081 [US5] Implement Include History Notes behavior"
Task: "T085 [US5] Add print preview button style classes"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: US1 login/logout/password recovery.
4. Stop and validate US1 independently.

### Incremental Delivery

1. Add Phase 4 (US2) next because patient records and landing page behavior are the base for the requested windowing flow.
2. Add Phase 5 (US3) to deliver labeled Medical History entry/edit behavior.
3. Add Phase 6 (US4) to keep backup/restore aligned with the clinical model.
4. Add Phase 7 (US5) to deliver modal print preview and Include History Notes printing.
5. Finish Phase 8 with UI consistency, tests, quickstart validation, documentation, and packaging.

### Parallel Team Strategy

1. One developer completes setup/foundation and `ModalWindowHelper.java`.
2. After foundation, US1 and US2 can proceed in parallel.
3. US3 starts after patient profile modal behavior is available.
4. US4 service tests can proceed while UI work is underway.
5. US5 starts after `MedicalHistoryService.getCurrentPrescriptionText(long patientId)` and modal patient-profile windows exist.

---

## Notes

- All tasks are unchecked by design because this file is regenerated from the current specification.
- Current implementation may already satisfy many tasks; `/speckit-implement` should verify and mark completed tasks `[X]` as it proceeds.
- Use native JavaFX `FileChooser` and Java print APIs only; do not add REST, browser, PDF, cloud sync, networking, or multi-user authentication work.
