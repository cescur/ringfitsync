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
    namespace = "com.solof.fitlog"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.solof.fitlog"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
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

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
