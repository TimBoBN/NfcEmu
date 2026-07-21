package com.nfcemu.data.export

/** All failure modes when reading a `.nfcemu` file, with a message safe to show to the user. */
sealed class NfcEmuFileException(message: String) : Exception(message) {

    class Corrupt(message: String) : NfcEmuFileException(
        "Datei ist beschädigt oder kein gültiges .nfcemu-Format: $message",
    )

    class UnsupportedVersion(fileVersion: Int, supportedVersion: Int) : NfcEmuFileException(
        "Diese Datei wurde mit einer neueren App-Version erstellt (Format $fileVersion, unterstützt bis $supportedVersion). Bitte NfcEmu aktualisieren.",
    )
}
