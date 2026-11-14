import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Where the app finds the REST API. Set these in gradle.properties or with -P on the command line.
// Both must end with a slash.
//   apiBaseUrl        - debug builds (what you run from Android Studio). Default: your laptop.
//       USB phone + `adb reverse tcp:8080 tcp:8080` -> http://localhost:8080/api/   (default)
//       Android emulator                            -> http://10.0.2.2:8080/api/
//       Hosted API                                  -> https://<your-host>/api/
//   releaseApiBaseUrl - release builds (Play Store). Must be the hosted HTTPS address.
//                       Falls back to apiBaseUrl when not set.
val apiBaseUrl: String = (findProperty("apiBaseUrl") as String?) ?: "http://localhost:8080/api/"
val releaseApiBaseUrl: String = (findProperty("releaseApiBaseUrl") as String?) ?: apiBaseUrl

// Release signing details live in keystore.properties (never committed - see keystore.properties.example).
val keystoreProps = Properties().apply {

    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProps.containsKey("storeFile")

android {
    namespace = "com.codingandcurls.spunkyshairflair"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.codingandcurls.spunkyshairflair"
        minSdk = 26
        targetSdk = 36          // Google Play requires API 36 for new apps and updates (from 31 Aug 2026)
        versionCode = 3
        versionName = "3.0"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            isMinifyEnabled = false
            // Signed only when keystore.properties exists, so CI and teammates can still build.
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    // Android's own org.json is only a stub in JVM unit tests, so tests use the real library.
    testImplementation("org.json:json:20240303")
}
