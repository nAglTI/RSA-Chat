import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.detekt)
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlinx.atomicfu") version "0.27.0"
    id("com.ncorti.ktfmt.gradle") version "0.21.0"
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true

            // Push notifs
            export(libs.kmpnotifier)

            // Decompose
            export(libs.decompose)
            export(libs.lifecycle)

            // Optional, only if you need state preservation on Darwin (Apple) targets
            export(libs.state.keeper)
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer =
                    (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                        static =
                            (static ?: mutableListOf()).apply {
                                // Serve sources to debug inside browser
                                add(rootDirPath)
                                add(projectDirPath)
                            }
                    }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // Pinch to zoom
            implementation(libs.lib.zoomable)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            // Needed for splash screen
            implementation(libs.androidx.material)
            // HTTP and coroutine impl
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            // Permissions
            implementation(libs.accompanist.permissions)
            // Please don't kill my resources folder
            implementation(libs.androidx.startup.runtime)
            // Pinch to zoom
            implementation(libs.lib.zoomable)
        }
        commonMain.dependencies {
            implementation(kotlin("reflect"))
            // Atomics
            implementation(libs.atomicfu)
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.adaptive)
            implementation(libs.adaptive.layout)
            implementation(libs.adaptive.navigation)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // Settings Handling
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            // JSON Serialization
            implementation(libs.kotlinx.serialization.json)
            // ktor (HTTP & WebSockets)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            // Async Image Loading from URL
            // TODO: Add GIF support when it is merged: https://github.com/coil-kt/coil/pull/2594
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.coil.svg)
            implementation(libs.coil.network.cache.control)
            // Markdown Rendering
            implementation(libs.multiplatform.markdown.renderer)
            implementation(libs.multiplatform.markdown.renderer.m3)
            implementation(libs.multiplatform.markdown.renderer.coil3)
            implementation(libs.multiplatform.markdown.renderer.code)
            // Base64
            implementation(libs.ktor.utils)
            // Logging
            implementation(libs.kermit)
            // Datetime
            implementation(libs.kotlinx.datetime)
            // File picker
            implementation(libs.filekit.core)
            implementation(libs.filekit.compose)
            // Material You Color scheme generation on other platforms
            implementation(libs.material.kolor)
            // Note: The following dependencies are declared as api() to work with iOS
            // Navigation
            api(libs.decompose)
            api(libs.decompose.extensions.compose)
            // Utils for navigation library
            api(libs.lifecycle)
            api(libs.lifecycle.coroutines)
            api(libs.state.keeper)
            api(libs.instance.keeper)
            api(libs.back.handler)
            // Notifications
            api(libs.kmpnotifier)
        }
        commonTest.dependencies { implementation(kotlin("test")) }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            /*implementation(compose.desktop.windows_x64)
            implementation(compose.desktop.linux_x64)
            implementation(compose.desktop.macos_arm64)*/
            // HTTP and coroutine impl
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.swing)
            // SLF4J for use with Kermit
            implementation(libs.slf4j.api)
            implementation(libs.slf4j.simple)
            // Stdlib extensions
            implementation(libs.commons.lang3)
            // dbus
            implementation(libs.dbus.java.core)
            // JNA for native OS calls
            implementation(libs.jna.platform)
        }
    }
}

android {
    namespace = "com.hypergonial.chat"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].res.srcDirs("src/commonMain/composeResources")
    sourceSets["main"].resources.srcDirs("src/commonMain/composeResources")

    defaultConfig {
        applicationId = "com.hypergonial.chat"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            multiDexKeepProguard = file("multidex-config.pro")

            /* TODO: Change when actually releasing */
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies { implementation(libs.androidx.foundation.android)
    debugImplementation(compose.uiTooling) }

compose.desktop {
    application {
        mainClass = "com.hypergonial.chat.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "com.hypergonial.chat"
            packageVersion = "1.0.0"

            // Needed by filekit
            linux { modules("jdk.security.auth") }
        }

        buildTypes.release.proguard {
            version.set("7.6.1")
            configurationFiles.from("proguard-desktop-rules.pro")
            joinOutputJars = true
            optimize = true
            // Causes issues with dbus if enabled
            obfuscate = false
        }
    }
}

ktfmt {
    // Breaks lines longer than maxWidth. Default 100.
    maxWidth.set(120)
    // blockIndent is the indent size used when a new block is opened, in spaces.
    blockIndent.set(4)
    // continuationIndent is the indent size used when a line is broken because it's too
    continuationIndent.set(4)
    // Whether ktfmt should remove imports that are not used.
    removeUnusedImports.set(true)
    // Whether ktfmt should automatically add/remove trailing commas.
    manageTrailingCommas.set(true)
}

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    allRules = false // activate all available (even unstable) rules.
    config.setFrom("$projectDir/detekt.yml")
    source.setFrom(
        "src/commonMain/kotlin",
        "src/commonTest/kotlin",
        "src/androidMain/kotlin",
        "src/desktopMain/kotlin",
        "src/iosMain/kotlin",
        "src/wasmJsMain/kotlin",
    )
    basePath = projectDir.absolutePath
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

tasks.withType<Detekt>().configureEach { jvmTarget = "21" }

tasks.withType<DetektCreateBaselineTask>().configureEach { jvmTarget = "21" }
