package com.nfcemu.data

import com.nfcemu.ndefengine.NdefPayload

/** Short, stable label used for library-entry icons and lists; matches the JSON `type` discriminator. */
fun NdefPayload.typeLabel(): String = when (this) {
    is NdefPayload.Uri -> "uri"
    is NdefPayload.VCard -> "vcard"
    is NdefPayload.Text -> "text"
    is NdefPayload.WifiHandover -> "wifi"
    is NdefPayload.BluetoothHandover -> "bluetooth"
}
