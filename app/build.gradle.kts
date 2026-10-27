import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// La clef de signature vit hors du depot : %USERPROFILE%\.unicorn-radio\signature.properties
// (storeFile, storePassword, keyAlias, keyPassword). Sans elle, la release n'est pas signee.
val signature = Properties().apply {
    val f = file(System.getProperty("user.home")).resolve(".unicorn-radio/signature.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "org.unicornradio.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.unicornradio.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "0.9.2"
    }

    signingConfigs {
        if (signature.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(signature.getProperty("storeFile"))
                storePassword = signature.getProperty("storePassword")
                keyAlias = signature.getProperty("keyAlias")
                keyPassword = signature.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        buildConfig = true
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)
    implementation(libs.coil)
    implementation(libs.zxing)
}
