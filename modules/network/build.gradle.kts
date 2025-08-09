plugins {
    kotlin("jvm")
    java
    kotlin("plugin.serialization") version "2.2.0"
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
    // 依赖核心模块
    api(project(":core"))

    // PaperMC API
    compileOnly(Dependencies.Core.paperApi)
    implementation(Dependencies.Core.kotlinStdlib)

    // 协程支持
    implementation(Dependencies.Optional.Async.kotlinCoroutines)
    implementation(Dependencies.Optional.Async.kotlinCoroutinesJdk8)

    // Ktor Client 核心依赖
    implementation(Dependencies.Optional.Network.ktor)
    implementation(Dependencies.Optional.Network.ktorOkHttp) // OkHttp 引擎
    implementation(Dependencies.Optional.Network.ktorContentNegotiation)
    implementation(Dependencies.Optional.Network.ktorLogging)
    implementation(Dependencies.Optional.Network.ktorAuth)
    implementation(Dependencies.Optional.Network.ktorResources)

    // 序列化支持
    implementation(Dependencies.Optional.Network.ktorSerializationJson)
    implementation(Dependencies.Optional.Network.ktorSerializationJackson)
    implementation(Dependencies.Optional.Network.kotlinxSerializationJson)

    // Jackson 支持 (与config模块兼容)
    implementation(Dependencies.Optional.Config.jacksonDatabind)
    implementation(Dependencies.Optional.Config.jacksonKotlin)

    // 测试依赖
    testImplementation(Dependencies.Test.junitJupiter)
    testImplementation(Dependencies.Test.mockk)
    testImplementation(Dependencies.Optional.Network.ktorMock)
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
