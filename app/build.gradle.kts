import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "pe.kerolabs.pozzo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pe.kerolabs.pozzo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Landing page with the Terms and Conditions and the Privacy Policy; override it with pozzo.landingUrl
        val landingUrl = localProperties.getProperty("pozzo.landingUrl") ?: "https://kerolabs.github.io/pozzo-landing-page/"
        buildConfigField("String", "LANDING_URL", "\"$landingUrl\"")
    }

    // Which backend the app talks to; switch it in Android Studio > Build Variants.
    flavorDimensions += "backend"
    productFlavors {
        // The deployed backend: for real phones, the demo and the delivered APK.
        create("render") {
            dimension = "backend"
            isDefault = true
            resValue("string", "app_name", "Pozzo")
            buildConfigField("String", "API_BASE_URL", "\"https://pozzo-backend.onrender.com/api/v1/\"")
        }
        // A backend running on this computer. It installs next to the other one as "Pozzo Local".
        // The emulator, or a phone on USB, reaches it after "adb reverse tcp:8080 tcp:8080".
        create("local") {
            dimension = "backend"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            resValue("string", "app_name", "Pozzo Local")
            val localApiBaseUrl = localProperties.getProperty("pozzo.localApiBaseUrl") ?: "http://localhost:8080/api/v1/"
            buildConfigField("String", "API_BASE_URL", "\"$localApiBaseUrl\"")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.extended)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // ML Kit: reads the receipts on the device
    implementation(libs.mlkit.text.recognition)

    // Push notifications (Firebase Cloud Messaging)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Data Store
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}