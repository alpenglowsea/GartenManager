// Oberste Build-Datei: legt nur fest, welche Plugins es gibt.
// Die eigentliche Arbeit steht in app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
