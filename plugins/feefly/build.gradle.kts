plugins {
    id("newnancity-plugin")
}

val pluginName = "FeeFly"
version = "2.0.0"
group = "city.newnan.feefly"
description = "Modern fee-based flying plugin with economy integration"

// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.FeeFlyPlugin"
        it.description = "Modern fee-based flying plugin with economy integration"
        it.authors = listOf("Sttot", "AI")
        it.prefix = pluginName
        it.load = PluginLoadMode.STARTUP
        it.dependencies = listOf("Vault")
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

    // Cloud 命令框架
    implementation(Dependencies.Optional.Command.cloudPaper)
    implementation(Dependencies.Optional.Command.cloudMinecraftExtras)
    implementation(Dependencies.Optional.Command.cloudAnnotations)

    // Vault API for economy
    compileOnly(Dependencies.Optional.ThirdParty.vault)

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
