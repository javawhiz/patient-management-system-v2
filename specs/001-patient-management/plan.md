# Implementation Plan: Patient Management System (Solo Doctor, Single Machine)

**Branch**: `001-patient-management` | **Date**: 2026-09-15 | **Spec**: [spec.md](./spec.md)

**Implementation Sync**: Updated 2026-09-16 to record patient-form validation,
choice-based gender, password recovery, dual backup formats, background backup
operations, progress feedback, and non-destructive inline error feedback
delivered during implementation. Updated again for default last-7-days landing
search, full-screen/maximized startup, modal stacked patient/print windows,
colored action buttons, labeled medical-history fields, and optional printing
of history notes.

**Input**: Feature specification from `/specs/001-patient-management/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

A single-doctor, single-machine patient management **desktop application**
replacing a 14-year-old Java Swing + MySQL system. The doctor logs in with a
static local credential, manages patient records (personal info, dated
medical history entries that include diagnosis, prescription text, and notes),
exports/restores full data as either a portable SQL dump or an H2-native
schema/data script via native OS dialogs, and prints clean patient handouts via
the OS-native print dialog. Delivered as a single JavaFX desktop application
(no browser, no HTTP server, no REST API), backed by a file-based H2
database with Flyway-managed schema, packaged as a Windows `.exe` via
`jpackage` with a bundled JVM + JavaFX runtime. Entirely offline; login is a
single local static-credential gate, not a networked auth system. After login,
the landing patient list opens full-screen/maximized with the last-7-days date
range pre-filled and matching patients loaded; patient profiles, print preview,
and medical-history edit dialogs open as stacked modal windows above their
owner windows.

## Technical Context

**Language/Version**: Java 21.

**Primary Dependencies**: JavaFX 21 (`javafx-controls`, `javafx-fxml`) for
the UI, Hibernate/JPA (persistence, used directly — no Spring Framework),
Flyway, H2 (embedded, file mode, version pinned), jBCrypt (or a small
hand-rolled BCrypt-compatible hasher) for the static-credential password
hash, Maven (build + packaging orchestration), `jpackage` (Windows `.exe`
with bundled JVM + JavaFX runtime).

**Storage**: H2 embedded database, file mode (not in-memory), data file
stored under the doctor's OS user-profile directory in a dedicated app
folder (e.g., `%USERPROFILE%\.patient-management-system\data\patientdb`).

**Testing**: JUnit 5 + Mockito for services/repositories; TestFX for
JavaFX UI/controller tests.

**Patient Form Validation**: `PatientService` is authoritative for patient
validation. The Add Patient dialog validates before closing and displays all
applicable field errors as red inline labels while preserving entered values.
Phone is exactly 10 digits; email is optional but must be valid when present;
date of birth must be more than one year ago; gender choices are Male,
Female, and Undisclosed. Existing unsupported gender values may be preserved
unchanged during edits.

**Windowing and Interaction**: The primary JavaFX stage enters full-screen or
maximized mode after login. Opening a patient from the landing page creates an
owned modal patient-profile stage rather than replacing the landing scene;
opening print from a patient profile creates another owned modal stage above
the profile. Medical-history edit remains an owned modal dialog. Each modal is
sized/positioned so the owner remains visible behind it and cannot be
interacted with until the modal closes.

**UI Styling**: Buttons use non-default, semantically relevant colors by
action category (primary/save, open/edit, print, backup/restore, delete,
restore, cancel/back). Medical History fields use visible labels in addition
to prompt text. Print preview exposes a Patient History checkbox, a Current
Prescription checkbox, and an Include History Notes checkbox that applies only
when Patient History is printed.

**Target Platform**: Windows 10/11 desktop, single machine, single user;
native JavaFX window — no browser involved.

**Project Type**: Single-project native desktop application (JavaFX +
JPA/Hibernate), packaged via `jpackage` — no client/server split.

**Performance Goals**: UI actions (patient search, open profile, save
history/prescription entry) complete and render within ~1 second under
typical solo-practice data volumes; opening and saving a history edit popup
feels immediate under typical solo-practice data volumes; backup
export/restore of a full practice's data completes within a few seconds to
low minutes depending on record count.

**Constraints**: No networking of any kind; authentication remains a single
local static credential with a local BCrypt recovery-answer reset; H2 version
is pinned at project start in `pom.xml` and never silently upgraded; the
default portable backup MUST be ANSI-compatible SQL, while the separately
labeled H2-native backup MAY use H2-specific syntax; H2-native restore MUST
use a fresh temporary database and an ANSI safety backup; printing MUST use
the Java `PrinterJob` API only, no PDF libraries; no Docker/containerization;
no Spring Boot, REST API, or browser-hosted UI.

**Scale/Scope**: Single solo-doctor practice; expected up to tens of
thousands of patient records, each with an unbounded number of medical
history entries containing diagnosis, prescription text, and notes; 5
prioritized user stories; 41 functional requirements.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|---|---|---|
| I. Single-Machine, Single-User Scope | No networking of any kind; static local login only, no multi-user/roles | PASS |
| II. Layered Architecture (View/Controller/Service/Repository) | JavaFX FXML views + controllers call services in-process; services call JPA repositories; no HTTP layer | PASS |
| III. No Patient Data in Logs | Logging config and code review MUST exclude patient fields and clinical entry content from any log statement | PASS (enforced in Phase 1 design + implementation review) |
| IV. Schema Versioning via Flyway (NON-NEGOTIABLE) | All schema changes as Flyway migrations under `src/main/resources/db/migration`; Hibernate `hbm2ddl.auto=validate`; H2 version pinned in `pom.xml` with a comment | PASS |
| V. Backup/Restore as Sole Migration Path | Export generates ANSI-compatible SQL INSERT dump; native `FileChooser` save/open dialogs; restore replaces data inside one transaction | PASS |
| VI. Native OS Dialogs & Native Print Only | JavaFX `FileChooser` for export/restore, Java `PrinterJob` for printing; no PDF libs | PASS |
| VII. Zero-Friction User Experience (v3.0.0) | Launches directly as a native desktop window, no server/browser/URL; single static-credential login/logout with non-blocking 5-day backup reminder; clear success/failure feedback everywhere | PASS |

Post-design re-check: The added full-screen/maximized landing page, modal
owned windows, colored buttons, field labels, and print-notes checkbox remain
inside the JavaFX view/controller layer and do not introduce networking,
multi-user behavior, non-native dialogs, PDF generation, or schema changes.

No violations identified. Complexity Tracking table is omitted (not needed).

## Project Structure

### Documentation (this feature)

```text
specs/001-patient-management/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md         # Phase 1 output (/speckit-plan command)
├── quickstart.md         # Phase 1 output (/speckit-plan command)
├── contracts/            # Phase 1 output (/speckit-plan command)
│   └── service-contracts.md
└── tasks.md              # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
app/
├── pom.xml                          # Pins H2 version explicitly (commented); jpackage config
├── src/main/java/com/clinic/pms/
│   ├── PatientManagementApp.java    # javafx.application.Application entry point
│   ├── config/                      # PersistenceConfig (EntityManagerFactory), AppPaths (data dir)
│   ├── view/                        # FXML controllers: LoginController, SetupController,
│   │                                 # PatientListController, PatientProfileController,
│   │                                 # MedicalHistoryEditController,
│   │                                 # DeletedPatientsController, BackupRestoreController,
│   │                                 # PrintPreviewController, ModalWindowHelper
│   ├── service/                      # Business logic per domain (PatientService, AuthService,
│   │                                 # BackupService, H2NativeBackupService, ClinicProfileService, ...)
│   ├── repository/                   # JPA repositories (plain DAO classes using EntityManager)
│   ├── entity/                       # JPA entities (Patient, MedicalHistoryEntry, DoctorCredential, ...)
│   └── exception/                    # Domain exceptions (NotFoundException, BadRequestException)
├── src/main/resources/
│   ├── db/migration/                 # Flyway V1__..., V2__... scripts (schema only)
│   ├── fxml/                         # Login.fxml, Setup.fxml, PatientList.fxml, PatientProfile.fxml,
│   │                                 # DeletedPatients.fxml, BackupRestore.fxml, PrintPreview.fxml
│   └── css/                          # application.css, print.css (screen vs. print styling)
└── src/test/java/com/clinic/pms/
    ├── service/
    └── repository/
```

**Structure Decision**: Single-project JavaFX desktop application (`app/`)
built with Maven; FXML + controllers replace the former React frontend,
plain JPA/Hibernate repositories (no Spring Data) replace the former REST
API layer. View/Controller → Service → Repository stays layered per
Principle II, just entirely in-process with no network boundary. Packaged
directly into a `jpackage` Windows `.exe` — no separate frontend build step,
no static-resource copying.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations — table intentionally omitted.

