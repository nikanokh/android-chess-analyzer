plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.nikanokh.chessinsightanalyzermobile"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.nikanokh.chessinsightanalyzermobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "4.0.0-compose"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val storeFileProp = (project.findProperty("CHESS_UPLOAD_STORE_FILE") as String?)
                ?: System.getenv("CHESS_UPLOAD_STORE_FILE")
            val storePassProp = (project.findProperty("CHESS_UPLOAD_STORE_PASSWORD") as String?)
                ?: System.getenv("CHESS_UPLOAD_STORE_PASSWORD")
            val keyAliasProp = (project.findProperty("CHESS_UPLOAD_KEY_ALIAS") as String?)
                ?: System.getenv("CHESS_UPLOAD_KEY_ALIAS")
            val keyPassProp = (project.findProperty("CHESS_UPLOAD_KEY_PASSWORD") as String?)
                ?: System.getenv("CHESS_UPLOAD_KEY_PASSWORD")

            if (!storeFileProp.isNullOrBlank() &&
                !storePassProp.isNullOrBlank() &&
                !keyAliasProp.isNullOrBlank() &&
                !keyPassProp.isNullOrBlank()
            ) {
                storeFile = file(storeFileProp)
                storePassword = storePassProp
                keyAlias = keyAliasProp
                keyPassword = keyPassProp
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.findByName("release")
            signingConfig = if (releaseSigning?.storeFile != null) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
