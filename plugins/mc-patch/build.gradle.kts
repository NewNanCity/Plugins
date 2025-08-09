plugins {
    id("newnancity-plugin")
}

val pluginName = "MCPatch"
version = "2.0.0"
group = "city.newnan.mcpatch"
description = "Comprehensive Minecraft server security and stability protection plugin"

// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.MCPatchPlugin"
        it.authors = listOf("NewNanCity", "AI")
        it.prefix = pluginName
        it.load = PluginLoadMode.POSTWORLD
        it.dependencies = listOf()
        it.database = false
    }

    inputs.properties(pluginProperties.toMap())
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(pluginProperties.toMap())
    }
}

dependencies {
    // 项目模块
    implementation(project(":core"))
    implementation(project(":modules:config"))
    implementation(project(":modules:i18n"))

    // Paper API
    compileOnly(Dependencies.Core.paperApi)

    // Google Guava (for ByteStreams)
    implementation(Dependencies.Optional.Utils.guava)
}
