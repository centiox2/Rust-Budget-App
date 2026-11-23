import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
}
kotlin {
    androidLibrary {
        namespace = "com.centiox.verdant.shared"
        compileSdk = 36
        minSdk = 26
        androidResources.enable = true
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    jvm("desktop") { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        val desktopTest by getting {
            dependencies {
                implementation("org.jetbrains.compose.ui:ui-test:1.10.3")
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
compose.resources { packageOfResClass = "com.centiox.verdant.resources"; publicResClass = true }
tasks.withType<Test>().configureEach {
    systemProperty("java.awt.headless", "true")
    systemProperty("skiko.renderApi", "SOFTWARE")
    val screenshots = rootProject.layout.buildDirectory.dir("ui-screenshots").get().asFile
    systemProperty("verdant.screenshots", screenshots.absolutePath)
    outputs.dir(screenshots)
}
