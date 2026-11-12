plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// نسخه‌ی محلی نیز باید از آخرین کد منتشرشده پایین‌تر نباشد.
// CI همچنان versionName و versionCode را از ورودی build تزریق می‌کند.
val appVersionName: String = (findProperty("versionName") as String?) ?: "1.2"
val appVersionCode: Int = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1027

android {
    namespace = "com.flow.focus"
    compileSdk = 35

    defaultConfig {
        // عمداً ثابت نگه داشته شده تا نصب نسخه‌ی جدید روی FLOW فعلی ممکن باشد.
        applicationId = "com.flow.focus"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    // اطلاعات امضا فقط از متغیرهای محیطی (GitHub Secrets) خوانده می‌شود.
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
            // بدون کلید انتشار، خروجی فقط برای تست است و نباید به‌عنوان Update منتشر شود.
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
        // Lint is part of release verification; do not silently ship critical findings.
        abortOnError = true
        checkReleaseBuilds = true
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

    // ExoPlayer remains for the separate ambient sound feature.
    implementation("androidx.media3:media3-exoplayer:1.5.1")

    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.10.5")

    testImplementation("junit:junit:4.13.2")
}
