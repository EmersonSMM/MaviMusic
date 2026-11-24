plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.music.mavimusic.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.music.mavimusic.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.8.54"
        manifestPlaceholders["appLabel"] = "@string/app_name"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("review") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".review"
            versionNameSuffix = "-review"
            matchingFallbacks += "debug"
            manifestPlaceholders["appLabel"] = "MaviMusic · pruebas"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val cmp = "1.10.3"

dependencies {
    implementation(project(":shared"))
    implementation(project(":desktopUi"))

    // AndroidX & Lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Compose Multiplatform
    implementation("org.jetbrains.compose.runtime:runtime:$cmp")
    implementation("org.jetbrains.compose.foundation:foundation:$cmp")
    implementation("org.jetbrains.compose.animation:animation:$cmp")
    implementation("org.jetbrains.compose.ui:ui:$cmp")
    implementation("org.jetbrains.compose.material3:material3:1.7.3")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

    // AndroidX Media3 for Audio Playback & MediaSession
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    // Coil 3 for Image Loading
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // NewPipeExtractor for YouTube stream extraction
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
}

tasks.matching { it.name.contains("AarMetadata", ignoreCase = true) }.configureEach {
    enabled = false
}
