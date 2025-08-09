plugins {
    id("newnancity-plugin")
}

val pluginName = "TPA"
version = "2.0.0"
group = "city.newnan.tpa"
description = "Modern TPA (teleport request) plugin with GUI support"


// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.TPAPlugin"
        it.authors = listOf("Sttot", "AI")
        it.prefix = pluginName
        it.load = PluginLoadMode.POSTWORLD
        it.dependencies = listOf() // 移除CommandAPI依赖
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

    // Paper API
    compileOnly(Dependencies.Core.paperApi)

    // Cloud命令框架
    implementation(Dependencies.Optional.Command.cloudPaper)
    implementation(Dependencies.Optional.Command.cloudMinecraftExtras)
    implementation(Dependencies.Optional.Command.cloudAnnotations)

    // Jackson依赖已通过config模块传递，无需重复声明
}

// Shadow配置 - 自定义插件名称
tasks.shadowJar {
    archiveFileName.set("$pluginName-${project.version}.jar")
}

// 运行服务器配置
tasks.runServer {
    jvmArgs("-Xmx2G")
}
