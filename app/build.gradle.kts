plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.pengeluaran"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pengeluaran"
        minSdk = 31
        targetSdk = 34

        val passedVersionCode = project.findProperty("customVersionCode")?.toString()?.toIntOrNull() ?: 1
        val passedVersionName = project.findProperty("customVersionName")?.toString() ?: "1.0.0"

        versionCode = passedVersionCode
        versionName = passedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("releaseKey") {
            // Menggunakan keystore tetap yang digenerate oleh pipeline dengan seed statis
            storeFile = file("${System.getProperty("user.home")}/release.keystore")
            storePassword = "password123"
            keyAlias = "releasealias"
            keyPassword = "password123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Terapkan keystore statis
            signingConfig = signingConfigs.getByName("releaseKey")
        }
    }

    // ... sisa compileOptions, kotlinOptions, dependencies tetap sama


dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Network (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
