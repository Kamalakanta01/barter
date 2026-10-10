# Security model

What Even defends against, what it does not, and why each choice was made. Written before the
transport exists so the decisions are reviewable rather than implied by whatever shipped first.

The short version: **Even has two distinct adversaries, and they need different tools.** A local
swap exposes you to the person in front of you. Browsing the national feed exposes you to whoever
runs the infrastructure. Anonymity only helps with the second; authenticity helps with the first.

## The adversaries

### 1. The counterparty

The person you are physically swapping with. They know where you are, they see your face, and they
see your `TraderId`.

This threat is not solved by anonymity — it is barely affected by it. They are at the other end of
the connection; there is no relay in between to misdirect. Worse, a *stable* pseudonymous id is
itself a liability here: meet twenty people and each of them sees the same id, and a few of them
talking to each other reconstructs a history of your movements.

What actually helps:

| Measure | Defends against |
|---|---|
| Noise XX handshake, forward secrecy | A counterparty who records traffic and decrypts it later |
| Ephemeral session keys | A counterparty linking your separate swaps into one profile |
| Mutual authentication | A third party impersonating either side mid-exchange |

This is why Even wants a Noise handshake rather than "encrypted". The property worth having is
*authenticatable*, not *anonymous*: you want to know the bytes came from this trader unmodified,
not that nobody can ever learn you were there.

### 2. Network and state monitoring

Whoever operates the relay or blob store, and whoever can observe the path — an ISP, a carrier, or
an agency able to compel them.

This is where Tor belongs, and it belongs on the **Global path only**:

| Measure | Defends against |
|---|---|
| Tor on the Global path | Carrier or relay learning your IP and what you browse or offer |
| End-to-end encryption between peers | Relay or exit node reading or altering a message |
| Encrypted data at rest | A lost, seized, or compelled-to-unlock device |

**Tor does not provide end-to-end encryption between peers.** A Tor exit node terminates TLS by
design, so it sees plaintext. If offers and messages carry real content, peer-to-peer encryption is
mandatory and Tor is only the road underneath it. Exit nodes are volunteer-run and are frequently
hostile; without peer authentication, one can rewrite an offer in flight.

## Why Tor is not used for local transfers

The instinct — "P2P direct exposes metadata, route it through Tor instead" — is reasonable and
wrong in this specific case, for four reasons:

1. **It cannot work.** Tor obscures your origin from a destination you reach over the internet. A
   peer two metres away needs no obscuring, and the BLE discovery broadcast that revealed your
   presence has already happened, in the clear.
2. **It takes the device's VPN slot.** On Android, Tor means Orbot means `VpnService` means the one
   per-device VPN slot. A user on a corporate VPN, or their own VPN, cannot use both. Even breaks or
   silently displaces it. This is a real usability cost paid by every user to defend a threat that
   does not apply here.
3. **Latency is wrong for barter.** Circuit setup takes seconds; steady-state round trips are
   100–300 ms. A swap is a live, two-person, in-person exchange.
4. **Trust grows rather than shrinks.** Direct P2P trusts one peer. Via Tor you trust three relay
   operators plus the blob host — a larger surface, merely anonymous.

There is also a product tension: Tor makes throwaway identities cheap, which cuts against the
reputation and Sybil-resistance work Phase 3 is expected to land. Those pull in opposite directions.

## Limits worth stating plainly

- **Tor is not immunity.** It defeats content reading and casual correlation, not a well-resourced
  adversary with enough vantage to correlate globally.
- **Metadata still leaks under Tor.** A relay sees connection timing and volume. On an intermittent
  barter app this is low-signal, but it is not zero.
- **Encryption at rest does not help a running device.** Neither FBE nor SQLCipher protects a
  compromised OS, a rooted phone with runtime access, or an unlocked session. They raise the cost of
  *offline* extraction: a lost phone, a seizure, or a compelled unlock.

## Properties already in place

These are correct today. They are recorded here so nobody "simplifies" them away.

| Property | Where |
|---|---|
| `allowBackup="false"` — plaintext DB never reaches cloud backup | `app/src/main/AndroidManifest.xml` |
| No `INTERNET` permission — currently zero network attack surface | `app/src/main/AndroidManifest.xml` |
| The phone number is HMAC'd, never stored, and is not recoverable from a `TraderId` | `core/identity` |
| Every rule is a pure `BarterState` to `BarterState` function, so one writer owns each transition | `core/domain/BarterRules.kt` |
| The number is only ever HMAC input, and the device secret is not yet on a Keystore | `core/identity/TraderIds.kt` |

The identity design is doing more privacy work than it is credited for: Even genuinely cannot
produce your phone number from the id it stores. Phase 2 finishing this — real SIM-derived ids plus
a Keystore-held device secret — is the foundation for the counterparty threat, because a device
secret is what stops someone who knows your number from computing your id.

## Encryption at rest

**Recommendation: `net.zetetic:sqlcipher-android` with a Keystore-held passphrase, and image bytes
stored as BLOBs in the same database.**

```kotlin
implementation("net.zetetic:sqlcipher-android:4.19.1")
implementation("androidx.sqlite:sqlite:2.7.0")
```

Room integrates through `openHelperFactory(SupportOpenHelperFactory(passphrase))`. SQLCipher 4.18.0
and later support Room 3 at `compileSdk` 37, which is what this project builds against.

Why this over a Keystore-backed file key:

- **One boundary instead of two.** A file key means encrypting the database and every image file
  separately, with two key stores to get wrong. Storing image bytes as BLOBs puts everything behind
  a single encryption boundary.
- **The WAL is covered.** A plaintext `-wal` alongside an encrypted database is a common and easy
  leak. SQLCipher encrypts it as part of the page store.
- **No bespoke crypto.** Implementing page-level encryption by hand is how subtle, permanent holes
  get made.

Two honest caveats:

- Android's file-based encryption already covers app-private files by default from API 28, and
  `minSdk` here is 24. SQLCipher's marginal benefit over that is rooted devices, offline extraction,
  and the API 24–27 slice — not protection against a live compromise.
- Store image BLOBs only while the image count is modest. A user's own items are small; a future
  peer cache should live in files and be a separate decision.

The passphrase is a random 256-bit value generated once, wrapped by a Keystore key, and rewrapped
only on `setUserAuthenticationRequired` changes. It is never written to disk or to `SharedPreferences`
in the clear.

## Build order

Each step is testable on its own and commits independently.

1. **Phase 2 — identity.** SIM-derived ids, Keystore-held device secret. The linkability
   foundation for the counterparty threat.
2. **Encrypted at rest.** The largest single win for the monitoring adversary, and it needs no
   network, no relays, and no VPN slot.
3. **Noise XX handshake** with ephemeral session keys.
4. **Nearby transport** for local discovery and transfer.
5. **Images**, once reputation and transport have determined what a listing *is*.
6. **Tor on Global**, as a toggle, for browsing and messaging off-device.

## Keep out of this repository

Publishing a design note is not a security risk here, because the source is public regardless and a
doc reveals intent rather than capability. Three things are different:

- **Relay and blob-host lists.** A recommended list is simultaneously an availability target and a
  correlation set: anyone connecting to every listed relay can be linked. Keep them out of git and
  out of user-facing docs.
- **Custom cryptographic designs.** If a bespoke construction is ever proposed, its specification
  must not live in a public document. Standard primitives with published constructions are fine.
- **Discovery beacon timing.** "We broadcast every Y ms" is a fingerprinting surface. Describe the
  mechanism, not the parameters.

## Open questions

Not a list of accepted weaknesses — things genuinely undecided.

- Where Global image bytes are hosted, and who operates it.
- Whether Tor-on-Global ships, given there may be no infrastructure to protect.
- Reputation and Sybil resistance, which interact badly with cheap anonymous identity.
- Whether an offer is revocable after both parties meet, which is a product question with a
  security consequence.