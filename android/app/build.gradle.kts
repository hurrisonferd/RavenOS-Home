plugins {
    id("com.android.application")
}

android {
    namespace = "com.ravenos.home"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ravenos.home"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
    }

    sourceSets["main"].assets.srcDir("../../site")
}
