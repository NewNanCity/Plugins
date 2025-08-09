plugins {
    kotlin("jvm")
    java
}

group = "city.newnan"
version = Versions.project

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    compileOnly(Dependencies.Core.paperApi)
    implementation(Dependencies.Core.kotlinStdlib)
    implementation(Dependencies.Core.kotlinReflect)

    // CommandAPI 支持 - BaseCommandRegistry 需要
    compileOnly(Dependencies.Optional.Command.commandapi)
    compileOnly(Dependencies.Optional.Command.commandapiKotlin)

    // Minecraft 相关依赖（从utils模块合并）
    compileOnly(Dependencies.Optional.ThirdParty.authlib) // Mojang AuthLib for GameProfile

    // 测试依赖
    testImplementation(Dependencies.Test.junitJupiter)
    testImplementation(Dependencies.Test.mockk)
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

tasks.test {
    useJUnitPlatform()
}
