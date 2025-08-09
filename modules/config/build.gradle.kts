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
    // 依赖核心模块（包含kotlin-reflect）
    api(project(":core"))

    // PaperMC API
    compileOnly(Dependencies.Core.paperApi)
    implementation(Dependencies.Core.kotlinStdlib)

    // Jackson 核心依赖（必需）- 使用api以便插件可以访问Jackson注解
    api(Dependencies.Optional.Config.jacksonCore)
    api(Dependencies.Optional.Config.jacksonDatabind)
    api(Dependencies.Optional.Config.jacksonKotlin)

    // HikariCP 用于连接池管理
    compileOnly(Dependencies.Optional.Database.hikariCP)

    // 必需格式支持
    implementation(Dependencies.Optional.Config.jacksonYaml)

    // 可选格式支持（compileOnly，运行时可选）
    compileOnly(Dependencies.Optional.Config.jacksonToml)
    compileOnly(Dependencies.Optional.Config.jacksonXml)
    compileOnly(Dependencies.Optional.Config.jacksonCsv)
    compileOnly(Dependencies.Optional.Config.jacksonProperties)
    compileOnly(Dependencies.Optional.Config.jacksonHocon)

    // 测试依赖
    testImplementation(Dependencies.Test.junitJupiter)
    testImplementation(Dependencies.Test.mockk)

    // 测试时需要Paper API来支持Bukkit类型的序列化
    testImplementation(Dependencies.Core.paperApi)

    // 测试时包含所有格式支持
    testImplementation(Dependencies.Optional.Config.jacksonToml)
    testImplementation(Dependencies.Optional.Config.jacksonXml)
    testImplementation(Dependencies.Optional.Config.jacksonCsv)
    testImplementation(Dependencies.Optional.Config.jacksonProperties)
    testImplementation("org.honton.chas.hocon:jackson-dataformat-hocon:1.1.1")
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
