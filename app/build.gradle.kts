plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.custom.usbwriter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.custom.usbwriter"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("me.jahnen.libaums:core:0.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
