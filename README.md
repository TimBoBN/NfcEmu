# NfcEmu

An NFC multi-tool for Android that emulates an NFC Forum Type 4 Tag via Host Card
Emulation (HCE), serving various NDEF contents (website, phone number, email, SMS,
location, Play Store app, Wi-Fi credentials, business card, plain text, arbitrary
URI). Fully offline, no analytics, no network permission.

## Architecture

```
:ndefengine   pure Kotlin/JVM module, no Android dependency
              - NdefPayload (sealed interface) + one encoder per type
              - NdefMessageEncoder/-Parser (binary NDEF format)
              - CapabilityContainer (Type 4 Tag CC file)
              - Type4TagApduProcessor (SELECT/READ BINARY, chunking, status words)

:app          Android app (Kotlin, Jetpack Compose, Material 3, Hilt)
  hce/        thin HostApduService wrapper around Type4TagApduProcessor
  domain/     ActiveNdefSource interface (consumed by the service)
  data/       Profile model, ProfileRepository (DataStore), .nfcemu export/import,
              library index for saved files
  ui/         Compose screens + ViewModels (Home, profile list, forms, library)
  di/         Hilt modules (DataStore instances, CoroutineScope, bindings)
```

MVVM: Compose (UI) → ViewModel → Repository (data layer) → NdefEngine (domain
encoding). Everything Flow-based, no blocking I/O in the service hot path: the HCE
service only ever reads pre-encoded NDEF bytes from an in-memory cache that's only
recomputed on a profile switch.

### Why `NdefPayload` is both the engine and the persistence model

`NdefPayload` (in `:ndefengine`) is `@Serializable` and reused directly as the
`Profile.fields` type. That avoids a separate mapper layer between "how it gets
encoded" and "how it gets persisted" - a profile is structurally identical to
what ends up as NDEF bytes.

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

## Known limitations

- The Wi-Fi handover encoder (Connection Handover + WSC carrier) is deliberately
  isolated as a stretch goal; the structure follows the spec closely but hasn't
  been verified against real hardware.
- This environment had no access to a physical Android device or an emulator with
  NFC support - the debug and minified release build were successfully built
  locally with an installed Android SDK (Platform 34) and all unit/round-trip
  tests were run, but actually tapping a reader could not be manually verified.
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
  APK, renames it to `NfcEmu-<tag>.apk`, and attaches it as an asset to a newly
  created GitHub Release. `versionCode`/`versionName` are set from the CI run
  number and the tag name respectively (see `app/build.gradle.kts`).

Trigger a release:

```
git tag v1.0.0
git push origin v1.0.0
```

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

## Running tests

```
JAVA_HOME=<jdk17> ./gradlew :ndefengine:test :app:testDebugUnitTest
```

## Building

```
JAVA_HOME=<jdk17> ./gradlew :app:assembleDebug
JAVA_HOME=<jdk17> ./gradlew :app:assembleRelease   # minified, R8
```

`local.properties` must point to an Android SDK with Platform 34 / Build-Tools
34.0.0 (`sdk.dir=...`).
