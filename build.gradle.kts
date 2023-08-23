import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("idea")
    id("java")
    kotlin("jvm") version "1.6.21"
    // https://github.com/johnrengelman/shadow
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val targetJavaVersion = 11
tasks.register("buildAll") {
    // dependsOn(subprojects.map { it.tasks.named("clean") })
    dependsOn(subprojects.map { it.tasks.named("shadowJar") })
}
tasks.register("cleanAll") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}

subprojects {
    apply(plugin = "idea")
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    // https://github.com/johnrengelman/shadow
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
         mavenLocal()
         maven("https://jitpack.io")
         maven("https://repo.lucko.me")
         maven("https://libraries.minecraft.net")
         maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
         maven("https://repo.essentialsx.net/releases/")
         maven("https://papermc.io/repo/repository/maven-public")
         maven("https://repo.aikar.co/content/groups/aikar")
         maven("https://repo.mikeprimm.com/")
         maven("https://repo.triumphteam.dev/snapshots/")
         mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
        testImplementation("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
        testImplementation(kotlin("test"))
    }

    tasks.shadowJar {
        manifest {
            attributes["Main-Class"] = "${project.group}.PluginMain"
        }
        dependencies {
            exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
            exclude("LICENSE*", "LICENSE", "license*", "license")
        }
        archiveFileName.set("${project.name}-${project.version}.jar")
        minimize {
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
            include(dependency("org.jetbrains.kotlin:kotlin-reflect"))
        }
        // Relocate
        listOf(
            "city.newnan.violet",
            "dev.triumphteam",
            "me.lucko",
            "co.aikar",
            "org.yaml.snakeyaml",
            "com.fasterxml.jackson",
            "com.jasonclawson.jackson",
        ).forEach { relocate(it, "${project.group}._dependencies_.$it") }
    }

    project.ext["load"] = "POSTWORLD" // "STARTUP"
    ext["apiVersion"] = "1.16"
    project.ext["contributors"] = emptySet<String>()
    project.ext["authors"] = setOf("Gk0Wk")
    project.ext["depend"] = emptySet<String>()
    project.ext["loadBefore"] = emptySet<String>()
    project.ext["softDepend"] = setOf("Citizens", "QuickShop", "Essentials", "WorldEdit", "WorldGuard")
    project.ext["provides"] = emptySet<String>()
    project.ext["libraries"] = emptySet<String>()
    project.ext["url"] = "https://www.newnan.city/"
    tasks.processResources {
        outputs.upToDateWhen { false } // 禁用增量构建, processResources 看到文件未修改就不处理了, 上面这一堆修改了也不会更新
        filteringCharset = "UTF-8"
        from("src/main/resources") {
            include("**/*.yml", "**/*.yaml", "**/*.json", "**/*.properties")
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            expand(project.properties)
        }
    }

    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    java {
        val javaVersion = JavaVersion.toVersion(targetJavaVersion)
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.javaParameters = true
        kotlinOptions.jvmTarget = JavaVersion.toVersion(targetJavaVersion).toString()
    }

    tasks.named<Jar>("jar") { enabled = false }

    tasks.test { useJUnitPlatform() }
}

repositories { mavenCentral() }
