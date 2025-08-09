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
    // 核心依赖
    api(project(":core"))
    compileOnly(Dependencies.Core.paperApi)
    implementation(Dependencies.Core.kotlinStdlib)

    // 协程支持
    implementation(Dependencies.Optional.Async.kotlinCoroutines)
    implementation(Dependencies.Optional.Async.kotlinCoroutinesJdk8)

    // Adventure 组件支持
    compileOnly(Dependencies.Optional.UI.adventureApi)
    compileOnly(Dependencies.Optional.UI.adventureMiniMessage)

    // 测试依赖
    testImplementation(Dependencies.Test.junitJupiter)
    testImplementation(Dependencies.Test.mockk)
    testImplementation(project(":core"))
    testImplementation(Dependencies.Optional.Async.kotlinCoroutinesTest)

    // 测试时需要Paper API和Adventure组件
    testImplementation(Dependencies.Core.paperApi)
    testImplementation(Dependencies.Optional.UI.adventureApi)
    testImplementation(Dependencies.Optional.UI.adventureMiniMessage)
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
