plugins {
    id("newnancity-plugin")
}

val pluginName = "MCron"
version = "2.0.0"
group = "city.newnan.mcron"
description = "A modern Minecraft scheduling plugin supporting cron expressions and multiple scheduling modes"


// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.MCronPlugin"
        it.authors = listOf("Sttot")
        it.prefix = pluginName
        it.load = PluginLoadMode.STARTUP
        it.dependencies = listOf() // Cloud框架不需要软依赖
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}

dependencies {
    // 项目模块依赖
    implementation(project(":core"))
    implementation(project(":modules:config"))
    implementation(project(":modules:i18n"))

    // Paper API
    compileOnly(Dependencies.Core.paperApi)

    // Cloud框架
    implementation(Dependencies.Optional.Command.cloudPaper)
    implementation(Dependencies.Optional.Command.cloudMinecraftExtras)
    implementation(Dependencies.Optional.Command.cloudAnnotations)

    // Cron工具库
    implementation(Dependencies.Optional.Utils.cronUtils)

    // 协程支持
    implementation(Dependencies.Optional.Async.kotlinCoroutines)

    // Jackson依赖已通过config模块传递，无需重复声明

    // 测试依赖
    testImplementation(Dependencies.Test.junitJupiter)
    testImplementation(Dependencies.Test.mockito)
    testImplementation(Dependencies.Test.mockitoKotlin)
    testImplementation(Dependencies.Core.paperApi)

    // 协程测试依赖
    testImplementation(Dependencies.Optional.Async.kotlinCoroutinesTest)

    // MockK依赖
    testImplementation(Dependencies.Test.mockk)
}

// Shadow配置 - 自定义插件名称和额外重定位
tasks.shadowJar {
    archiveFileName.set("$pluginName-${project.version}.jar")
}

// 运行服务器配置
tasks.runServer {
    jvmArgs("-Xmx2G")
}
