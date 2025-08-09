plugins {
    id("newnancity-plugin")
}

val pluginName = "ExternalBook"
version = "1.0.0"
group = "city.newnan.externalbook"
description = "Modern external book management plugin for NewNanCity"

// 配置插件元数据
tasks.processResources {
    val pluginProperties = createPluginMetadata {
        it.name = pluginName
        it.mainClass = "${project.group}.ExternalBookPlugin"
        it.authors = listOf("Sttot", "AI")
        it.prefix = pluginName
        it.load = PluginLoadMode.POSTWORLD
        it.dependencies = listOf()
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

    // Paper API
    compileOnly(Dependencies.Core.paperApi)

    // Cloud 命令框架
    implementation(Dependencies.Optional.Command.cloudPaper)
    implementation(Dependencies.Optional.Command.cloudMinecraftExtras)
    implementation(Dependencies.Optional.Command.cloudAnnotations)

    // Jackson依赖已通过config模块传递，无需重复声明

    // 测试依赖
    testImplementation(Dependencies.Test.junitJupiter)
    testImplementation(Dependencies.Test.junitJupiterApi)
    testRuntimeOnly(Dependencies.Test.junitJupiterEngine)

    // 数据库
    implementation(Dependencies.Optional.Database.hikariCP)
    implementation(Dependencies.Optional.Database.mysql)
    implementation(Dependencies.Optional.Database.sqlite)
    implementation(Dependencies.Optional.Database.postgresql)
    implementation(Dependencies.Optional.Database.exposedCore)
    implementation(Dependencies.Optional.Database.exposedJdbc)
    implementation(Dependencies.Optional.Database.exposedJson)
    implementation(Dependencies.Optional.Database.exposedKotlinDatetime)

    // 需要ULID
    implementation(Dependencies.Optional.Utils.ulid)
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