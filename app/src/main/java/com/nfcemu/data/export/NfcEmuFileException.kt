package com.nfcemu.data.export

/** All failure modes when reading a `.nfcemu` file, with a message safe to show to the user. */
sealed class NfcEmuFileException(message: String) : Exception(message) {

    class Corrupt(message: String) : NfcEmuFileException(
        "File is corrupted or not a valid .nfcemu format: $message",
    )

    class UnsupportedVersion(fileVersion: Int, supportedVersion: Int) : NfcEmuFileException(
        "This file was created with a newer app version (format $fileVersion, supported up to $supportedVersion). Please update NfcEmu.",
    )
}
