# Contributing to Pozzo Mobile

Thank you for contributing to the Pozzo Android mobile application. This guide outlines the development standards, branching strategy, Compose best practices, and pull request checklist required for all contributors.

---

## 1. Branching Workflow

Pozzo follows a GitFlow-inspired branching model centered around `develop` and `main`:

```
main (Production releases)
  ▲
  │ Pull Request
develop (Active development integration)
  ▲
  ├── feat/user-receipt-scan
  ├── fix/token-refresh-expiry
  └── docs/code-comments-and-guidelines
```

- **`develop`**: The primary branch for ongoing feature integration. All feature and bugfix branches must be branched off `develop` and merged back into `develop` via Pull Requests.
- **`main`**: Represents production-ready releases. Only stable code from `develop` is merged into `main`.
- **Feature/Topic Branches**: Create short-lived branches named with descriptive prefixes:
  - `feat/<topic>`: New user-facing features or capabilities
  - `fix/<topic>`: Bug fixes and issue patches
  - `docs/<topic>`: Documentation additions and comment updates
  - `refactor/<topic>`: Code restructuring without functional behavior changes
  - `test/<topic>`: Adding or modifying unit/instrumentation tests

---

## 2. Kotlin & Android Code Style

We adhere to the [Official Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and enforce style through `.editorconfig`:

- **Indentation:** 4 spaces for Kotlin/Java; 2 spaces for JSON, YAML, and XML.
- **Line Length:** Maximum 120 characters.
- **Wildcard Imports:** Star imports (`import foo.bar.*`) are strictly prohibited; always use explicit imports.
- **Trailing Commas:** Recommended on multiline parameter lists and call sites for cleaner git diffs.
- **Naming Conventions:**
  - `PascalCase` for classes, interfaces, sealed hierarchies, and `@Composable` functions emitting UI.
  - `camelCase` for variables, properties, methods, and `@Composable` functions returning values.
  - `UPPER_SNAKE_CASE` for compile-time constants (`const val`).
- **File Organization:** Order declarations logically: companion objects, properties, public functions, followed by private helpers.

---

## 3. Jetpack Compose Guidelines

To maintain predictable state management, optimal recomposition performance, and high reusability:

### State Hoisting & Unidirectional Data Flow (UDF)
- Separate stateful screen containers from stateless UI components.
- Screen composables collect `StateFlow` from ViewModels using `collectAsStateWithLifecycle()`.
- Reusable UI composables must be **stateless**: they accept data parameters (state down) and emit lambdas for user actions (events up).

### Modifier Best Practices
- Every reusable `@Composable` should accept an optional `modifier: Modifier = Modifier` parameter.
- The `modifier` parameter should be the first optional parameter in the argument list.
- Always apply the passed `modifier` to the root layout node within the composable.

### Recomposition & Performance
- Avoid allocating objects (e.g., lambdas, formatters) repeatedly inside composables without `remember`.
- Pass stable IDs to `LazyColumn` and `LazyRow` items using the `key` parameter.
- Use derived state (`remember(keys) { ... }` or `derivedStateOf`) when performing expensive calculations.

### Previews
- Provide `@Preview` annotations for leaf and layout components.
- Wrap previews in `PozzoTheme` to verify both light and dark mode appearance.

---

## 4. Commit Message Policy

Pozzo enforces the **Conventional Commits** specification via `.githooks/commit-msg` and CI pipelines:

### Format
```
<type>: <description>
```
*Optional scope:* `<type>(<scope>): <description>`

### Types
- `feat`: A new user-facing feature
- `fix`: A bug fix
- `docs`: Documentation changes only (e.g., README, KDoc)
- `style`: Formatting, missing semi-colons, no code changes
- `refactor`: Refactoring code without altering logic or adding features
- `perf`: Performance improvements
- `test`: Adding or correcting tests
- `build`: Changes that affect the build system or dependencies
- `ci`: Changes to CI configuration files and scripts
- `chore`: Routine maintenance tasks

### Requirements
- Commit descriptions must be written in English and begin with a lowercase imperative verb.
- **No Tool Trailers:** Commits must NOT contain automated tool signatures (e.g., `Co-authored-by: ...`, `Signed-off-by: ...`).
- Enable local hooks upon cloning:
  ```bash
  git config core.hooksPath .githooks
  ```

---

## 5. Pull Request Checklist

Before submitting a Pull Request targeting `develop`, verify that:

- [ ] Branch is up to date with the latest `develop` branch.
- [ ] Code compiles without warnings: `./gradlew assembleDebug`.
- [ ] All unit tests pass: `./gradlew test`.
- [ ] No functional business logic has been broken or altered unexpectedly.
- [ ] Jetpack Compose previews render properly.
- [ ] KDoc comments are added to new public interfaces, composables, and ViewModels.
- [ ] All commits comply with the Conventional Commits policy.
- [ ] Temporary debug code, prints, and commented-out code blocks have been removed.
