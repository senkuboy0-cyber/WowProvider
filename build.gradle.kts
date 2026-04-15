plugins {
    id("com.android.library") version "8.1.0"
    id("org.jetbrains.kotlin.android") version "1.8.10"
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