plugins {
    id("com.mikepenz.aboutlibraries.plugin") version "13.2.1" apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.lsplugin.apksign) apply false
    alias(libs.plugins.lsplugin.apktransform) apply false
}

subprojects {
    apply(plugin = "com.mikepenz.aboutlibraries.plugin")
    extensions.configure<com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension> {
        collect {
            all.set(true)
            filterVariants.addAll("RuntimeClasspath", "CompileKlibraries", "coreLibraryDesugaring")
        }
    }
    tasks.register("exportLicenseArtifacts") {
        notCompatibleWithConfigurationCache("许可导出在执行阶段读取实际解析的依赖产物")
        val artifactReport = layout.buildDirectory.file("reports/licenses/artifacts.json")
        outputs.file(artifactReport)
        outputs.upToDateWhen { false }
        doLast {
            val artifacts = configurations.filter {
                it.isCanBeResolved && !it.name.contains("test", ignoreCase = true) &&
                    (it.name.endsWith("RuntimeClasspath") || it.name == "coreLibraryDesugaring")
            }.flatMap {
                it.incoming.artifactView {
                    componentFilter { component ->
                        component is org.gradle.api.artifacts.component.ModuleComponentIdentifier
                    }
                }.artifacts.artifacts
            }.map {
                val component = it.id.componentIdentifier as org.gradle.api.artifacts.component.ModuleComponentIdentifier
                mapOf(
                    "id" to "${component.group}:${component.module}",
                    "version" to component.version,
                    "path" to it.file.absolutePath,
                )
            }.distinct()
            artifactReport.get().asFile.apply {
                parentFile.mkdirs()
                writeText(groovy.json.JsonOutput.toJson(artifacts))
            }
        }
    }
    configurations.configureEach {
        // MMKV 的源码构建只服务 Desktop；其他配置按声明版本从 Maven 解析。
        val usesMmkvSourceBuild =
            project.path == ":desktopApp" ||
                (project.path == ":shared" && name.contains("desktop", ignoreCase = true))
        if (!usesMmkvSourceBuild) {
            resolutionStrategy.useGlobalDependencySubstitutionRules = false
        }
    }
}

tasks.register<Exec>("updateOpenSourceLicenses") {
    dependsOn(subprojects.flatMap {
        listOf("${it.path}:exportLibraryDefinitions", "${it.path}:exportLicenseArtifacts")
    })
    commandLine("python3", "scripts/update_open_source_licenses.py")
}
