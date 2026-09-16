# Patient Management System

A single-machine, single-user patient management **JavaFX desktop
application** for a solo doctor. See
[specs/001-patient-management](specs/001-patient-management) for the full
spec, plan, data model, service contracts, and task breakdown.

## Prerequisites

- JDK 21
- Maven

## Build & Run (local development)

```bash
cd app
mvn javafx:run
```

A native window opens directly — no browser, no URL, no server process. On
first run, you'll be guided through a one-time setup screen to choose your
login username/password and enter your clinic details — no default
credentials are shipped with the app.

After login, the patient landing page opens maximized and defaults the date
range to the last 7 days. Opening a patient keeps the landing page behind it
and shows the profile as a modal child window; print preview opens as another
modal window above the profile.

If the password is forgotten, use **Forgot Password?** on the login screen and
answer the recovery question. The recovery answer is stored as a one-way
BCrypt hash, not as readable text; resetting the password does not alter
patient or clinic data.

## Build a runnable JAR

```bash
cd app
mvn clean package
java -jar target/patient-management-system.jar
```

## Packaging a Windows installer

```bash
cd app
mvn clean package
mvn -Ppackage-windows exec:exec
```

This produces a Windows `.exe` installer (via `jpackage`) with a bundled JVM
**and** JavaFX runtime, so the doctor's machine needs no separate Java or
JavaFX installation. `jpackage` cannot cross-compile installers, so this must
be run on a Windows machine (or the `windows` job in
[`.github/workflows/build-distributions.yml`](.github/workflows/build-distributions.yml),
which also installs the WiX Toolset that `jpackage --type exe` requires).
The installer is unsigned, so first launch will show a Windows SmartScreen
warning ("Windows protected your PC") until the doctor clicks **More info →
Run anyway**, or the `.exe` is signed with a code-signing certificate.

## Packaging a macOS installer

```bash
cd app
mvn -Ppackage-mac package
```

On macOS this produces a `.dmg` installer (written to `app/`, e.g.
`Patient Management System-1.0.dmg`) with a bundled JVM **and** JavaFX
runtime — no separate Java install is needed — plus the app icon in
Finder, the Dock, and the installed application launcher. The dmg is
unsigned/unnotarized, so first launch requires right-click → **Open** (or
System Settings → Privacy & Security → **Open Anyway**) once, until the app
is signed and notarized with an Apple Developer ID.

## Building both distributions

Both installers can be built automatically via
[`.github/workflows/build-distributions.yml`](.github/workflows/build-distributions.yml)
(triggered manually or on a `v*` tag push), since a genuine Windows `.exe`
and macOS `.dmg` each require building on their own OS. Each job runs the
same Maven commands as above on a matching GitHub-hosted runner and uploads
the installer as a build artifact.

## Backup & machine migration

Use **Backup & Restore** in the app to export a full, plain-SQL backup
(`.sql` file) to a location of your choice via the native save dialog (e.g.,
a USB drive). To move to a new laptop: install the app fresh on the new
machine, then use **Restore** and select that same backup file via the
native open dialog. This is the only supported migration path — there is no
live sync between machines.

The Backup & Restore screen also provides an **H2 Native Backup** option
(`.h2.sql`). It includes H2-specific schema and data details and can be faster
for restoring this application, but it is tied to the bundled H2 version and
is not portable to another database system. H2 restore creates an ANSI SQL
safety backup first, validates a fresh database, and replaces the live database
only after validation succeeds.

## Printing

Print preview always includes patient and clinic information. Optional sections
include Patient History, Current Prescription, and Include History Notes; notes
are printed inline with history only when that checkbox is selected.

## Data location

The H2 database file lives under
`~/.patient-management-system/data/patientdb` (file-based, never in-memory).

Application logs are written under
`~/.patient-management-system/log/patient-management-system.log`. Logs also
continue to appear in the console when the application is started from a
terminal. Log files rotate at 10 MB, are retained for 30 days, and are capped
at 200 MB in total. On Windows, `~` means the user's home directory, typically
`C:\\Users\\<username>`.

## Validation

```bash
cd app
mvn test
```

The test suite covers core service flows including setup/login, patient
validation, history entries with embedded prescription/notes, backup/restore,
and H2-native backup validation. Run the quickstart scenarios in
[specs/001-patient-management/quickstart.md](specs/001-patient-management/quickstart.md)
before relying on a packaged build for real patient data.
