# NfcEmu

Ein NFC-Multi-Tool für Android, das per Host Card Emulation (HCE) einen NFC Forum
Type 4 Tag emuliert und dabei verschiedene NDEF-Inhalte (Website, Telefonnummer,
E-Mail, SMS, Standort, Play-Store-App, WLAN-Zugang, Visitenkarte, Freitext, beliebige
URI) ausliefert. Komplett offline, keine Analytics, keine Netzwerk-Permission.

## Architektur

```
:ndefengine   reines Kotlin/JVM-Modul, keine Android-Abhängigkeit
              - NdefPayload (sealed interface) + ein Encoder pro Typ
              - NdefMessageEncoder/-Parser (binäres NDEF-Format)
              - CapabilityContainer (Type-4-Tag CC-Datei)
              - Type4TagApduProcessor (SELECT/READ BINARY, Chunking, Statuswörter)

:app          Android-App (Kotlin, Jetpack Compose, Material 3, Hilt)
  hce/        dünner HostApduService-Wrapper um Type4TagApduProcessor
  domain/     ActiveNdefSource-Schnittstelle (vom Service konsumiert)
  data/       Profile-Modell, ProfileRepository (DataStore), .nfcemu-Export/Import,
              Bibliotheks-Index für gespeicherte Dateien
  ui/         Compose-Screens + ViewModels (Home, Profil-Liste, Formulare, Bibliothek)
  di/         Hilt-Module (DataStore-Instanzen, CoroutineScope, Bindings)
```

MVVM: Compose (UI) → ViewModel → Repository (Datenschicht) → NdefEngine (Domain-Encoding).
Alles Flow-basiert, kein blockierendes I/O im Service-Hot-Path: der HCE-Service liest
ausschließlich vorab kodierte NDEF-Bytes aus einem In-Memory-Cache, der nur bei
Profilwechsel neu berechnet wird.

### Warum `NdefPayload` sowohl Engine- als auch Persistenz-Modell ist

`NdefPayload` (in `:ndefengine`) ist `@Serializable` und wird direkt als
`Profile.fields`-Typ wiederverwendet. Das erspart eine separate Mapper-Schicht
zwischen "wie wird encodiert" und "wie wird gespeichert" – ein Profil ist
strukturell identisch mit dem, was am Ende auf den NDEF-Bytes landet.

## Einen neuen NdefPayload-Typ hinzufügen

1. In `ndefengine/.../NdefPayload.kt`: neue `@Serializable @SerialName("...")`-Variante
   zum sealed interface hinzufügen.
2. Einen `XyzRecordEncoder`-Objekt schreiben, der `RawNdefRecord` (oder eine Liste
   davon) erzeugt.
3. Den neuen Zweig in `NdefMessageFactory.encodePayloadRecords` verdrahten (der
   Compiler erzwingt das, da der `when` exhaustiv ist).
4. Unit-Test für den Encoder + Round-Trip-Test über `NdefParser` in
   `NdefMessageFactoryTest`.
5. Für die UI: in `app/.../ui/profileform/ProfileFormFields.kt` eine passende
   `ProfileTypeTemplate` + Formular-Feld-Variante ergänzen, dazu Validierung/Mapping
   in `ProfileFormCodec.kt` (validate/toPayload/toFormFields) sowie Icon/Label in
   `ui/components/ProfileTypeIcon.kt`.

Siehe `ndefengine/README.md` für Details zur Engine selbst.

## Das `.nfcemu`-Dateiformat

JSON, versioniert, enthält sowohl die strukturierten (wieder editierbaren) Profildaten
als auch die rohen NDEF-Bytes (Base64) für Interop mit Drittprogrammen:

```jsonc
{
  "formatVersion": 1,
  "exportedAt": 1732000000000,        // Unix-Millis
  "profile": {
    "name": "Meine Visitenkarte",
    "fields": {                        // NdefPayload, polymorph serialisiert
      "type": "vcard",                 // Diskriminator: uri | vcard | text | wifi
      "name": "Ada Lovelace",
      "phones": ["+491701234567"],
      "emails": [],
      "organization": null,
      "title": null,
      "website": null,
      "address": null
    },
    "aarPackageName": null             // optional, bindet an eine bestimmte App
  },
  "ndefBase64": "AwoAA1UDaHR0cH..."     // fertig kodierte NDEF-Message, Base64
}
```

- **`formatVersion`**: wird erhöht, sobald sich das Schema inkompatibel ändert.
  `NfcEmuFileCodec` lehnt Dateien mit einer höheren Version als der eigenen
  `CURRENT_FORMAT_VERSION` mit einer klaren Fehlermeldung ab
  (`NfcEmuFileException.UnsupportedVersion`), statt sie falsch zu interpretieren.
- **Unbekannte zusätzliche Felder** werden beim Lesen ignoriert (`ignoreUnknownKeys`),
  damit künftige, abwärtskompatible Erweiterungen alte App-Versionen nicht brechen.
- **Defensive Deserialisierung**: jede Art von kaputtem/manipuliertem Input (invalides
  JSON, fehlende Pflichtfelder, ungültiges Base64, unbekannter `type`-Diskriminator)
  führt zu `NfcEmuFileException.Corrupt` mit verständlicher Meldung – nie zu einem
  Absturz.
- Reiner NDEF-Binärdump-Export (`.ndef`/`.bin`, ohne JSON-Hülle) ist eine separate
  Export-Option für Drittprogramme wie NFC-Tools zum Beschreiben physischer Tags.

## Bekannte Einschränkungen

- Der Wifi-Handover-Encoder (Connection Handover + WSC-Carrier) ist bewusst als
  Stretch-Goal isoliert; die Struktur ist spezifikationsnah, aber nicht an echter
  Hardware verifiziert.
- Es gab in dieser Umgebung keinen Zugriff auf ein physisches Android-Gerät oder
  einen Emulator mit NFC-Unterstützung – Debug- und minifizierter Release-Build
  wurden lokal mit einem installierten Android SDK (Platform 34) erfolgreich gebaut
  und alle Unit-/Round-Trip-Tests ausgeführt, aber das tatsächliche Antippen eines
  Lesegeräts konnte nicht manuell verifiziert werden.
- Kein Signing-Config für den Release-Build hinterlegt (App-Signing ist eine
  Nutzerentscheidung); `assembleRelease` erzeugt daher eine unsignierte APK.

## Tests ausführen

```
JAVA_HOME=<jdk17> ./gradlew :ndefengine:test :app:testDebugUnitTest
```

## Bauen

```
JAVA_HOME=<jdk17> ./gradlew :app:assembleDebug
JAVA_HOME=<jdk17> ./gradlew :app:assembleRelease   # minifiziert, R8
```

`local.properties` muss auf einen Android SDK mit Platform 34 / Build-Tools 34.0.0
zeigen (`sdk.dir=...`).
