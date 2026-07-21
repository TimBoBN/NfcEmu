# ndefengine

Pure-Kotlin/JVM module (no Android dependency) that turns a [`NdefPayload`](src/main/kotlin/com/nfcemu/ndefengine/NdefPayload.kt)
into the exact bytes served by the HCE `HostApduService`. Kept Android-free so its
encoders run as plain, fast JUnit tests without an emulator or Robolectric.

## Adding a new emulated type

1. Add a variant to the `NdefPayload` sealed interface.
2. Write a `XyzRecordEncoder` object that returns a `RawNdefRecord` (or `List<RawNdefRecord>`
   if your type needs more than one record, like `WifiHandoverRecordEncoder`).
3. Wire the new branch into `NdefMessageFactory.encodePayloadRecords` — the compiler
   will force you to handle it since the `when` is exhaustive over the sealed interface.
4. Unit-test the encoder in isolation, plus a round-trip test via `NdefParser` in
   `NdefMessageFactoryTest`.

## Files

- `RawNdefRecord.kt` / `NdefMessageEncoder.kt` — binary NDEF serialization (MB/ME/CF/SR/IL
  flags, short vs. long payload length).
- `UriRecordEncoder.kt`, `TextRecordEncoder.kt`, `VCardRecordEncoder.kt`,
  `AarRecordEncoder.kt`, `WifiHandoverRecordEncoder.kt` — one encoder per NDEF record type.
- `NdefMessageFactory.kt` — combines a payload + optional AAR into final message bytes.
- `CapabilityContainer.kt` — Type 4 Tag CC file (E103), sized dynamically from the actual
  NDEF message length.
- `NdefParser.kt` — minimal decoder used only by tests to verify round-trips.
- `hce/Type4TagApduProcessor.kt` / `hce/StatusWord.kt` — the Type 4 Tag command set
  itself (SELECT AID/CC/NDEF, READ BINARY with offset-based chunking), kept here
  rather than in `:app` so the whole ISO 7816-4 state machine is testable as plain
  JUnit too. `:app`'s `NfcEmuHostApduService` is just a thin Android wrapper around it.

## Wi-Fi handover caveat

`WifiHandoverRecordEncoder` is a stretch goal: it builds a Connection Handover "Hs" +
WSC carrier record structure that mirrors what NFC-writing tools commonly use for
"Wi-Fi config token" tags, but it has not been validated against real hardware. It's
isolated in its own file precisely so a gap here doesn't affect the other, spec-solid
record types (URI, Text, vCard, AAR).

## Running tests

```
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :ndefengine:test
```
