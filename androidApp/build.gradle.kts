plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}
android {
    namespace = "com.centiox.verdant"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.centiox.verdant"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "2.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    signingConfigs {
        create("production") {
            storeFile = file(System.getenv("VERDANT_SIGNING_FILE") ?: "${rootDir}/.signing/release.p12")
            storeType = "PKCS12"
            storePassword = System.getenv("VERDANT_SIGNING_PASSWORD")
            keyAlias = "verdant"
            keyPassword = System.getenv("VERDANT_SIGNING_PASSWORD")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("production")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.core:core-ktx:1.17.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
