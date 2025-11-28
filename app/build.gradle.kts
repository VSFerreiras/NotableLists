plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("androidx.room") version "2.7.2" apply false
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android") version "2.57.1"
}

android {
    namespace = "ucne.edu.notablelists"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ucne.edu.notablelists"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.compose.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.hilt.work)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.compose.material3:material3:1.5.0-alpha08")
    implementation("androidx.compose.material:material-icons-extended:1.6.4")
    //navegacion

    implementation("androidx.navigation:navigation-compose:2.9.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

//room

    implementation("androidx.room:room-runtime:2.7.2")

    annotationProcessor("androidx.room:room-compiler:2.7.2")

    ksp("androidx.room:room-compiler:2.7.2")

// optional - Kotlin Extensions and Coroutines support for Room

    implementation("androidx.room:room-ktx:2.7.2")

//Hilt

    implementation("com.google.dagger:hilt-android:2.57.1")

    ksp("com.google.dagger:hilt-android-compiler:2.57.1")

    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
//retrofit

    implementation("com.squareup.retrofit2:retrofit:3.0.0")

    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")

    implementation("com.squareup.retrofit2:converter-moshi:3.0.0")

    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")
}