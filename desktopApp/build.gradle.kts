plugins { kotlin("jvm"); kotlin("plugin.compose"); id("org.jetbrains.compose") }
kotlin { jvmToolchain(17) }
dependencies { implementation(project(":shared")); implementation(compose.desktop.currentOs); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2") }
compose.desktop { application { mainClass = "com.centiox.verdant.desktop.MainKt" } }
