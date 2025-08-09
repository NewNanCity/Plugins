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
    // 依赖核心模块
    api(project(":core"))

    // 依赖配置模块
    api(project(":modules:config"))

    // PaperMC API
    compileOnly(Dependencies.Core.paperApi)
    implementation(Dependencies.Core.kotlinStdlib)

    // Jackson 核心依赖（从config模块继承）
    // 这些依赖会通过config模块传递过来

    // 测试依赖
    testImplementation(Dependencies.Test.kotlinTest)
    testImplementation(Dependencies.Test.junitJupiter)
    testImplementation(Dependencies.Test.mockito)
    testImplementation(Dependencies.Test.mockitoKotlin)
}

tasks.test {
    useJUnitPlatform()
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
