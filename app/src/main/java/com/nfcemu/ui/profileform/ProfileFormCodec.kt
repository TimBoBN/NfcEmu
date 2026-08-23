package com.nfcemu.ui.profileform

import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.ndefengine.WifiAuthType

/** Field-level validation errors, keyed by a field identifier the form UI understands (e.g. "ssid", "number"). */
data class FormValidationResult(val errors: Map<String, String>) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * Validates [ProfileFormFields] and converts a valid form to the [NdefPayload] the
 * engine encodes, plus the reverse mapping used to re-open an existing profile for
 * editing. Pure Kotlin/JVM logic (no Android imports) so it's testable as plain JUnit.
 */
object ProfileFormCodec {

    fun validate(fields: ProfileFormFields): FormValidationResult = when (fields) {
        is ProfileFormFields.Website -> validateWebsite(fields)
        is ProfileFormFields.Phone -> validatePhone(fields)
        is ProfileFormFields.Email -> validateEmail(fields)
        is ProfileFormFields.Sms -> validateSms(fields)
        is ProfileFormFields.Location -> validateLocation(fields)
        is ProfileFormFields.PlayStore -> validatePlayStore(fields)
        is ProfileFormFields.Wifi -> validateWifi(fields)
        is ProfileFormFields.Bluetooth -> validateBluetooth(fields)
        is ProfileFormFields.VCard -> validateVCard(fields)
        is ProfileFormFields.Text -> validateText(fields)
        is ProfileFormFields.CustomUri -> validateCustomUri(fields)
    }

    /** @throws IllegalStateException if [fields] is not valid - always call [validate] first. */
    fun toPayload(fields: ProfileFormFields): NdefPayload {
        check(validate(fields).isValid) { "toPayload() called on invalid form state" }
        return when (fields) {
            is ProfileFormFields.Website -> NdefPayload.Uri(withScheme(fields.url))
            is ProfileFormFields.Phone -> NdefPayload.Uri("tel:" + fields.number.filterNot { it.isWhitespace() })
            is ProfileFormFields.Email -> NdefPayload.Uri(buildMailto(fields))
            is ProfileFormFields.Sms -> NdefPayload.Uri(buildSms(fields))
            is ProfileFormFields.Location -> NdefPayload.Uri("geo:${fields.latitude.trim()},${fields.longitude.trim()}")
            is ProfileFormFields.PlayStore -> NdefPayload.Uri("market://details?id=${fields.appId.trim()}")
            is ProfileFormFields.Wifi -> NdefPayload.WifiHandover(
                ssid = fields.ssid.trim(),
                authType = fields.authType,
                password = fields.password.takeIf { it.isNotBlank() },
            )
            is ProfileFormFields.Bluetooth -> NdefPayload.BluetoothHandover(
                deviceAddress = fields.deviceAddress.trim().uppercase(),
                deviceName = fields.deviceName.trim().takeIf { it.isNotBlank() },
            )
            is ProfileFormFields.VCard -> NdefPayload.VCard(
                name = fields.name.trim().takeIf { it.isNotBlank() },
                phones = fields.phones.map { it.trim() }.filter { it.isNotBlank() },
                emails = fields.emails.map { it.trim() }.filter { it.isNotBlank() },
                organization = fields.organization.trim().takeIf { it.isNotBlank() },
                title = fields.title.trim().takeIf { it.isNotBlank() },
                website = fields.website.trim().takeIf { it.isNotBlank() },
                address = fields.address.trim().takeIf { it.isNotBlank() },
            )
            is ProfileFormFields.Text -> NdefPayload.Text(fields.text, fields.languageCode.ifBlank { "de" })
            is ProfileFormFields.CustomUri -> NdefPayload.Uri(fields.uri.trim())
        }
    }

    /** Reconstructs editable form fields from a persisted payload (used when opening "Bearbeiten"). */
    fun toFormFields(payload: NdefPayload): ProfileFormFields = when (payload) {
        is NdefPayload.VCard -> ProfileFormFields.VCard(
            name = payload.name.orEmpty(),
            phones = payload.phones.ifEmpty { listOf("") },
            emails = payload.emails.ifEmpty { listOf("") },
            organization = payload.organization.orEmpty(),
            title = payload.title.orEmpty(),
            website = payload.website.orEmpty(),
            address = payload.address.orEmpty(),
        )
        is NdefPayload.Text -> ProfileFormFields.Text(payload.text, payload.languageCode)
        is NdefPayload.WifiHandover -> ProfileFormFields.Wifi(payload.ssid, payload.authType, payload.password.orEmpty())
        is NdefPayload.BluetoothHandover -> ProfileFormFields.Bluetooth(payload.deviceAddress, payload.deviceName.orEmpty())
        is NdefPayload.Uri -> inferUriTemplate(payload.uri)
    }

    private fun inferUriTemplate(uri: String): ProfileFormFields = when {
        uri.startsWith("tel:") -> ProfileFormFields.Phone(uri.removePrefix("tel:"))
        uri.startsWith("mailto:") -> parseMailto(uri)
        uri.startsWith("sms:") -> parseSms(uri)
        uri.startsWith("geo:") -> parseGeo(uri)
        uri.startsWith("market://details?id=") -> ProfileFormFields.PlayStore(uri.removePrefix("market://details?id="))
        uri.startsWith("http://") || uri.startsWith("https://") -> ProfileFormFields.Website(uri)
        else -> ProfileFormFields.CustomUri(uri)
    }

    private fun withScheme(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.contains("://")) trimmed else "https://$trimmed"
    }

    private fun buildMailto(fields: ProfileFormFields.Email): String {
        val params = buildList {
            if (fields.subject.isNotBlank()) add("subject=" + percentEncode(fields.subject))
            if (fields.body.isNotBlank()) add("body=" + percentEncode(fields.body))
        }
        val query = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        return "mailto:${fields.address.trim()}$query"
    }

    private fun buildSms(fields: ProfileFormFields.Sms): String {
        val query = if (fields.body.isNotBlank()) "?body=" + percentEncode(fields.body) else ""
        return "sms:${fields.number.filterNot { it.isWhitespace() }}$query"
    }

    private fun parseMailto(uri: String): ProfileFormFields.Email {
        val withoutScheme = uri.removePrefix("mailto:")
        val (address, query) = splitQuery(withoutScheme)
        val params = parseQueryParams(query)
        return ProfileFormFields.Email(address, params["subject"].orEmpty(), params["body"].orEmpty())
    }

    private fun parseSms(uri: String): ProfileFormFields.Sms {
        val withoutScheme = uri.removePrefix("sms:")
        val (number, query) = splitQuery(withoutScheme)
        val params = parseQueryParams(query)
        return ProfileFormFields.Sms(number, params["body"].orEmpty())
    }

    private fun parseGeo(uri: String): ProfileFormFields {
        val coords = uri.removePrefix("geo:").substringBefore('?').split(",")
        return if (coords.size >= 2) {
            ProfileFormFields.Location(coords[0].trim(), coords[1].trim())
        } else {
            ProfileFormFields.CustomUri(uri)
        }
    }

    private fun splitQuery(value: String): Pair<String, String?> {
        val idx = value.indexOf('?')
        return if (idx == -1) value to null else value.substring(0, idx) to value.substring(idx + 1)
    }

    private fun parseQueryParams(query: String?): Map<String, String> {
        if (query.isNullOrEmpty()) return emptyMap()
        return query.split("&").mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq == -1) null else percentDecode(part.substring(0, eq)) to percentDecode(part.substring(eq + 1))
        }.toMap()
    }

    // --- validation ---

    private fun validateWebsite(fields: ProfileFormFields.Website): FormValidationResult =
        errorsOf("url" to "Please enter a website address".takeIf { fields.url.isBlank() })

    private fun validatePhone(fields: ProfileFormFields.Phone): FormValidationResult {
        val digits = fields.number.filterNot { it.isWhitespace() }
        val valid = digits.isNotEmpty() && digits.all { it.isDigit() || it == '+' || it == '-' || it == '(' || it == ')' }
        return errorsOf("number" to "Please enter a valid phone number".takeIf { !valid })
    }

    private fun validateEmail(fields: ProfileFormFields.Email): FormValidationResult {
        val address = fields.address.trim()
        val valid = address.contains("@") && address.substringAfter("@").contains(".") && !address.startsWith("@")
        return errorsOf("address" to "Please enter a valid email address".takeIf { !valid })
    }

    private fun validateSms(fields: ProfileFormFields.Sms): FormValidationResult {
        val digits = fields.number.filterNot { it.isWhitespace() }
        val valid = digits.isNotEmpty() && digits.all { it.isDigit() || it == '+' }
        return errorsOf("number" to "Please enter a valid phone number".takeIf { !valid })
    }

    private fun validateLocation(fields: ProfileFormFields.Location): FormValidationResult {
        val lat = fields.latitude.trim().toDoubleOrNull()
        val lng = fields.longitude.trim().toDoubleOrNull()
        val errors = mutableMapOf<String, String>()
        if (lat == null || lat < -90.0 || lat > 90.0) errors["latitude"] = "Latitude must be between -90 and 90"
        if (lng == null || lng < -180.0 || lng > 180.0) errors["longitude"] = "Longitude must be between -180 and 180"
        return FormValidationResult(errors)
    }

    private fun validatePlayStore(fields: ProfileFormFields.PlayStore): FormValidationResult {
        val id = fields.appId.trim()
        val valid = id.isNotEmpty() && id.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$"))
        return errorsOf("appId" to "Please enter a valid app ID (e.g. com.example.app)".takeIf { !valid })
    }

    private fun validateWifi(fields: ProfileFormFields.Wifi): FormValidationResult {
        val errors = mutableMapOf<String, String>()
        if (fields.ssid.isBlank()) {
            errors["ssid"] = "Please enter a network name (SSID)"
        }
        if (fields.authType != WifiAuthType.OPEN) {
            if (fields.password.isBlank()) {
                errors["password"] = "Please enter a password"
            } else if (fields.authType != WifiAuthType.WEP && fields.password.length < 8) {
                errors["password"] = "The password must be at least 8 characters long"
            }
        }
        return FormValidationResult(errors)
    }

    private fun validateBluetooth(fields: ProfileFormFields.Bluetooth): FormValidationResult {
        val valid = fields.deviceAddress.trim().matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$"))
        return errorsOf("deviceAddress" to "Please enter a valid Bluetooth address (e.g. AA:BB:CC:DD:EE:FF)".takeIf { !valid })
    }

    private fun validateVCard(fields: ProfileFormFields.VCard): FormValidationResult {
        val hasAnyContent = fields.name.isNotBlank() ||
            fields.phones.any { it.isNotBlank() } ||
            fields.emails.any { it.isNotBlank() } ||
            fields.organization.isNotBlank() ||
            fields.title.isNotBlank() ||
            fields.website.isNotBlank() ||
            fields.address.isNotBlank()
        return errorsOf("general" to "Please fill in at least one field".takeIf { !hasAnyContent })
    }

    private fun validateText(fields: ProfileFormFields.Text): FormValidationResult =
        errorsOf("text" to "Please enter some text".takeIf { fields.text.isBlank() })

    private fun validateCustomUri(fields: ProfileFormFields.CustomUri): FormValidationResult {
        val valid = fields.uri.isNotBlank() && fields.uri.contains(":")
        return errorsOf("uri" to "Please enter a complete URI with a scheme (e.g. myapp://...)".takeIf { !valid })
    }

    private fun errorsOf(vararg pairs: Pair<String, String?>): FormValidationResult =
        FormValidationResult(pairs.mapNotNull { (key, message) -> message?.let { key to it } }.toMap())
}

/** Minimal percent-encoding for URI query values (space -> %20, not "+"). */
internal fun percentEncode(value: String): String {
    val sb = StringBuilder()
    for (byte in value.toByteArray(Charsets.UTF_8)) {
        val c = byte.toInt() and 0xFF
        val isUnreserved = (c in 'A'.code..'Z'.code) || (c in 'a'.code..'z'.code) || (c in '0'.code..'9'.code) ||
            c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
        if (isUnreserved) {
            sb.append(c.toChar())
        } else {
            sb.append('%').append("%02X".format(c))
        }
    }
    return sb.toString()
}

internal fun percentDecode(value: String): String {
    val bytes = ArrayList<Byte>(value.length)
    var i = 0
    while (i < value.length) {
        val ch = value[i]
        if (ch == '%' && i + 2 < value.length) {
            val hex = value.substring(i + 1, i + 3)
            val byte = hex.toIntOrNull(16)
            if (byte != null) {
                bytes.add(byte.toByte())
                i += 3
                continue
            }
        }
        bytes.addAll(ch.toString().toByteArray(Charsets.UTF_8).toList())
        i += 1
    }
    return bytes.toByteArray().toString(Charsets.UTF_8)
}
