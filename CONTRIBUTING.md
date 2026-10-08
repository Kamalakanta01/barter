# Contributing

Thanks for taking the time. Small, focused pull requests are easier to review than large ones.

## Setup

1. Install JDK 21 and the Android SDK with platform 37.
2. Point the build at the SDK in `local.properties`:
   ```properties
   sdk.dir=/path/to/Android/sdk
   ```
3. Confirm the baseline is green before you change anything:
   ```bash
   ./gradlew build
   ```

## Making a change

- Match the surrounding style; `.editorconfig` pins the basics. Kotlin uses official code style
  (`kotlin.code.style=official`).
- Keep dependency versions in `gradle/libs.versions.toml`. Never hardcode a version in a module
  build file.
- Keep repeated Android configuration in a `build-logic` convention plugin rather than copying it
  into modules.
- Respect the dependency direction: `app` → `feature/*` → `core/*`, never the reverse. A feature's
  `ui` layer must talk to `domain` interfaces, not to `data` implementations.
- Put barter rules in `core:domain`'s `BarterRules`, not in a repository. A repository applies them and
  publishes the result; that is what keeps the Room migration logic-free.
- Add or update a test. ViewModel tests need no Android runtime — see
  `feature/browse/src/test/java/dev/barter/feature/browse/BrowseViewModelTest.kt`.
- Do not commit `local.properties`, keystores, or anything in `secrets.properties`.

## Before opening a pull request

```bash
./gradlew build          # compiles every variant, runs unit tests and Lint
./gradlew connectedCheck # only if you touched code that needs a device
```

CI runs the same commands on JDK 21. It must pass before a review starts.

## Reporting bugs

Open an issue with the device and OS version, the Barter version, and the steps to reproduce.
