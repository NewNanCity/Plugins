/**
 * NewNanPlugins 多项目构建配置
 * 这是根项目的构建脚本，主要用于配置子项目的通用设置
 */

plugins {
    kotlin("jvm") apply false
    id("com.gradleup.shadow") apply false
    id("xyz.jpenilla.run-paper") apply false
}

group = "city.newnan"
version = "1.0-SNAPSHOT"

// 为所有子项目配置通用设置
allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo"
        }
        maven("https://oss.sonatype.org/content/groups/public/") {
            name = "sonatype"
        }
        maven("https://maven.enginehub.org/repo/") {
            name = "enginehub"
        }
        maven("https://jitpack.io") {
            name = "jitpack"
        }
    }
}

// 为子项目配置通用设置
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")

    // Java工具链配置 - 使用JDK21进行编译
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(Versions.developmentJdk))
        }
    }

    // Kotlin编译配置 - 生成JDK17兼容的字节码
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(listOf(
                "-Xjsr305=strict",
                "-Xannotation-default-target=param-property"
            ))
        }
    }

    // Java编译配置 - 确保Java代码也生成JDK17兼容的字节码
    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }

    // 测试配置已禁用，不执行测试
    // tasks.withType<Test> {
    //     useJUnitPlatform()
    // }

    // 配置构建任务跳过测试
    tasks.withType<Test> {
        enabled = false
    }
}

// 根项目任务：构建所有插件
tasks.register("buildAllPlugins") {
    group = "build"
    description = "构建所有插件项目"

    dependsOn(subprojects.filter { it.path.startsWith(":plugins:") }.map { "${it.path}:build" })
}

// 根项目任务：清理所有项目
tasks.register("cleanAll") {
    group = "build"
    description = "清理所有项目的构建文件"

    dependsOn(subprojects.map { "${it.path}:clean" })
}

// 根项目任务：构建所有插件的shadowJar
tasks.register("shadowJarAll") {
    group = "shadow"
    description = "构建所有插件项目的shadowJar文件"

    dependsOn(subprojects.filter { it.path.startsWith(":plugins:") }.map { "${it.path}:shadowJar" })
}
