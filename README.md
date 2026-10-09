# Pozzo Mobile

Pozzo is a modern Android mobile application built with **Kotlin** and **Jetpack Compose** designed to digitize and manage community savings groups (*juntas*, tandas, or ROSCAs—Rotating Savings and Credit Associations). The app provides automated payment tracking, turn rotation scheduling, receipt verification with on-device OCR, and participant reliability scoring.

---

## Technical Specifications

- **Language:** Kotlin 2.4+
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Minimum SDK:** API 26 (Android 8.0 Oreo)
- **Target SDK:** API 37
- **Compile SDK:** API 37
- **Java Compatibility:** Java 11 / Java 17 toolchain
- **Dependency Injection:** Hilt
- **Local Persistence:** Room Database & Jetpack DataStore Preferences
- **Networking:** Retrofit, OkHttp, Gson
- **On-Device ML:** Google ML Kit Text Recognition (receipt scanning)
- **Push Notifications:** Firebase Cloud Messaging (FCM)
- **Image Loading:** Coil 3

---

## Architecture Overview

The codebase is organized following **Clean Architecture** and **Feature-First** modular patterns, ensuring separation of concerns, testability, and scalability.

```
app/src/main/java/pe/kerolabs/pozzo/
├── core/                       # Shared modules and cross-cutting concerns
│   ├── database/               # Room database definition and migrations
│   ├── designsystem/           # Reusable Compose UI components, themes, typography
│   ├── di/                     # Global Hilt dependency injection modules
│   ├── format/                 # Formatting utilities (currency, dates, phone numbers)
│   ├── network/                # HTTP client configuration, interceptors, error handling
│   └── push/                   # Push notification dispatchers and channels
├── features/                   # Business capabilities
│   ├── compliancehistory/      # Punctuality metrics, reputation score, and contribution history
│   ├── contributions/          # Payment recording, receipt verification (OCR), and pot status
│   ├── iam/                    # Identity & Access: authentication, profile, session, preferences
│   ├── notifications/          # In-app notices and push notification history
│   └── savingsgroups/          # Group lifecycle: creation, invite codes, members, and turn rotations
└── navigation/                 # AppNavHost, top-level routes, session state, and theme management
```

### Architectural Layers (per Feature)

Each feature package is partitioned into the following layers:

1. **`domain`**: Pure Kotlin layer containing business models, value objects, and repository contracts (interfaces). Independent of Android platform frameworks.
2. **`application`**: Use cases coordinating business logic and interacting with domain repositories.
3. **`infrastructure`**: Concrete implementations of repositories, Retrofit API services, DTOs, mappers, Room DAOs, and local preference managers.
4. **`presentation`**: UI layer composed of Jetpack Compose screens, ViewModels (MVVM with Unidirectional Data Flow), UI states (`StateFlow`), and type-safe navigation graphs.

---

## Getting Started

From a fresh clone to the app running on an emulator or device:

1. **Clone the repository** and switch to the integration branch:
   ```bash
   git clone https://github.com/kerolabs/pozzo-mobile.git
   cd pozzo-mobile
   git checkout develop
   ```
2. **Open the project in Android Studio** (*File > Open* and select the `pozzo-mobile` folder) and let the first Gradle sync finish. The sync also runs `git config core.hooksPath .githooks`, so the commit-message hook is active from the start.
3. **Pick a build variant** in *Build > Select Build Variant*:
   - `cloudDebug` to use the deployed backend (recommended for most work, no extra setup).
   - `localDebug` to use a backend running on your machine (see [Working with the Local Flavor](#working-with-the-local-flavor)).
4. **Run the app** with *Run > Run 'app'* on an emulator or a USB-connected device running Android 8.0 (API 26) or newer.
5. **Before opening a Pull Request**, read [CONTRIBUTING.md](CONTRIBUTING.md) for the branching model, code style and PR checklist.

> Firebase is already configured: `app/google-services.json` is committed and registers both `pe.kerolabs.pozzo` and `pe.kerolabs.pozzo.local`, so push notifications work in either flavor without extra steps.

---
## Build Variants & Product Flavors

Pozzo defines a `backend` flavor dimension with two configurations:

| Flavor | Application ID | Target Environment | Default API Base URL |
|---|---|---|---|
| `cloud` *(default)* | `pe.kerolabs.pozzo` | Oracle Cloud Production/Staging | `https://api-kerolabs.duckdns.org/api/v1/` |
| `local` | `pe.kerolabs.pozzo.local` | Localhost Development Backend | `http://localhost:8080/api/v1/` |

### Working with the Local Flavor
When running a local backend instance on your development machine, forward the port to your connected device or emulator:
```bash
adb reverse tcp:8080 tcp:8080
```
You can also override the URL in `local.properties`:
```properties
pozzo.localApiBaseUrl=http://10.0.2.2:8080/api/v1/
```

---

## Build Instructions

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17 or compatible toolchain
- Android SDK Platform 37

### Building from Command Line

Build the debug APK for the default flavor (`cloud`):
```bash
./gradlew assembleDebug
```

Build debug APKs for specific flavors:
```bash
# Cloud flavor (default)
./gradlew assembleCloudDebug

# Local flavor
./gradlew assembleLocalDebug
```

Run unit tests:
```bash
./gradlew test
```

Run Android lint checks:
```bash
./gradlew lint
```

### Release Signing
Release builds can be signed locally by adding the following properties to `local.properties` (never committed to git):
```properties
pozzo.keystore.path=/path/to/release.keystore
pozzo.keystore.password=your_keystore_password
pozzo.keystore.alias=your_key_alias
pozzo.keystore.keyPassword=your_key_password
```

---

## Git Workflow and Commit Policies

The project enforces **Conventional Commits** via pre-commit Git hooks configured in `.githooks/` and CI checks:
- Format: `<type>: <description>` (e.g., `feat: implement receipt upload screen`)
- Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`
- Commits must not contain AI/bot tool trailers (`Co-authored-by`, `Signed-off-by`).
