package com.nfcemu.widget

import com.nfcemu.R
import com.nfcemu.ndefengine.NdefPayload
import com.nfcemu.ndefengine.WifiAuthType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [NdefPayload.widgetIconRes] mirrors `NdefPayload.icon()`'s bucket logic (including
 * URI-scheme sniffing) against a parallel drawable resource set, since
 * [android.widget.RemoteViews] can't reference the Compose icons directly.
 */
class ProfileWidgetRemoteViewsServiceTest {

    @Test
    fun `vcard, text and wifi payloads map to their dedicated icons`() {
        assertEquals(R.drawable.ic_widget_vcard, NdefPayload.VCard(name = "Jane").widgetIconRes())
        assertEquals(R.drawable.ic_widget_text, NdefPayload.Text("hello").widgetIconRes())
        assertEquals(
            R.drawable.ic_widget_wifi,
            NdefPayload.WifiHandover(ssid = "net", authType = WifiAuthType.WPA2_PSK).widgetIconRes(),
        )
    }

    @Test
    fun `uri payloads map by scheme, falling back to the generic link icon`() {
        assertEquals(R.drawable.ic_widget_phone, NdefPayload.Uri("tel:12345").widgetIconRes())
        assertEquals(R.drawable.ic_widget_email, NdefPayload.Uri("mailto:a@b.com").widgetIconRes())
        assertEquals(R.drawable.ic_widget_sms, NdefPayload.Uri("sms:12345").widgetIconRes())
        assertEquals(R.drawable.ic_widget_location, NdefPayload.Uri("geo:1,2").widgetIconRes())
        assertEquals(R.drawable.ic_widget_play_store, NdefPayload.Uri("market://details?id=com.example").widgetIconRes())
        assertEquals(R.drawable.ic_widget_website, NdefPayload.Uri("https://example.com").widgetIconRes())
        assertEquals(R.drawable.ic_widget_website, NdefPayload.Uri("http://example.com").widgetIconRes())
        assertEquals(R.drawable.ic_widget_link, NdefPayload.Uri("urn:custom:thing").widgetIconRes())
    }
}
