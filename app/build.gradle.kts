import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "uk.appyapp.stepsafe"
    compileSdk = 36

    // Load local properties (API keys, debug keystore credentials) so sensitive values live outside VCS
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    // Maps API key (used below in defaultConfig)
    val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""

    // Debug keystore properties (must be provided in local.properties)
    val debugKeystorePath = requireNotNull(localProperties.getProperty("DEBUG_KEYSTORE_PATH")) {
        "DEBUG_KEYSTORE_PATH must be set in local.properties and point to the debug keystore (e.g. keystores/debug.jks)"
    }
    val debugKeystoreStorePassword = requireNotNull(localProperties.getProperty("DEBUG_KEYSTORE_STORE_PASSWORD")) {
        "DEBUG_KEYSTORE_STORE_PASSWORD must be set in local.properties"
    }
    val debugKeystoreKeyAlias = requireNotNull(localProperties.getProperty("DEBUG_KEYSTORE_KEY_ALIAS")) {
        "DEBUG_KEYSTORE_KEY_ALIAS must be set in local.properties"
    }
    val debugKeystoreKeyPassword = requireNotNull(localProperties.getProperty("DEBUG_KEYSTORE_KEY_PASSWORD")) {
        "DEBUG_KEYSTORE_KEY_PASSWORD must be set in local.properties"
    }

    defaultConfig {
        applicationId = "uk.appyapp.stepsafe"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Maps API key read from top-level localProperties (see above)
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        resValue("string", "google_maps_key", mapsApiKey)
    }

    signingConfigs {
        create("debugStore") {
            storeFile = rootProject.file(debugKeystorePath)
            storePassword = debugKeystoreStorePassword
            keyAlias = debugKeystoreKeyAlias
            keyPassword = debugKeystoreKeyPassword
        }
    }
    buildTypes {
        // TEMPORARY: For testing only — this `release` build is configured to use the
        // repository/debug signing key and enables code shrinking/obfuscation. DO NOT publish
        // APKs signed with this key to Google Play. Before publishing, restore the release
        // signing configuration to use the real private release key and verify signing settings.
        release {
            // Enable R8/ProGuard optimization and resource shrinking for a realistic 'release' build
            isMinifyEnabled = true
            isShrinkResources = true
            // Use the optimized default proguard file and the project's proguard rules
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sign the temporary release with the debugStore keystore (provided via local.properties)
            signingConfig = signingConfigs.getByName("debugStore")
        }
        // Use a repository-stored debug keystore for consistent debug signing across devs
        debug {
            // Path is relative to the module directory; keystores/debug.jks placed at repo root by default
            signingConfig = signingConfigs.findByName("debugStore") ?: signingConfigs.create("debugStore") {
                storeFile = rootProject.file(debugKeystorePath)
                storePassword = debugKeystoreStorePassword
                keyAlias = debugKeystoreKeyAlias
                keyPassword = debugKeystoreKeyPassword
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.accompanist.permissions)
    implementation(libs.play.services.location)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)
    implementation(libs.moshi.kotlin)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)
    
    // Google Maps
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.android)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.uiautomator)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.hilt.android.compiler)
    "ksp"(libs.hilt.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
}
