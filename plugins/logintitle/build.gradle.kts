plugins {
    id("newnancity-plugin")
}

val pluginName = "LoginTitle"
version = "1.0.0"
group = "city.newnan.logintitle"
description = "Simple plugin that shows title to players on login"

// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.LoginTitlePlugin"
        it.authors = listOf("AI")
        it.prefix = pluginName
        it.load = PluginLoadMode.STARTUP
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}

dependencies {
    // 项目模块 - 只依赖core模块
    implementation(project(":core"))

    // Paper API
    compileOnly(Dependencies.Core.paperApi)
}

// Shadow配置 - 自定义插件名称
tasks.shadowJar {
    archiveFileName.set("$pluginName-${project.version}.jar")
}

// 运行服务器配置
tasks.runServer {
    jvmArgs("-Xmx2G")
}
