import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val mmkvKotlinVersion = providers.gradleProperty("mmkvKotlinVersion").get()

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    id("com.tencent.kuiklybase.resource.generator") version "0.0.1-neupass.21"
}

val isAppleHost =
    System.getProperty("os.name").lowercase().let {
        it.contains("mac") || it.contains("darwin")
    }

kotlin {
    androidTarget {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    jvm("desktop")
    if (isAppleHost) {
        iosArm64()
        iosSimulatorArm64()
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.2.0-alpha03")
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.backhandler)
                implementation(libs.serialization.json)
                implementation(libs.datetime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.core)
                implementation(libs.jetbrains.lifecycle.viewmodel.compose)
                implementation(libs.jetbrains.lifecycle.runtime.compose)
                implementation(libs.navigation3.runtime)
                implementation(libs.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.reorderable)
                implementation(libs.pangu)
                implementation(libs.material.kolor)
                implementation("com.tencent.kuiklybase:resource-compose:0.0.1-neupass.21")
                implementation("com.ctrip.flight.mmkv:mmkv-kotlin:$mmkvKotlinVersion")
            }
        }
        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.zxing)
            }
        }
        val androidMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.paging)
                implementation(libs.paging.compose)
                implementation(libs.browser)
                implementation(libs.tbs)
            }
        }
        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.compose.desktop)
                implementation(libs.native.webview)
                implementation(libs.native.webview.runtime)
                implementation(libs.kotlinx.coroutines.swing)
                implementation("org.slf4j:slf4j-nop:1.7.36")
            }
        }
        if (isAppleHost) {
            val iosMain by creating {
                dependsOn(commonMain)
                dependencies {
                    implementation(libs.native.webview)
                    implementation(libs.ktor.client.darwin)
                }
            }
            val iosArm64Main by getting { dependsOn(iosMain) }
            val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
            }
        }
    }
}

android {
    namespace = "ink.chyk.pass"
    compileSdk = 36
    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures.compose = true
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
    debugImplementation(libs.compose.ui.tooling)
}

multiplatformResources {
    multiplatformResourcesPackage = "ink.chyk.pass.resources"
    multiplatformResourcesPrefix = "neu_"
    multiplatformResourcesClassName = "MR"
}

if (isAppleHost) {
    val xcodeConfiguration = providers.environmentVariable("CONFIGURATION").orElse("Debug")
    val xcodePlatformName = providers.environmentVariable("PLATFORM_NAME").orElse("iphonesimulator")
    val xcodeSdkName = providers.environmentVariable("SDK_NAME").orElse("iphonesimulator")
    val mmkvAppleTargetName =
        if (xcodePlatformName.get() == "iphoneos") {
            "ios"
        } else {
            "iosSimulator"
        }
    val podBuildTask =
        if (xcodePlatformName.get() == "iphoneos") {
            ":mmkv-kotlin:podBuildMMKVIos"
        } else {
            ":mmkv-kotlin:podBuildMMKVIosSimulator"
        }
    val mmkvBuildDirectory =
        rootProject.layout.projectDirectory.dir(
            ".gradle/vcs/mmkv-kotlin-$mmkvKotlinVersion/mmkv-kotlin/build",
        )
    val mmkvPodBuildSettings =
        mmkvBuildDirectory.file(
            "cocoapods/buildSettings/build-settings-$mmkvAppleTargetName-MMKV.properties",
        )
    val mmkvPodBuildDirectory = providers.provider {
        val properties = Properties()
        mmkvPodBuildSettings.asFile.inputStream().use(properties::load)
        val mmkvConfigurationBuildDirectory =
            properties.getProperty("CONFIGURATION_BUILD_DIR")
                ?: error("MMKV CocoaPods build settings do not contain CONFIGURATION_BUILD_DIR")
        file(mmkvConfigurationBuildDirectory).parentFile
    }

    tasks.register<Copy>("prepareMmkvFrameworkForXcode") {
        dependsOn(gradle.includedBuild("mmkv-kotlin").task(podBuildTask))
        from(mmkvPodBuildDirectory.map { it.resolve("MMKV/MMKV.framework") }) {
            into("MMKV.framework")
        }
        from(mmkvPodBuildDirectory.map { it.resolve("MMKVCore/MMKVCore.framework") }) {
            into("MMKVCore.framework")
        }
        into(
            layout.buildDirectory.dir(
                xcodeConfiguration.zip(xcodeSdkName) { configuration, sdkName ->
                    "xcode-frameworks/$configuration/$sdkName"
                },
            ),
        )
    }
}
