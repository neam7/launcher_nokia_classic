plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nokia.launcher"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.nokia.launcher"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    // The launcher speaks English and Spanish: drop the libraries' other languages.
    androidResources {
        localeFilters += listOf("en", "es")
    }

    buildTypes {
        release {
            // R8 over the whole app: limiting it to the libraries made the release crash on start.
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}