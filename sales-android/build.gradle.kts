plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kobe.warehouse.sales"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kobe.warehouse.sales"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Build config fields for API endpoints
        // Note: 10.0.2.2 is the special IP for emulator to access host machine's localhost
        // For physical devices, change to your machine's actual IP (e.g., 192.168.1.188)
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:9080/\"")
        buildConfigField("String", "AUTH_ENDPOINT", "\"/api/auth/login\"")
        buildConfigField("String", "ACCOUNT_ENDPOINT", "\"/api/account\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = rootProject.findProperty("RELEASE_STORE_PASSWORD") as String? ?: ""
            keyAlias = rootProject.findProperty("RELEASE_KEY_ALIAS") as String? ?: ""
            keyPassword = rootProject.findProperty("RELEASE_KEY_PASSWORD") as String? ?: ""
        }
        create("internal") {
            storeFile = file("internal-release.jks")
            storePassword = rootProject.findProperty("INTERNAL_STORE_PASSWORD") as String? ?: ""
            keyAlias = rootProject.findProperty("INTERNAL_KEY_ALIAS") as String? ?: ""
            keyPassword = rootProject.findProperty("INTERNAL_KEY_PASSWORD") as String? ?: ""
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
            isDebuggable = false
        }
        create("staging") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            applicationIdSuffix = ".staging"
        }
        create("internal") {
            initWith(getByName("release"))
            applicationIdSuffix = ".internal"
            signingConfig = signingConfigs.getByName("internal")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
        compose = true
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

    buildToolsVersion = "36.0.0"
    ndkVersion = "26.3.11579264"
}

dependencies {
    // Jetpack Compose BOM (Bill of Materials)
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))

    // Jetpack Compose dependencies
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Compose Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")

    // Compose integration with existing libraries
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime-rxjava3")

    // Compose debugging tools (debug only)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit for networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // RecyclerView & SwipeRefreshLayout
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // SharedPreferences - Encrypted (for secure token storage)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // LocalBroadcastManager for session event broadcasting
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // Room Database (optional for offline caching)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Sunmi Printer Library (for thermal receipt printing)
    implementation("com.sunmi:printerlibrary:1.0.18")

    // ZXing for QR Code generation and scanning
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Image loading library (Coil for Kotlin)
    implementation("io.coil-kt:coil:2.5.0")

    // Paging 3 for pagination
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")

    // Navigation component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // ViewPager2 for responsive layouts
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Card View
    implementation("androidx.cardview:cardview:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Compose testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
