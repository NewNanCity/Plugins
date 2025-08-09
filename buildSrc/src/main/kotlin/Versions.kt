/**
 * 版本管理对象
 * 统一管理所有依赖的版本号
 */
object Versions {
    const val kotlin = "2.2.0"

    // 开发环境JDK版本（用于开发和编译）
    const val developmentJdk = 21

    // 目标运行时JDK版本（生成的字节码兼容性）
    const val targetJdk = 17

    const val paperApi = "1.20.1-R0.1-SNAPSHOT"
    const val minecraftVersion = "1.20.1"
    const val apiVersion = "1.20"  // Paper 1.20.1 支持的API版本
    const val shadow = "8.3.7"

    // 项目版本
    const val project = "1.0-SNAPSHOT"

    // 通用库版本
    const val kotlinStdlib = "2.2.0"

    // 测试库版本
    const val junit = "5.13.2"
    const val mockito = "5.7.0"
    const val mockitoKotlin = "5.2.1"
    const val mockk = "1.13.8"

    // CommandApi版本
    const val commandApi = "10.0.1"
    const val cloudBukkit = "2.0.0-beta.10"
    const val cloudCore = "2.0.0"

    // 序列化库版本
    const val jackson = "2.18.3"
    const val kotlinxSerialization = "1.6.3"

    // 数据库版本
    const val ktorm = "4.1.1"
    const val hikariCP = "6.3.0"
    const val exposed = "1.0.0-beta-4"
    const val h2 = "2.3.232"
    const val mysql = "8.0.33"
    const val mariadb = "3.5.4"
    const val postgres = "42.7.7"
    const val sqlite = "3.50.2.0"

    // 网络库版本
    const val ktor = "2.3.12"

    // UI库版本
    const val adventure = "4.14.0"

    // 工具库版本
    const val cronUtils = "9.2.1"
    const val vault = "1.7.1"
    const val authlib = "1.5.21"
    const val essentialsX = "2.21.1"
    const val xConomy = "2.25.1"
    const val dynmap = "3.7-beta-6"

    // 协程版本
    const val coroutines = "1.10.2"
    const val coroutinesTest = "1.8.0"
}
