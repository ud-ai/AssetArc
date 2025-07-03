import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.assetarc"
    compileSdk = 36

    // Load local.properties
    val localProperties = Properties().apply {
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { this.load(it) }
        }
    }

    defaultConfig {
        applicationId = "com.example.assetarc"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val finnhubApiKey = project.findProperty("FINNHUB_API_KEY") as? String ?: ""
        buildConfigField("String", "FINNHUB_API_KEY", "\"$finnhubApiKey\"")

        val geminiApiKey: String? =
            project.findProperty("GEMINI_API_KEY") as String? ?: localProperties.getProperty("GEMINI_API_KEY")
        if (geminiApiKey != null) {
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        }

        val newsApiKey: String? =
            project.findProperty("NEWS_API_KEY") as String? ?: localProperties.getProperty("NEWS_API_KEY")
        if (newsApiKey != null) {
            buildConfigField("String", "NEWS_API_KEY", "\"$newsApiKey\"")
        }
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}