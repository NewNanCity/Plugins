/**
 * PaperMC插件约定插件
 * 为所有插件子项目提供统一的构建配置
 */

plugins {
    kotlin("jvm")
    java
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
}

// 通用配置
group = "city.newnan"
version = Versions.project

// 仓库配置
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
    maven("https://repo.mikeprimm.com/") {
        name = "dynmap"
    }
}

// 核心依赖 - 所有插件都需要
dependencies {
    compileOnly(Dependencies.Core.paperApi)
    implementation(Dependencies.Core.kotlinStdlib)

    // 测试依赖已移除，不在构建中包含测试
    // testImplementation(Dependencies.Test.junitJupiter)
    // testImplementation(Dependencies.Test.mockk)
}

// Java工具链配置 - 使用JDK21进行编译
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(Versions.developmentJdk))
    }
}

// Kotlin编译配置 - 生成JDK17兼容的字节码
kotlin {
    jvmToolchain(Versions.developmentJdk)

    // 配置编译器参数
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

// Java编译配置 - 确保Java代码也生成JDK17兼容的字节码
tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
}

// 测试配置已禁用，不执行测试
// tasks.test {
//     useJUnitPlatform()
// }

// 明确禁用所有测试任务
tasks.withType<Test> {
    enabled = false
}

// Shadow配置 - 通用配置
tasks.shadowJar {
    // 设置输出文件名（移除-all后缀）
    archiveClassifier.set("")

    // 重定位依赖包避免冲突
    relocate("com.fasterxml.jackson", "${project.group}.libs.jackson")
    relocate("kotlin.reflect", "${project.group}.libs.kotlin.reflect")
    relocate("org.jetbrains.kotlin", "${project.group}.libs.kotlin")
    relocate("com.cronutils", "${project.group}.libs.cronutils")
    relocate("kotlinx.coroutines", "${project.group}.libs.coroutines")

    // 保留必要的 META-INF 服务文件
    mergeServiceFiles()

    // 排除不需要的文件，但保留服务文件
    exclude("META-INF/maven/**")
    exclude("META-INF/versions/**")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    // 不使用minimize以确保kotlin-reflect被包含
    // minimize可能会意外排除kotlin-reflect相关类
}

// 构建配置
tasks.build {
    dependsOn("shadowJar")
}

// 运行服务器配置
tasks.runServer {
    minecraftVersion(Versions.minecraftVersion)
}
