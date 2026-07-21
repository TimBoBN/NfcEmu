# NfcEmu

An NFC multi-tool for Android that emulates an NFC Forum Type 4 Tag via Host Card
Emulation (HCE), serving various NDEF contents (website, phone number, email, SMS,
location, Play Store app, Wi-Fi credentials, business card, plain text, arbitrary
URI). Fully offline, no analytics, no network permission.

## Features

- **Profile types**: website, phone number, email, SMS, geo location, Play Store
  app, Wi-Fi credentials, vCard business card, plain text, or any custom URI -
  each with its own input form and live validation.
- **Multiple profiles**, one active at a time. Pin profiles for quick access, set
  or clear the active one from the Home screen, the profile list, the home screen
  widget, or the Quick Settings tile.
- **Android Application Record (AAR)**: optionally bind a profile to a specific
  installed app, picked from a searchable list (or typed manually if the target
  app isn't installed on this device yet).
- **Home screen widget**: shows the active profile and a tappable list of
  pinned/recently-used profiles - switch without opening the app.
- **Quick Settings tile**: cycles through pinned profiles on tap (Android allows
  one static tile per app, not one per profile, so cycling is the closest fit).
- **Save & reuse**: export any profile as a `.nfcemu` file (structured + re-editable)
  via the Storage Access Framework, or as a raw `.ndef` binary dump for third-party
  NFC-writing tools. Import `.nfcemu` files back in, browse them in the in-app
  library, re-activate or share them from there.
- **NFC read suppression while open**: since NfcEmu emulates a card, it actively
  ignores the phone's own tag-discovery dispatch while in the foreground, so
  holding two HCE-capable phones together doesn't trigger Android's default
  "empty tag" handling on either side.
- Onboarding on first launch, dark/light theme (Material You dynamic color on
  Android 12+), edge-to-edge, animated screen transitions, accessible (content
  descriptions, TalkBack-friendly).

## Architecture

```
:ndefengine        pure Kotlin/JVM module, no Android dependency
                    - NdefPayload (sealed interface) + one encoder per type
                    - NdefMessageEncoder/-Parser (binary NDEF format)
                    - CapabilityContainer (Type 4 Tag CC file)
                    - Type4TagApduProcessor (SELECT/READ BINARY, chunking, status words)

:app                Android app (Kotlin, Jetpack Compose, Material 3, Hilt)
  hce/              thin HostApduService wrapper around Type4TagApduProcessor
  nfc/              NfcStateSource (NFC on/off) - consumed by Home's status banner
  domain/           ActiveNdefSource interface (consumed by the HCE service)
  data/             Profile model, ProfileRepository (DataStore), .nfcemu export/
                    import (data/export), library index for saved files (data/library)
  widget/           home screen widget: AppWidgetProvider, RemoteViewsService,
                    click receiver, and the updater that keeps it in sync
  tile/             Quick Settings TileService
  util/             InstalledAppsSource (AAR app picker)
  ui/               Compose screens + ViewModels: home, profilelist, profileform,
                    library, onboarding, navigation, theme, components
  di/               Hilt modules (DataStore instances, CoroutineScope, bindings)
```

MVVM: Compose (UI) → ViewModel → Repository (data layer) → NdefEngine (domain
encoding). Everything Flow-based, no blocking I/O in the service hot path: the HCE
service only ever reads pre-encoded NDEF bytes from an in-memory cache that's only
recomputed on a profile switch.

### Design decisions worth knowing about

- **`NdefPayload` is both the engine and the persistence model.** It's
  `@Serializable` (in `:ndefengine`) and reused directly as `Profile.fields`. That
  avoids a separate mapper layer between "how it gets encoded" and "how it gets
  persisted" - a profile is structurally identical to what ends up as NDEF bytes.
- **Eager singletons for entry points that aren't the UI.** `ProfileRepository` and
  `ProfileWidgetUpdater` are field-injected into `NfcEmuApplication` so Hilt
  constructs them - and starts their DataStore-backed Flows - as soon as the
  process exists. This matters because the HCE service, the widget, or the QS tile
  can all be the very first thing the system starts in this process, with the
  Activity never having run.
- **Interfaces at every Android-framework seam** (`ActiveNdefSource`,
  `NfcStateSource`, `InstalledAppsSource`) so ViewModels are unit-testable against
  fakes without Robolectric or an emulator - see the `test` source sets.
- **Read-modify-write mutators always go through the DataStore Flow directly**
  (`dataStore.profiles.first()`), never through the UI-facing cached StateFlow, to
  avoid a race where two calls back-to-back (e.g. create-then-activate) read a
  stale pre-write snapshot.

## Adding a new NdefPayload type

1. In `ndefengine/.../NdefPayload.kt`: add a new `@Serializable @SerialName("...")`
   variant to the sealed interface.
2. Write an `XyzRecordEncoder` object that produces a `RawNdefRecord` (or a list of
   them).
3. Wire the new branch into `NdefMessageFactory.encodePayloadRecords` (the compiler
   enforces this since the `when` is exhaustive).
4. Unit-test the encoder + a round-trip test via `NdefParser` in
   `NdefMessageFactoryTest`.
5. For the UI: add a matching `ProfileTypeTemplate` + form field variant in
   `app/.../ui/profileform/ProfileFormFields.kt`, plus validation/mapping in
   `ProfileFormCodec.kt` (validate/toPayload/toFormFields) and an icon/label in
   `ui/components/ProfileTypeIcon.kt`.

See `ndefengine/README.md` for details on the engine itself.

## The `.nfcemu` file format

JSON, versioned, contains both the structured (re-editable) profile data and the
raw NDEF bytes (Base64) for interop with third-party tools:

```jsonc
{
  "formatVersion": 1,
  "exportedAt": 1732000000000,        // Unix millis
  "profile": {
    "name": "My Business Card",
    "fields": {                        // NdefPayload, polymorphically serialized
      "type": "vcard",                 // discriminator: uri | vcard | text | wifi
      "name": "Ada Lovelace",
      "phones": ["+491701234567"],
      "emails": [],
      "organization": null,
      "title": null,
      "website": null,
      "address": null
    },
    "aarPackageName": null             // optional, binds to a specific app
  },
  "ndefBase64": "AwoAA1UDaHR0cH..."     // fully encoded NDEF message, Base64
}
```

- **`formatVersion`**: bumped whenever the schema changes incompatibly.
  `NfcEmuFileCodec` rejects files with a version higher than its own
  `CURRENT_FORMAT_VERSION` with a clear error (`NfcEmuFileException.UnsupportedVersion`)
  instead of misinterpreting them.
- **Unknown extra fields** are ignored on read (`ignoreUnknownKeys`), so future,
  backward-compatible extensions don't break older app versions.
- **Defensive deserialization**: any kind of broken/tampered input (invalid JSON,
  missing required fields, invalid Base64, unknown `type` discriminator) results in
  `NfcEmuFileException.Corrupt` with an understandable message - never a crash.
- A plain NDEF binary dump export (`.ndef`/`.bin`, no JSON wrapper) is a separate
  export option for third-party tools like NFC-writing apps for physical tags.

## Permissions & privacy

- **`android.permission.NFC`** only. No storage permission (file export/import is
  entirely Storage Access Framework based - the user picks the file location every
  time), no network permission, no analytics/tracking of any kind.
- The home screen widget and Quick Settings tile only ever read/write the local
  profile DataStore; nothing they do requires additional permissions beyond NFC.

## Known limitations

- The Wi-Fi handover encoder (Connection Handover + WSC carrier) is deliberately
  isolated as a stretch goal; the structure follows the spec closely but hasn't
  been verified against real hardware.
- The home screen widget shows a single generic icon per row instead of a
  type-specific one (website/vCard/Wi-Fi/...): `RemoteViews` can only reference
  drawable resources, not the Compose `ImageVector`s the rest of the app uses, so
  matching the in-app icon set exactly would mean maintaining a parallel set of
  vector drawables purely for the widget.
- This development environment had no access to a physical Android device or an
  NFC-capable emulator - the debug and minified release builds were verified
  locally with an installed Android SDK (Platform 34), all unit/round-trip tests
  were run, and a real release APK downloaded from a GitHub Release was verified
  byte-for-byte (zip integrity, signature, manifest) - but actually tapping a
  reader, and the widget/tile/NFC-read-suppression behavior specifically, could
  only be confirmed by the user on real hardware, not by this assistant.
- Without configured signing secrets, the release build falls back to the debug
  keystore (installable for testing, but not a real upload key) - see
  "Setting up release signing" below.

## CI/CD

Two GitHub Actions workflows under `.github/workflows/`:

- **`ci.yml`**: runs on every push/PR to `main`. Runs all unit tests
  (`:ndefengine:test`, `:app:testDebugUnitTest`), builds the debug and minified
  release APK (to catch R8/ProGuard regressions early), and uploads both as
  workflow artifacts.
- **`release.yml`**: runs on a tag push (`v*.*.*`, e.g. `v1.0.0`) or manually via
  "Run workflow" with an existing tag. Runs the tests, builds the signed release
  APK, and attaches it to a newly created GitHub Release as two assets:
  `NfcEmu-<tag>.apk` (that exact version) and `NfcEmu-latest.apk` (identical file,
  fixed name). `versionCode`/`versionName` are set from the CI run number and the
  tag name respectively (see `app/build.gradle.kts`).

Trigger a release:

```
git tag v1.0.0
git push origin v1.0.0
```

### Stable "latest" download link

Because `NfcEmu-latest.apk` is attached under that exact same name on every
release, and each new release is marked as GitHub's "latest" release
(`make_latest: true`), this URL always resolves to the newest build without
ever changing:

```
https://github.com/TimBoBN/NFCEmu/releases/latest/download/NfcEmu-latest.apk
```

Useful for bookmarking, a QR code, or linking from elsewhere without having to
update the URL on every new version.

### Setting up release signing

Without the following repository secrets, `release.yml` still builds successfully
but only signs with the debug keystore (a warning appears in the workflow log).
For a real upload key:

1. Generate a keystore (if you don't have one yet):
   ```
   keytool -genkey -v -keystore release.keystore.jks -keyalg RSA -keysize 2048 \
     -validity 10000 -alias nfcemu
   ```
2. Base64-encode it: `base64 -w0 release.keystore.jks`
3. In the repository settings under *Settings → Secrets and variables → Actions*,
   create these secrets:
   - `KEYSTORE_BASE64` - output from step 2
   - `KEYSTORE_PASSWORD` - keystore password
   - `KEY_ALIAS` - e.g. `nfcemu`
   - `KEY_PASSWORD` - the alias's password (often the same as the keystore password)

Never commit the keystore itself to the repository.

## Requirements

- minSdk 24 (Android 7.0), targetSdk 34.
- A device or emulator with `android.hardware.nfc.hce` support to actually emulate
  a card (the app still builds and installs without it, but the HCE service won't
  be selectable as a reader target).
- JDK 17 and an Android SDK with Platform 34 / Build-Tools 34.0.0 for building
  locally; `local.properties` must point to it (`sdk.dir=...`).

## Running tests

```
JAVA_HOME=<jdk17> ./gradlew :ndefengine:test :app:testDebugUnitTest
```

## Building

```
JAVA_HOME=<jdk17> ./gradlew :app:assembleDebug
JAVA_HOME=<jdk17> ./gradlew :app:assembleRelease   # minified, R8
```
