# Even

An open source barter app: list what you own, see what nearby people want, and swap directly. No
account, no server you host — identity comes from your phone's SIM, and the data lives on your
device.

Kotlin · Jetpack Compose · Clean Architecture across Gradle modules · Hilt · CI.

## Toolchain

| Component           | Version    | Notes                                                   |
|---------------------|------------|---------------------------------------------------------|
| Gradle              | 9.6.0      | Required by AGP 9.4                                      |
| Android Gradle Plugin | 9.4.0    | Built-in Kotlin; the `kotlin-android` plugin is gone     |
| Kotlin              | 2.4.20     | Selected via the root `buildscript` classpath            |
| KSP                 | 2.3.12     | The only supported annotation processing path            |
| Hilt                | 2.60.1     | `com.google.dagger:hilt-compiler` under `ksp`           |
| Compose BOM         | 2026.09.00 |                                                          |
| JDK                 | 21         | 17 is the AGP minimum; 21 is what this pins              |
| compileSdk/targetSdk| 37         | Android 17                                               |
| minSdk              | 24         | Hilt 2.60 requires 23 or higher                          |

## Getting started

```bash
# JDK 21 (see .java-version for jenv/asdf users)
./gradlew :app:assembleDebug
```

Point the build at the Android SDK (platform 37) in `local.properties`, which is git-ignored:

```properties
sdk.dir=/path/to/Android/sdk
```

## Module layout

```
core/domain/       Pure Kotlin. Listing, SwapOffer, BarterState, BarterRules. No Android at all.
core/identity/     TraderIds — HMAC derivation of a pseudonymous id from a phone number.
core/data/         Room database and the repository that mirrors BarterState to it.
core/designsystem/ EvenTheme, LetterTile, EvenCard.
feature/browse/    Hero, search, listing grid, swap dialog.
feature/myitems/   Your listings, remove, add-listing dialog.
feature/offers/    Incoming and outgoing offers, accept/decline/withdraw.
app/               EvenNavHost, the tab bar, and the Hilt entry point.
build-logic/       Convention plugins shared by every module.
```

Dependencies run one way only:

```
app ──┬──> feature/browse  ──┐
     ├──> feature/myitems ──┼──> core/domain  (pure Kotlin)
     ├──> feature/offers  ──┘        ▲
     └──> core/data ─────────────────┘   binds impl → interface
```

`app` depends **only** on `core:data`, which `api`-exposes `core:domain`, so a single dependency
supplies both the Hilt bindings and the repository interface. Features depend on `core:domain`
directly and must never import `core:data`.

### Where the rules live

Every legal transition is a pure `BarterState` to `BarterState` function in `BarterRules`, not a
method on a repository. Repositories just apply them:

```
BarterRules.acceptOffer(state, offerId)   →  new state
RoomBarterRepository.acceptOffer(id)      →  applies the rule, saves the new state, publishes it
```

This is why swapping the store changes no logic, and why the rule tests in `core:domain` run on a
plain JVM with no emulator in about a second.

### Ownership, not a boolean

A `Listing` is owned by a `TraderId`, so `myListings(myId)` is a filter rather than a stored flag.
That is what lets a listing arriving from a nearby phone look exactly like one that was already on
the device — which is the whole point of the next phases.

## Phases

- **1 — done.** Domain, in-memory store, and the three screens ported from the HTML prototype.
- **2 — in progress.** Room storage is done: listings and offers now survive restarts, and no demo
  data ships. Still to do: wire `TraderIds.fromPhoneNumber` to `TelephonyManager.getLine1Number()`,
  store the device secret in the Keystore, and fall back to a locally generated id when the carrier
  reports no number (with a Room migration for the owner id, see `IdentityModule`).
- **3.** `core:transport` behind one interface: Nearby Connections for proximity, Nostr for global.
  This is also where reputation has to land, before the schema hardens around the idea that a
  verified number means something.
- **4.** `feature:onboarding`, and the scope switch between Nearby and Global.

## Testing

```bash
./gradlew test              # JVM unit tests
./gradlew connectedCheck    # instrumented tests, needs a device or emulator
./gradlew lint              # Android Lint
```

Feature ViewModel tests share `FakeBarterRepository` from `core:domain`'s test fixtures, so they
exercise the same production rules as the real store without Hilt or Android.

## Known gaps

- Fonts are the platform sans-serif with matched weights and tracking. The prototype specifies
  Bricolage Grotesque and Hanken Grotesk, which would need font resources to ship.
- Release signing is not configured, because keystore handling differs per distribution channel. Add
  a signing config to `app/build.gradle.kts` reading from `~/.gradle/gradle.properties` (not
  `local.properties`, which Gradle does not read).
- No reputation or Sybil resistance yet; without a server there is no moderation.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). By participating you agree to the
[Code of Conduct](CODE_OF_CONDUCT.md).

## License

[MIT](LICENSE).
