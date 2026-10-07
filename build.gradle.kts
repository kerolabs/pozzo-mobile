// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Points git at the shared hooks in .githooks, so the commit-msg hook that
// enforces the commit policy is active as soon as the project syncs.
if (rootDir.resolve(".git").exists()) {
    runCatching {
        providers.exec {
            commandLine("git", "config", "core.hooksPath", ".githooks")
        }.result.get()
    }
}