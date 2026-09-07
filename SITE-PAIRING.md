# Scan-to-link (websites)

How a WordPress site gets linked to an account by scanning a QR code, with nothing typed at
either end - no domain, no link code, and no email.

This is separate from **Link a computer**, which pairs the web console over the Realtime Database
and uses `sc1:` codes. Website codes are `sc2:`. Each screen rejects the other's code with a
message that says which screen to use, rather than failing on a lookup.

## Why Firebase sits in the middle

The obvious design is: the app posts the link code straight to the customer's site. It was
rejected for two reasons that are both structural.

1. **The app has no HTTP client.** `UpdateChecker` uses `HttpURLConnection` for the update
   manifest and there is a `WebView` in `MapCard`; there is no OkHttp, Retrofit or Volley
   anywhere. Talking to arbitrary customer domains would mean adding networking to an app that
   deliberately has none, plus cleartext exceptions for the sites that need them.
2. **Phones usually cannot reach a site being set up.** Local installs, staging behind basic
   auth, a laptop on another network, or anything behind a VPN would all fail no matter how
   correct the code was.

Both halves can always reach Firestore, so a short-lived document there is the meeting point.

## The flow

```
WordPress                         Firestore                        Android app
---------                         ---------                        -----------
press "Show QR code"
  create sitePairings/{session}  ->  status: waiting
  draw  sc2:{session}
  poll  every 3s                                                   scan sc2:{session}
                                     read session (get by id)   <- fill in the domain
                                                                   press Register
                                                                     createWebsite(domain)
                                                                     mints apiKey + linkCode
                                     status: ready              <- write linkCode + ownerEmail
  see ready
  SSC_Linker::link(code, email)
    read linkCodes/{code}, burn it
    save settings
  status: done                     ->
  reload the settings page
```

The session id is 32 random characters and it is the only credential involved. The rules allow
`get` on one `sitePairings` document by exact id and refuse `list`, so a session cannot be found
without the QR code - exactly the model `linkCodes` already uses.

## What is in the session document

| Field | Written by | Notes |
| --- | --- | --- |
| `domain`, `siteUrl`, `siteName` | WordPress | What the app shows and fills in |
| `status` | both | `waiting` -> `ready` -> `done`, or `cancelled` |
| `createdAt`, `expiresAt` | WordPress | Ten minutes, capped at fifteen by the rules |
| `linkCode`, `ownerEmail`, `answeredAt` | the app | Added when it answers |

**The API key is never written here.** The only credential that crosses is the eight-character
link code, which is single use, expires in ten minutes, cannot be listed, and is worthless
without the `linkCodes` document it points at. That is the same bargain the typed-code flow
already makes; this just saves the typing.

## Code touched

- `data/api/ApiModels.kt` - `SitePairing`.
- `data/api/SupportApi.kt` - `sitePairing(sessionId)` reads and validates a session;
  `completeSitePairing(sessionId, linkCode, ownerEmail)` answers it. Both go through the existing
  `firebase { }` wrapper, so failures arrive as ordinary `ApiException`s.
- `ui/screens/LinkWebsiteScreen.kt` - a **Scan a site QR code** button under Register, the domain
  auto-fill, and the write-back after a successful registration. It uses the
  `play-services-code-scanner` client that **Link a computer** already uses, so there is no new
  dependency and still no `CAMERA` permission (Play services hosts the scanner UI).
- `firestore.rules` - the `sitePairings` block.

No version change: this is still `versionCode 43` / `versionName 7.0.3`.

## Deploy the rules first

```
firebase deploy --only firestore:rules
```

Until that is done, every scan fails with a permission error, because the plugin's attempt to
create the session is refused. The plugin reports that specific case as "the sitePairings
security rules have probably not been deployed yet" rather than as a generic failure.

## Known limits

- **Anyone reachable can create junk sessions.** Rules cannot rate-limit, so this is capped by
  design rather than prevented: a session expires in ten minutes, holds nothing secret, and links
  nothing on its own. Old documents are not swept up; add a TTL policy on `expiresAt` if the
  collection should self-clean.
- **Re-linking the same domain still needs Remove first.** One website per account is enforced in
  the `createWebsite` transaction and re-checked in the rules, so a site that is already
  registered cannot be re-paired by scanning. "Reuse the existing site when the domain matches"
  would fix that and is not built.
- **A refused code cannot be retried.** It is single use, so the session closes and the owner
  shows a fresh QR code.
- The Kotlin here has not been compiled: this checkout has no Gradle or Android SDK.
