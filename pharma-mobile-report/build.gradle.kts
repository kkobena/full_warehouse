plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.kobe.warehouse.reports"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kobe.warehouse.reports"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default base URL for API calls
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:9080/\"")

        // Firebase configuration
        buildConfigField("Boolean", "FIREBASE_ENABLED", "true")
    }

    signingConfigs {
        create("release") {


          storeFile = file("release.jks")
          storePassword = rootProject.findProperty("RELEASE_STORE_PASSWORD") as String
          keyAlias = rootProject.findProperty("RELEASE_KEY_ALIAS") as String
          keyPassword = rootProject.findProperty("RELEASE_KEY_PASSWORD") as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            // Production API URL - empty string forces user to configure in settings
            buildConfigField("String", "BASE_URL", "\"\"")

            // Firebase enabled in production by default
            buildConfigField("Boolean", "FIREBASE_ENABLED", "true")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"

            // Debug API URL (emulator)
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:9080/\"")

            // Firebase can be disabled in debug for testing
            buildConfigField("Boolean", "FIREBASE_ENABLED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Security (Encrypted SharedPreferences)
    implementation(libs.androidx.security.crypto)

    // Swipe Refresh
    implementation(libs.androidx.swiperefreshlayout)

    // Image loading
    implementation(libs.coil)

    // Barcode scanning
    implementation(libs.zxing.android.embedded)

    // Room Database (for offline caching)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    annotationProcessor(libs.androidx.room.compiler)

    // WorkManager (for background sync)
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // TensorFlow Lite (ML Forecasting) - Disabled until 16KB page size support
    // implementation("org.tensorflow:tensorflow-lite:2.14.0")
    // implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // Shimmer loading effect
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // Badge count for all launchers
    implementation("me.leolin:ShortcutBadger:1.1.22@aar")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
