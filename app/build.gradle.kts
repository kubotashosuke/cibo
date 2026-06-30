import java.util.Properties

val properties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.gourmetsearch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gourmetsearch"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "HOTPEPPER_API_KEY", "\"${properties.getProperty("HOTPEPPER_API_KEY") ?: ""}\"")
            buildConfigField("String", "MAPTILER_API_KEY", "\"${properties.getProperty("MAPTILER_API_KEY") ?: ""}\"")
        }
        release {
            buildConfigField("String", "HOTPEPPER_API_KEY", "\"${properties.getProperty("HOTPEPPER_API_KEY") ?: ""}\"")
            buildConfigField("String", "MAPTILER_API_KEY", "\"${properties.getProperty("MAPTILER_API_KEY") ?: ""}\"")
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("io.coil-kt:coil:2.6.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("org.maplibre.gl:android-sdk:11.0.0")
    implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.2")

    implementation("com.google.android.material:material:1.12.0")
}