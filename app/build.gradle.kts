plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// La clave de firma vive fuera del repo. En local se lee de ~/.gradle/gradle.properties
// (fitlog.storeFile, fitlog.storePassword, fitlog.keyAlias, fitlog.keyPassword);
// en GitHub Actions, de las variables de entorno FITLOG_*.
fun signingValue(name: String): String? =
    providers.gradleProperty("fitlog.$name").orNull
        ?: providers.environmentVariable("FITLOG_" + name.replace(Regex("([A-Z])"), "_$1").uppercase()).orNull

android {
    namespace = "cl.cescur.fitlog"
    compileSdk = 37

    defaultConfig {
        applicationId = "cl.cescur.fitlog"
        minSdk = 26
        targetSdk = 36
        // GitHub Actions pasa la versión a partir de la etiqueta (v1.2.3 → 1.2.3).
        versionCode = providers.gradleProperty("fitlog.versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("fitlog.versionName").orNull ?: "1.0.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingValue("storeFile")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = signingValue("storePassword")
                keyAlias = signingValue("keyAlias")
                keyPassword = signingValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val release = signingConfigs.getByName("release")
            if (release.storeFile != null) signingConfig = release
        }
    }

    buildFeatures {
        compose = true
    }

    // El OCR de ML Kit pesa ~11 MB por arquitectura: un APK por procesador ARM (los de los móviles
    // reales), con las librerías nativas comprimidas.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
