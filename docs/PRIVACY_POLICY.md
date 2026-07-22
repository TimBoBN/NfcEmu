# Privacy Policy for NfcEmu

_Last updated: 2026-07-22_

NfcEmu is an offline NFC utility. This policy is short because the app does
very little with your data:

## No data collection

NfcEmu does not collect, transmit, or share any personal data, usage
analytics, crash reports, or advertising identifiers. The app has no backend
server and requests no `INTERNET` permission - it cannot send anything over
the network even if it wanted to.

## What's stored, and where

All profiles you create (website links, phone numbers, Wi-Fi credentials,
contact cards, etc.) and any `.nfcemu` files you export or import are stored
**only on your device**, using Android's local app storage (Jetpack
DataStore) and, for exported/imported files, wherever you chose to save them
via the system file picker. Nothing leaves the device unless you explicitly
export or share a file yourself.

## Permissions

NfcEmu requests exactly one permission: `android.permission.NFC`, required
to emulate an NFC tag via Host Card Emulation and, when you choose to scan a
physical tag, to read it. No location, contacts, storage, or network
permissions are requested.

## Third parties

NfcEmu does not integrate any third-party SDKs, analytics, or advertising
libraries.

## Changes to this policy

If this policy changes, the updated version will be published at the same
URL and the "Last updated" date above will change accordingly.

## Contact

Questions about this policy can be raised via the project's GitHub Issues
page.

---

_Hosting note: Play Console requires a publicly reachable URL for this
policy (e.g. via GitHub Pages) - publishing this file is a step for the repo
owner to take before the first Play Console submission, not something done
automatically by committing this file._
