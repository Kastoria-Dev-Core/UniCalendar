import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.kastoriadev.unicalendar"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.kastoriadev.unicalendar"
        minSdk = 26
        targetSdk = 35
        versionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull() ?: 10
        versionName = project.findProperty("versionName")?.toString() ?: "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }

    val storeFilePath: String = System.getenv("ANDROID_KEYSTORE_PATH")
        ?: System.getenv("KEYSTORE_PATH")
        ?: keystoreProperties.getProperty("storeFile")
        ?: "keystore/release.jks"
    val storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        ?: System.getenv("KEYSTORE_PASSWORD")
        ?: keystoreProperties.getProperty("storePassword")
    val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
        ?: System.getenv("KEY_ALIAS")
        ?: keystoreProperties.getProperty("keyAlias")
    val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        ?: System.getenv("KEY_PASSWORD")
        ?: keystoreProperties.getProperty("keyPassword")

    val candidateStoreFiles = listOf(
        project.file(storeFilePath),
        rootProject.file(storeFilePath),
        rootProject.file("app/$storeFilePath"),
        project.file("keystore/release.jks"),
        rootProject.file("app/keystore/release.jks"),
        rootProject.file("keystore/release.jks")
    )
    val resolvedStoreFile = candidateStoreFiles.firstOrNull { it.exists() } ?: project.file(storeFilePath)
    val hasReleaseSigning = resolvedStoreFile.exists() &&
        !storePassword.isNullOrEmpty() &&
        !keyAlias.isNullOrEmpty() &&
        !keyPassword.isNullOrEmpty()

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = resolvedStoreFile
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                // Fall back to debug signing if release credentials are not provided,
                // ensuring the produced APK is ALWAYS signed and installable.
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.okhttp)
    implementation(libs.jsoup)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    // Android's org.json is a stub in local unit tests; use the real implementation there.
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
