# Data Model: Patient Management System (Solo Doctor, Single Machine)

All entities are persisted via JPA/Hibernate and versioned through Flyway
migrations (constitution Principle IV). Field names below are logical;
exact column naming/typing is finalized in the Flyway migration scripts
during implementation.

## DoctorCredential

Single static login credential gating access to the app (FR-019).

| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | Always exactly one row |
| username | String, required, unique | Set during first-run setup |
| passwordHash | String, required | BCrypt hash, never plaintext |
| recoveryAnswerHash | String, required after V2 migration | BCrypt hash used only for password reset, never plaintext |
| createdAt | Timestamp | First-run setup time |
| updatedAt | Timestamp | Last credential change |

**Validation**: exactly one row must exist before login is possible
(enforced by first-run setup flow, not by the login screen itself). The V2
Flyway migration initializes the recovery-answer hash for existing V1 rows;
first-run setup initializes it for new rows.

## DoctorClinicProfile

Doctor/clinic static identifying info, always printed (FR-015).

| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | Singleton row |
| doctorName | String, required | |
| clinicName | String, required | |
| contactDetails | String, required | Phone/address/etc. |
| registrationNumber | String, required | Medical registration number |

## Patient

Core record; parent of medical history entries (FR-001, FR-002).

| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | |
| fullName | String, required | Searchable (partial match) |
| dateOfBirth | Date, required | Must be more than one year before the current date |
| gender | Enum/String, required | New or changed values are `Male`, `Female`, or `Undisclosed`; legacy values may be preserved unchanged during edit |
| address | String, optional | Entered through a wrapping multiline field with four visible rows |
| emergencyContact | String, optional | |
| phoneNumber | String, required | Exactly 10 digits; searchable (exact/partial match) |
| email | String, optional | Blank allowed; non-blank values must use valid email format |
| isDeleted | Boolean, required, default false | Soft-delete flag; hides the patient (and its history/prescriptions/note) from all normal queries, but the row is never physically removed |
| deletedAt | Timestamp, nullable | Set when isDeleted becomes true |
| createdAt | Timestamp | |
| updatedAt | Timestamp | |

**Relationships**: one-to-many → MedicalHistoryEntry.
**Validation**: Patient-service create/update operations enforce the phone,
email, date-of-birth, and gender rules before persistence. The Add Patient
form renders each returned field error inline in red and does not close or
clear entered values on validation failure.
**Deletion**: soft-deleting a Patient sets `isDeleted=true`/`deletedAt`; it
hides the patient and its medical history entries from normal reads (search,
lists, printing), but nothing is physically deleted.
Backups always include soft-deleted patients and their data (FR-020). A
soft-deleted patient can be restored (`isDeleted=false`, `deletedAt=null`),
which immediately makes it visible again everywhere (FR-020a).

## MedicalHistoryEntry

Dated clinical visit record, including prescription and notes for that visit
(FR-004, FR-005, FR-006a, FR-006b).

| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | |
| patientId | Long (FK → Patient) | Required |
| entryDate | Date, required | Used for chronological ordering and date-range search |
| diagnosis | Text, required | Multiline diagnosis/complaint; UI presents about 4 lines of visible height |
| prescription | Text, optional | Multiline medication/treatment/instructions text for this visit |
| notes | Text, optional | Multiline clinical notes for this visit |
| isDeleted | Boolean, required, default false | Soft-delete flag; hides the entry from normal history lists and printing |
| deletedAt | Timestamp, nullable | Set when isDeleted becomes true |
| createdAt | Timestamp | |
| updatedAt | Timestamp | Last edit time |

**Ordering**: non-deleted entries are retrieved in descending `entryDate`
order per patient by default (most recent visit first), per FR-004.
**Editing**: double-clicking a history row opens an edit popup populated with
this entry's date, diagnosis, prescription, and notes. Saving updates the
same row; soft-delete sets `isDeleted=true`/`deletedAt` and keeps the row in
the database and full backups.
**Current prescription rule**: the current prescription for printing is the
`prescription` value from the most recent non-deleted MedicalHistoryEntry
for the patient that has non-blank prescription text.

## BackupMetadata

Tracks backup recency for the logout/close reminders (FR-009a, FR-019a).

| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | Singleton row |
| lastBackupAt | Timestamp, nullable | Null means "no backup ever taken" |

**Update rule**: set to the current timestamp only after a backup export
completes successfully. Read (never written) by the logout/close-warning
checks.

## Entity Relationship Summary

```text
DoctorCredential (singleton)         DoctorClinicProfile (singleton)
BackupMetadata (singleton)

Patient (1) ──< (many) MedicalHistoryEntry
```

## Backup Formats

The application supports two intentionally separate backup formats:

- **Portable ANSI backup**: generated by `BackupService` as INSERT statements
	for all application tables, including soft-deleted rows. It is the default
	migration format and uses transactional restore validation.
- **H2 native backup**: generated by `H2NativeBackupService` with H2
	`SCRIPT NOSETTINGS`, a `PMS_H2_BACKUP_V1` marker, and H2-specific schema/data
	statements. Restore creates an ANSI safety backup, executes `RUNSCRIPT` only
	in a fresh temporary database, validates it, and swaps it into place only
	after success.
