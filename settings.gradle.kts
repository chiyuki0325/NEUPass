pluginManagement {
    // 本地开发 KmmResource 时可恢复这段 composite build。
    /*
    val kmmResourceVersion = "0.0.1-neupass.21"
    val kmmResourceDir = settings.layout.rootDirectory.dir(".gradle/vcs/KmmResource-$kmmResourceVersion").asFile
    if (!kmmResourceDir.resolve("settings.gradle.kts").exists()) {
        settings.providers.exec {
            commandLine(
                "git", "clone", "--depth", "1", "--branch", kmmResourceVersion,
                "https://github.com/chiyuki0325/KmmResource.git",
                kmmResourceDir.absolutePath,
            )
        }.result.get()
    }
    includeBuild(kmmResourceDir)
    */
    repositories {
        gradlePluginPortal()
        maven("https://artifacts.chyk.ink/maven")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://artifacts.chyk.ink/maven")
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
    }
}

rootProject.name = "NEUPass"
include(":androidApp", ":shared", ":desktopApp")

val rootLocalProperties = layout.rootDirectory.file("local.properties").asFile

// 本地开发 KmmResource 时可恢复这段 composite build。
/*
val kmmResourceVersion = "0.0.1-neupass.21"
val kmmResourceDir = layout.rootDirectory.dir(".gradle/vcs/KmmResource-$kmmResourceVersion").asFile
if (rootLocalProperties.exists()) {
    rootLocalProperties.copyTo(kmmResourceDir.resolve("local.properties"), overwrite = true)
}
includeBuild(kmmResourceDir) {
    dependencySubstitution {
        substitute(module("com.tencent.kuiklybase:resource-core")).using(project(":resource-core"))
        substitute(module("com.tencent.kuiklybase:resource-compose")).using(project(":resource-compose"))
    }
}
*/

// Desktop 从固定 tag 构建 MMKV；iOS 还会从同一源码构建并打包原生 framework。
// 其他依赖配置在根构建脚本中关闭 composite substitution，改用 Maven。
val mmkvKotlinVersion = providers.gradleProperty("mmkvKotlinVersion").get()
val mmkvKotlinDir = layout.rootDirectory.dir(".gradle/vcs/mmkv-kotlin-$mmkvKotlinVersion").asFile
if (!mmkvKotlinDir.resolve("settings.gradle.kts").exists()) {
    providers.exec {
        commandLine(
            "git", "clone", "--depth", "1", "--branch", mmkvKotlinVersion,
            "https://github.com/chiyuki0325/mmkv-kotlin.git",
            mmkvKotlinDir.absolutePath,
        )
    }.result.get()
}
if (rootLocalProperties.exists()) {
    rootLocalProperties.copyTo(mmkvKotlinDir.resolve("local.properties"), overwrite = true)
}
includeBuild(mmkvKotlinDir) {
    name = "mmkv-kotlin"
    dependencySubstitution {
        substitute(module("com.ctrip.flight.mmkv:mmkv-kotlin")).using(project(":mmkv-kotlin"))
    }
}
