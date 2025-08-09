plugins {
    id("newnancity-plugin")
}

val pluginName = "Guardian"
version = "1.0.4"
group = "city.newnan.guardian"
description = "Guardian for NewNanCity."

// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.GuardianPlugin"
        it.authors = listOf("Sttot", "AI")
        it.prefix = pluginName
        it.load = PluginLoadMode.STARTUP
        it.dependencies = listOf("Vault")
        it.database = true
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}

dependencies {
    // 项目模块
    implementation(project(":core"))
    implementation(project(":modules:config"))
    implementation(project(":modules:gui"))
    implementation(project(":modules:i18n"))

    // Cloud Command Framework
    implementation(Dependencies.Optional.Command.cloudPaper)
    implementation(Dependencies.Optional.Command.cloudMinecraftExtras)
    implementation(Dependencies.Optional.Command.cloudAnnotations)

    // Jackson依赖已通过config模块传递，无需重复声明

    // Vault API for permission
    compileOnly(Dependencies.Optional.ThirdParty.vault)

    // 数据库
    implementation(Dependencies.Optional.Database.hikariCP)
    implementation(Dependencies.Optional.Database.mysql)
    implementation(Dependencies.Optional.Database.exposedCore)
    implementation(Dependencies.Optional.Database.exposedJdbc)
    implementation(Dependencies.Optional.Database.exposedKotlinDatetime)
}

// Shadow配置 - 自定义插件名称
tasks.shadowJar {
    archiveFileName.set("$pluginName-${project.version}.jar")
}

// 运行服务器配置
tasks.runServer {
    jvmArgs("-Xmx2G")
}

// 测试配置
tasks.test {
    useJUnitPlatform()
}