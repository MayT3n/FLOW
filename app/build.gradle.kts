plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// نسخه از خارج Gradle تزریق می‌شود: -PversionName=1.2.3 -PversionCode=42
val appVersionName: String = (findProperty("versionName") as String?) ?: "1.0.0"
val appVersionCode: Int = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.flow.focus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.flow.focus"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    // اطلاعات امضا فقط از متغیرهای محیطی (GitHub Secrets) خوانده می‌شود؛ هیچ رازی در مخزن نیست.
    val keystorePath: String? = System.getenv("FLOW_KEYSTORE_PATH")
    val hasReleaseKeystore = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("FLOW_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("FLOW_KEY_ALIAS")
                keyPassword = System.getenv("FLOW_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // بدون Keystore: با کلید debug امضا می‌شود تا APK قابل نصب برای تست باشد.
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
}
