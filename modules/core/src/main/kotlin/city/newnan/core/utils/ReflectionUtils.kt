package city.newnan.core.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * 反射工具类
 *
 * 提供安全的反射操作，支持缓存以提高性能。
 * 支持 NMS 和 CraftBukkit 类的访问。
 * 融合了 violet/Reflection 和 helper/reflect 的功能。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
object ReflectionUtils {

    private val minecraftClassCache: MutableMap<String, Class<*>?> = HashMap()
    private val bukkitClassCache: MutableMap<String, Class<*>?> = HashMap()
    private val methodCache: MutableMap<Class<*>, MutableMap<Pair<String, Array<out Class<*>?>>, Method?>> = HashMap()
    private val declaredMethodCache: MutableMap<Class<*>, MutableMap<Pair<String, Array<out Class<*>?>>, Method?>> = HashMap()
    private val fieldCache: MutableMap<Class<*>, MutableMap<String, Field?>> = HashMap()
    private val declaredFieldCache: MutableMap<Class<*>, MutableMap<String, Field?>> = HashMap()
    private val foundFields: MutableMap<Class<*>, MutableMap<Class<*>, Field?>> = HashMap()
    private val constructorCache: MutableMap<Class<*>, MutableMap<List<Class<*>?>, Constructor<*>?>> = HashMap()

    /**
     * NMS 包前缀
     */
    const val NMS_PREFIX = "net.minecraft.server"

    /**
     * OBC 包前缀
     */
    const val OBC_PREFIX = "org.bukkit.craftbukkit"

    /**
     * 获取服务器版本
     */
    val serverVersion: String by lazy {
        val server = Bukkit.getServer().javaClass
        if (server.simpleName == "CraftServer" && server.name != "org.bukkit.craftbukkit.CraftServer") {
            val obcPackage = server.`package`.name
            if (obcPackage.startsWith("org.bukkit.craftbukkit.")) {
                obcPackage.substring("org.bukkit.craftbukkit.".length)
            } else ""
        } else ""
    }

    /**
     * 获取 CraftBukkit 类
     */
    @Synchronized
    fun getBukkitClass(className: String): Class<*>? {
        return bukkitClassCache.getOrPut(className) {
            val clazzName = "org.bukkit.craftbukkit.$serverVersion.$className"
            try {
                Class.forName(clazzName)
            } catch (e: ClassNotFoundException) {
                null
            }
        }
    }

    /**
     * 获取 Minecraft 服务端类 (NMS)
     */
    @Synchronized
    fun getMinecraftClass(className: String): Class<*>? {
        return minecraftClassCache.getOrPut(className) {
            val clazzName = "net.minecraft.server.$serverVersion.$className"
            try {
                Class.forName(clazzName)
            } catch (e: ClassNotFoundException) {
                null
            }
        }
    }

    /**
     * 获取玩家的网络连接对象
     */
    fun getPlayerConnection(player: Player): Any? {
        val getHandleMethod = getMethod(player.javaClass, "getHandle")
        if (getHandleMethod != null) {
            try {
                val nmsPlayer = getHandleMethod.invoke(player)
                val playerConField = getField(nmsPlayer.javaClass, "playerConnection")
                return playerConField?.get(nmsPlayer)
            } catch (e: Exception) {
                // 静默处理异常
            }
        }
        return null
    }

    /**
     * 获取构造函数
     */
    @Synchronized
    fun getConstructor(clazz: Class<*>, vararg params: Class<*>?): Constructor<*>? {
        val paramsList = params.toList()
        return constructorCache.getOrPut(clazz) { mutableMapOf() }.getOrPut(paramsList) {
            try {
                val constructor = clazz.getConstructor(*params)
                constructor.isAccessible = true
                constructor
            } catch (e: NoSuchMethodException) {
                null
            }
        }
    }

    /**
     * 获取声明的构造函数
     */
    fun getDeclaredConstructor(clazz: Class<*>, vararg params: Class<*>?): Constructor<*>? {
        return try {
            val constructor = clazz.getDeclaredConstructor(*params)
            constructor.isAccessible = true
            constructor
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    /**
     * 创建 NMS 类的完整名称
     */
    fun nmsClassName(className: String): String {
        return if (serverVersion.isEmpty()) {
            "$NMS_PREFIX.$className"
        } else {
            "$NMS_PREFIX.$serverVersion.$className"
        }
    }

    /**
     * 创建 OBC 类的完整名称
     */
    fun obcClassName(className: String): String {
        return if (serverVersion.isEmpty()) {
            "$OBC_PREFIX.$className"
        } else {
            "$OBC_PREFIX.$serverVersion.$className"
        }
    }

    /**
     * 获取 NMS 类（使用完整类名）
     */
    fun nmsClass(className: String): Class<*>? {
        return try {
            Class.forName(nmsClassName(className))
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    /**
     * 获取 OBC 类（使用完整类名）
     */
    fun obcClass(className: String): Class<*>? {
        return try {
            Class.forName(obcClassName(className))
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    /**
     * 获取公共方法
     */
    fun getMethod(clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? =
        getMethod(false, clazz, methodName, *params)

    /**
     * 获取公共方法（可选择是否静默）
     */
    @Synchronized
    fun getMethod(silent: Boolean, clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? =
        methodCache.getOrPut(clazz) { mutableMapOf() }.getOrPut(methodName to params) {
            try {
                val method = clazz.getMethod(methodName, *params)
                method.isAccessible = true
                method
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 获取声明的方法
     */
    fun getDeclaredMethod(clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? =
        getDeclaredMethod(false, clazz, methodName, *params)

    /**
     * 获取声明的方法（可选择是否静默）
     */
    @Synchronized
    fun getDeclaredMethod(silent: Boolean, clazz: Class<*>, methodName: String, vararg params: Class<*>?): Method? =
        declaredMethodCache.getOrPut(clazz) { mutableMapOf() }.getOrPut(methodName to params) {
            try {
                val method = clazz.getDeclaredMethod(methodName, *params)
                method.isAccessible = true
                method
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 获取公共字段
     */
    @Synchronized
    fun getField(clazz: Class<*>, fieldName: String): Field? =
        fieldCache.getOrPut(clazz) { mutableMapOf() }.getOrPut(fieldName) {
            try {
                val field = clazz.getField(fieldName)
                field.isAccessible = true
                field
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 获取声明的字段
     */
    @Synchronized
    fun getDeclaredField(clazz: Class<*>, fieldName: String): Field? =
        declaredFieldCache.getOrPut(clazz) { mutableMapOf() }.getOrPut(fieldName) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 根据类型查找字段
     */
    @Synchronized
    fun findField(clazz: Class<*>, type: Class<*>): Field? =
        foundFields.getOrPut(clazz) { mutableMapOf() }.getOrPut(type) {
            try {
                val allFields = mutableListOf<Field>().apply {
                    addAll(clazz.fields)
                    addAll(clazz.declaredFields)
                }
                allFields.find { it.type == type }?.also { it.isAccessible = true }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 安全地调用方法
     */
    fun <T> safeInvoke(method: Method?, instance: Any?, vararg args: Any?): T? {
        return try {
            @Suppress("UNCHECKED_CAST")
            method?.invoke(instance, *args) as? T
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 安全地获取字段值
     */
    fun <T> safeGet(field: Field?, instance: Any?): T? {
        return try {
            @Suppress("UNCHECKED_CAST")
            field?.get(instance) as? T
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 安全地设置字段值
     */
    fun safeSet(field: Field?, instance: Any?, value: Any?): Boolean {
        return try {
            field?.set(instance, value)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查类是否存在
     */
    fun classExists(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * 获取类的所有字段（包括父类）
     */
    fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var currentClass: Class<*>? = clazz

        while (currentClass != null) {
            fields.addAll(currentClass.declaredFields)
            currentClass = currentClass.superclass
        }

        return fields
    }

    /**
     * 获取类的所有方法（包括父类）
     */
    fun getAllMethods(clazz: Class<*>): List<Method> {
        val methods = mutableListOf<Method>()
        var currentClass: Class<*>? = clazz

        while (currentClass != null) {
            methods.addAll(currentClass.declaredMethods)
            currentClass = currentClass.superclass
        }

        return methods
    }
}
