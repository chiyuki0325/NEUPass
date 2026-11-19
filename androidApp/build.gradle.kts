plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.lsplugin.apktransform)
}

fun gitOutput(vararg args: String) = providers.exec {
    commandLine("git", *args)
}.standardOutput.asText.map { it.trim() }

val versionCodeFromGit = gitOutput("rev-list", "--count", "HEAD").map(String::toInt).get()
val versionNameFromGit = gitOutput("describe", "--tags", "--always", "--exclude", "beta").get()

apksign {
    storeFileProperty = "releaseStoreFile"
    storePasswordProperty = "releaseStorePassword"
    keyAliasProperty = "releaseKeyAlias"
    keyPasswordProperty = "releaseKeyPassword"
}

apktransform {
    copy {
        when (it.buildType) {
            "release" -> file("${it.name}/ink.chyk.pass.$versionNameFromGit-kmp.apk")
            "debug" -> file("${it.name}/ink.chyk.pass.$versionNameFromGit-kmp-debug.apk")
            else -> null
        }
    }
}

android {
    namespace = "ink.chyk.pass.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ink.chyk.pass"
        minSdk = 28
        targetSdk = 28
        versionCode = versionCodeFromGit
        versionName = versionNameFromGit
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        dex {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions.jvmTarget = "17"

    lint {
        disable += "ExpiredTargetSdkVersion"
    }
}

dependencies {
    implementation(project(":shared"))
    coreLibraryDesugaring(libs.desugar)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
