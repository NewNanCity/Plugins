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
    dependsOn(subprojects.map { it.tasks.named("clean") })
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
            "dev.triumphteam.gui",
            "me.lucko",
            "co.aikar",
            "org.yaml.snakeyaml",
            "com.fasterxml.jackson",
            "com.jasonclawson.jackson",
        ).forEach { relocate(it, "${project.group}._dependencies_.$it") }
    }

    project.ext["load"] = "STARTUP"
    ext["apiVersion"] = "1.16"
    project.ext["authors"] = listOf("Gk0Wk")
    project.ext["depend"] = listOf<String>()
    project.ext["loadBefore"] = listOf<String>()
    project.ext["softDepend"] = listOf<String>()
    project.ext["url"] = "https://www.newnan.city/"
    tasks.processResources {
        filteringCharset = "UTF-8"
        from("src/main/resources") {
            include("**/*.yml", "**/*.yaml", "**/*.json", "**/*.properties")
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            expand(project.properties.mapValues {
                if (it.value is List<*>) {
                    (it.value as List<*>).joinToString(", ") { it1 -> "'${it1.toString()}'" }
                } else {
                    it.value
                }
            })
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
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.toVersion(targetJavaVersion).toString()
    }

    tasks.named<Jar>("jar") { enabled = false }

    tasks.test { useJUnitPlatform() }
}

repositories { mavenCentral() }
