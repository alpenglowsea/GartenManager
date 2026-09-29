plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.alpenglowsea.gartenmanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.alpenglowsea.gartenmanager"
        minSdk = 26          // Android 8.0, entschieden am 29.09.2026
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    // Test-Signatur nur fuer Entwicklungs-Builds (oeffentlich, nicht geheim).
    // Fuer echte Veroeffentlichungen kommt spaeter ein eigener, geheimer Schluessel.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // F-Droid verlangt, dass keine Google-signierten Abhaengigkeitsinfos in der App stecken.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
