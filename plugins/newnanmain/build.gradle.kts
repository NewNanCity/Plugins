plugins {
    id("newnancity-plugin")
}

val pluginName = "NewNanMain"
version = "2.0.0"
group = "city.newnan.newnanmain"
description = "NewNan City main plugin with prefix management and teleport system"

// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.NewNanMainPlugin"
        it.authors = listOf("NewNanCity", "AI")
        it.prefix = pluginName
        it.load = PluginLoadMode.POSTWORLD
        it.dependencies = listOf() // 移除CommandAPI依赖
        it.softDependencies = listOf("Vault") // 软依赖Vault
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

    // 其他插件
    compileOnly(project(":plugins:feefly"))

    // Paper API
    compileOnly(Dependencies.Core.paperApi)

    // Cloud命令框架
    implementation(Dependencies.Optional.Command.cloudPaper)
    implementation(Dependencies.Optional.Command.cloudMinecraftExtras)
    implementation(Dependencies.Optional.Command.cloudAnnotations)

    // 软依赖
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
