/**
 * 依赖管理对象
 * 统一管理所有项目依赖，支持可选依赖
 */
object Dependencies {
    // 核心依赖 - 所有插件都需要
    object Core {
        const val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlinStdlib}"
        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
        const val paperApi = "io.papermc.paper:paper-api:${Versions.paperApi}"
    }

    // 测试依赖
    object Test {
        const val junitJupiter = "org.junit.jupiter:junit-jupiter:${Versions.junit}"
        const val junitJupiterApi = "org.junit.jupiter:junit-jupiter-api:${Versions.junit}"
        const val junitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit}"
        const val mockk = "io.mockk:mockk:${Versions.mockk}"
        const val mockito = "org.mockito:mockito-core:${Versions.mockito}"
        const val mockitoKotlin = "org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}"
        const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
    }

    // 可选依赖 - 按需引入
    object Optional {
        // 数据库相关
        object Database {
            const val mysql = "mysql:mysql-connector-java:${Versions.mysql}"
            const val mariadb = "org.mariadb.jdbc:mariadb-java-client:${Versions.mariadb}"
            const val postgresql = "org.postgresql:postgresql:${Versions.postgres}"
            const val sqlite = "org.xerial:sqlite-jdbc:${Versions.sqlite}"
            const val hikariCP = "com.zaxxer:HikariCP:${Versions.hikariCP}"
            const val h2 = "com.h2database:h2:${Versions.h2}"
            const val ktormCore = "org.ktorm:ktorm-core:${Versions.ktorm}"
            const val ktormSupport = "org.ktorm:ktorm-support-mysql:${Versions.ktorm}"
            const val exposedCore = "org.jetbrains.exposed:exposed-core:${Versions.exposed}"
            const val exposedJdbc = "org.jetbrains.exposed:exposed-jdbc:${Versions.exposed}"
            const val exposedDao = "org.jetbrains.exposed:exposed-dao:${Versions.exposed}"
            const val exposedJson = "org.jetbrains.exposed:exposed-json:${Versions.exposed}"
            const val exposedKotlinDatetime = "org.jetbrains.exposed:exposed-kotlin-datetime:${Versions.exposed}"
        }

        // 指令
        object Command {
            const val commandapi = "dev.jorel:commandapi-bukkit-core:${Versions.commandApi}"
            const val commandapiKotlin = "dev.jorel:commandapi-bukkit-kotlin:${Versions.commandApi}"
            const val cloudBukkit = "org.incendo:cloud-bukkit:${Versions.cloudBukkit}"
            const val cloudPaper = "org.incendo:cloud-paper:${Versions.cloudBukkit}"
            const val cloudMinecraftExtras = "org.incendo:cloud-minecraft-extras:${Versions.cloudBukkit}"
            const val cloudAnnotations = "org.incendo:cloud-annotations:${Versions.cloudCore}"
        }

        // 配置和序列化
        object Config {
            const val jacksonCore = "com.fasterxml.jackson.core:jackson-core:${Versions.jackson}"
            const val jacksonDatabind = "com.fasterxml.jackson.core:jackson-databind:${Versions.jackson}"
            const val jacksonKotlin = "com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jackson}"
            const val jacksonYaml = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${Versions.jackson}"
            const val jacksonToml = "com.fasterxml.jackson.dataformat:jackson-dataformat-toml:${Versions.jackson}"
            const val jacksonXml = "com.fasterxml.jackson.dataformat:jackson-dataformat-xml:${Versions.jackson}"
            const val jacksonCsv = "com.fasterxml.jackson.dataformat:jackson-dataformat-csv:${Versions.jackson}"
            const val jacksonProperties = "com.fasterxml.jackson.dataformat:jackson-dataformat-properties:${Versions.jackson}"
            const val jacksonHocon = "org.honton.chas.hocon:jackson-dataformat-hocon:1.1.1"
        }

        // 缓存
        object Cache {
            const val caffeine = "com.github.ben-manes.caffeine:caffeine:3.1.8"
            const val redis = "redis.clients:jedis:5.0.0"
        }

        // 日志
        object Logging {
            const val slf4jApi = "org.slf4j:slf4j-api:2.0.9"
            const val logback = "ch.qos.logback:logback-classic:1.4.11"
        }

        // 工具库
        object Utils {
            const val guava = "com.google.guava:guava:32.1.2-jre"
            const val commonsLang = "org.apache.commons:commons-lang3:3.13.0"
            const val commonsIO = "commons-io:commons-io:2.13.0"
            const val ulid = "com.github.f4b6a3:ulid-creator:5.2.3"
            const val cronUtils = "com.cronutils:cron-utils:${Versions.cronUtils}"
        }

        // 协程和异步
        object Async {
            const val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
            const val kotlinCoroutinesJdk8 = "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.coroutines}"
            const val kotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutinesTest}"
        }

        // 网络和HTTP
        object Network {
            const val ktor = "io.ktor:ktor-client-core:${Versions.ktor}"
            const val ktorOkHttp = "io.ktor:ktor-client-okhttp:${Versions.ktor}"
            const val ktorContentNegotiation = "io.ktor:ktor-client-content-negotiation:${Versions.ktor}"
            const val ktorLogging = "io.ktor:ktor-client-logging:${Versions.ktor}"
            const val ktorAuth = "io.ktor:ktor-client-auth:${Versions.ktor}"
            const val ktorResources = "io.ktor:ktor-client-resources:${Versions.ktor}"
            const val ktorSerializationJson = "io.ktor:ktor-serialization-kotlinx-json:${Versions.ktor}"
            const val ktorSerializationJackson = "io.ktor:ktor-serialization-jackson:${Versions.ktor}"
            const val ktorMock = "io.ktor:ktor-client-mock:${Versions.ktor}"
            const val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinxSerialization}"
        }

        // UI和界面
        object UI {
            const val adventureApi = "net.kyori:adventure-api:${Versions.adventure}"
            const val adventureMiniMessage = "net.kyori:adventure-text-minimessage:${Versions.adventure}"
        }

        // 第三方插件API
        object ThirdParty {
            const val vault = "com.github.MilkBowl:VaultAPI:${Versions.vault}"
            const val authlib = "com.mojang:authlib:${Versions.authlib}"
            const val essentialsX = "net.essentialsx:EssentialsX:${Versions.essentialsX}"
            const val xConomy = "com.github.YiC200333:XConomyAPI:${Versions.xConomy}"
            const val worldEdit = "com.sk89q.worldedit:worldedit-bukkit:7.0.0"
            const val dynmapApi = "us.dynmap:DynmapCoreAPI:${Versions.dynmap}"
        }
    }
}
