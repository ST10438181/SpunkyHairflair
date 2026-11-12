plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Where the app finds the REST API. Override with  -PapiBaseUrl=https://your-host/api/
// (or add apiBaseUrl=... to gradle.properties). Must end with a slash.
//   USB phone + `adb reverse tcp:8080 tcp:8080` -> http://localhost:8080/api/   (default)
//   Android emulator                            -> http://10.0.2.2:8080/api/
//   Hosted API                                  -> https://<your-host>/api/
val apiBaseUrl: String = (findProperty("apiBaseUrl") as String?) ?: "http://localhost:8080/api/"

android {
    namespace = "com.codingandcurls.spunkyshairflair"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.codingandcurls.spunkyshairflair"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
