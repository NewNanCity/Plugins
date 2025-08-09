plugins {
    id("newnancity-plugin")
}

val pluginName = "RailExpress"
version = "1.0.0"
group = "city.newnan.railexpress"
description = "Allows minecarts to have different maximum speeds on different blocks"


// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.RailExpressPlugin"
        it.authors = listOf("Sttot", "AI")
        it.prefix = pluginName
        it.dependencies = listOf()
    }

    inputs.properties(pluginProperties)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }

    filesMatching("config.yml") {
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

    // Cloud Command Framework
    implementation(Dependencies.Optional.Command.cloudPaper)
    implementation(Dependencies.Optional.Command.cloudMinecraftExtras)
    implementation(Dependencies.Optional.Command.cloudAnnotations)

    // 测试依赖
    testImplementation(Dependencies.Test.junitJupiter)
    testImplementation(Dependencies.Test.mockito)
    testImplementation(Dependencies.Test.mockitoKotlin)
}

// Shadow配置 - 自定义插件名称
tasks.shadowJar {
    archiveFileName.set("$pluginName-${project.version}.jar")
}

// 运行服务器配置
tasks.runServer {
    jvmArgs("-Xmx2G")
}