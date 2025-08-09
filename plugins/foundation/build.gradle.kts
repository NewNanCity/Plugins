plugins {
    id("newnancity-plugin")
}

val pluginName = "Foundation"
version = "2.0.0"
group = "city.newnan.foundation"
description = "Modern foundation plugin for NewNan city economic management"

// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.FoundationPlugin"
        it.authors = listOf("Sttot", "NSrank")
        it.prefix = pluginName
        it.load = PluginLoadMode.POSTWORLD
        it.dependencies = listOf("Vault")
        it.softDependencies = listOf("Essentials", "XConomy", "EssentialsX")
        it.database = true
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}

// 添加 EssentialsX 和 XConomy 仓库
repositories {
    maven("https://repo.essentialsx.net/releases/") {
        name = "essentialsx-releases"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    // 项目模块
    implementation(project(":core"))
    implementation(project(":modules:config"))
    implementation(project(":modules:i18n"))
    implementation(project(":modules:gui"))

    // Paper API
    compileOnly(Dependencies.Core.paperApi)

    // Cloud 命令框架
    implementation(Dependencies.Optional.Command.cloudPaper)
    implementation(Dependencies.Optional.Command.cloudMinecraftExtras)
    implementation(Dependencies.Optional.Command.cloudAnnotations)

    // Vault API for economy
    compileOnly(Dependencies.Optional.ThirdParty.vault)

    // EssentialsX API for transfer detection
    compileOnly(Dependencies.Optional.ThirdParty.essentialsX)

    // XConomy API for transfer detection
    compileOnly(Dependencies.Optional.ThirdParty.xConomy)

    // Jackson CSV support for data processing (额外格式支持)
    implementation(Dependencies.Optional.Config.jacksonCsv)

    // 数据库相关依赖
    implementation(Dependencies.Optional.Database.exposedCore)
    implementation(Dependencies.Optional.Database.exposedJdbc)
    implementation(Dependencies.Optional.Database.exposedKotlinDatetime)
    implementation(Dependencies.Optional.Database.hikariCP)
    implementation(Dependencies.Optional.Database.mysql)

    // Jackson核心依赖已通过config模块传递，无需重复声明
}

// Shadow配置 - 自定义插件名称
tasks.shadowJar {
    archiveFileName.set("$pluginName-${project.version}.jar")
}

// 运行服务器配置
tasks.runServer {
    jvmArgs("-Xmx2G")
}
