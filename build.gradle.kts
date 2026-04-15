plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 33
    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    implementation("com.lagradost.cloudstream3:cloudstream3:1.7.0")
}