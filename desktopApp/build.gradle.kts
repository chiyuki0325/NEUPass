plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

val mmkvKotlinVersion = providers.gradleProperty("mmkvKotlinVersion").get()

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.desktop.currentOs)
            implementation("com.ctrip.flight.mmkv:mmkv-kotlin:$mmkvKotlinVersion")
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
    }
}

tasks.withType<JavaExec>().matching { it.name == "run" }.configureEach {
    systemProperty("ink.chyk.pass.debug", "true")
}
