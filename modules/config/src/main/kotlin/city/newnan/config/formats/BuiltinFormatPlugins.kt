@file:Suppress("unused")

package city.newnan.config.formats

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import city.newnan.config.serializers.registerBukkitSerializers

/**
 * 反射操作缓存，避免重复的类加载检查
 */
private object ReflectionCache {
    private val classCache = java.util.concurrent.ConcurrentHashMap<String, Class<*>?>()
    private val availabilityCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    
    /**
     * 检查类是否可用，结果会被缓存
     */
    fun isClassAvailable(className: String): Boolean {
        return availabilityCache.computeIfAbsent(className) { 
            try {
                Class.forName(className)
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }
    
    /**
     * 获取类实例，结果会被缓存
     */
    fun getClass(className: String): Class<*>? {
        return classCache.computeIfAbsent(className) { 
            try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                null
            }
        }
    }
}

/**
 * JSON 格式插件（核心格式）
 */
class JsonFormatPlugin : ConfigFormatPlugin {
    override val formatName = "JSON"
    override val supportedExtensions = setOf("json")
    override val isCoreFormat = true

    override fun createMapper(): ObjectMapper {
        return ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerKotlinModule()
            .registerBukkitSerializers()
    }

    override fun isAvailable(): Boolean = true
}

/**
 * YAML 格式插件（核心格式）
 */
class YamlFormatPlugin : ConfigFormatPlugin {
    override val formatName = "YAML"
    override val supportedExtensions = setOf("yml", "yaml")
    override val isCoreFormat = true

    override fun createMapper(): ObjectMapper {
        val mapper = YAMLMapper()
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
        mapper.registerKotlinModule()
        mapper.registerBukkitSerializers()
        return mapper
    }

    override fun isAvailable(): Boolean = true
}

/**
 * TOML 格式插件（可选格式）
 */
class TomlFormatPlugin : ConfigFormatPlugin {
    override val formatName = "TOML"
    override val supportedExtensions = setOf("toml")
    override val isCoreFormat = false
    
    companion object {
        private const val TOML_MAPPER_CLASS = "com.fasterxml.jackson.dataformat.toml.TomlMapper"
    }

    override fun createMapper(): ObjectMapper {
        return try {
            val tomlMapperClass = ReflectionCache.getClass(TOML_MAPPER_CLASS)
                ?: throw ConfigFormatException.MissingDependencyException(
                    formatName, "jackson-dataformat-toml"
                )
            
            val mapper = tomlMapperClass.getDeclaredConstructor().newInstance() as ObjectMapper
            mapper
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerKotlinModule()
                .registerBukkitSerializers()
        } catch (e: Exception) {
            throw ConfigFormatException.MissingDependencyException(
                formatName, "jackson-dataformat-toml"
            )
        }
    }

    override fun isAvailable(): Boolean {
        return ReflectionCache.isClassAvailable(TOML_MAPPER_CLASS)
    }
}

/**
 * XML 格式插件（可选格式）
 */
class XmlFormatPlugin : ConfigFormatPlugin {
    override val formatName = "XML"
    override val supportedExtensions = setOf("xml")
    override val isCoreFormat = false
    
    companion object {
        private const val XML_MAPPER_CLASS = "com.fasterxml.jackson.dataformat.xml.XmlMapper"
    }

    override fun createMapper(): ObjectMapper {
        return try {
            val xmlMapperClass = ReflectionCache.getClass(XML_MAPPER_CLASS)
                ?: throw ConfigFormatException.MissingDependencyException(
                    formatName, "jackson-dataformat-xml"
                )
            
            val mapper = xmlMapperClass.getDeclaredConstructor().newInstance() as ObjectMapper
            mapper
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerKotlinModule()
                .registerBukkitSerializers()
        } catch (e: Exception) {
            throw ConfigFormatException.MissingDependencyException(
                formatName, "jackson-dataformat-xml"
            )
        }
    }

    override fun isAvailable(): Boolean {
        return ReflectionCache.isClassAvailable(XML_MAPPER_CLASS)
    }
}

/**
 * CSV 格式插件（可选格式）
 */
class CsvFormatPlugin : ConfigFormatPlugin {
    override val formatName = "CSV"
    override val supportedExtensions = setOf("csv")
    override val isCoreFormat = false
    
    companion object {
        private const val CSV_MAPPER_CLASS = "com.fasterxml.jackson.dataformat.csv.CsvMapper"
    }

    override fun createMapper(): ObjectMapper {
        return try {
            val csvMapperClass = ReflectionCache.getClass(CSV_MAPPER_CLASS)
                ?: throw ConfigFormatException.MissingDependencyException(
                    formatName, "jackson-dataformat-csv"
                )
            
            val mapper = csvMapperClass.getDeclaredConstructor().newInstance() as ObjectMapper
            mapper
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerKotlinModule()
                .registerBukkitSerializers()
        } catch (e: Exception) {
            throw ConfigFormatException.MissingDependencyException(
                formatName, "jackson-dataformat-csv"
            )
        }
    }

    override fun isAvailable(): Boolean {
        return ReflectionCache.isClassAvailable(CSV_MAPPER_CLASS)
    }
}

/**
 * Properties 格式插件（可选格式）
 */
class PropertiesFormatPlugin : ConfigFormatPlugin {
    override val formatName = "Properties"
    override val supportedExtensions = setOf("properties")
    override val isCoreFormat = false
    
    companion object {
        private const val PROPS_MAPPER_CLASS = "com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper"
    }

    override fun createMapper(): ObjectMapper {
        return try {
            val propsMapperClass = ReflectionCache.getClass(PROPS_MAPPER_CLASS)
                ?: throw ConfigFormatException.MissingDependencyException(
                    formatName, "jackson-dataformat-properties"
                )
            
            val mapper = propsMapperClass.getDeclaredConstructor().newInstance() as ObjectMapper
            mapper
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerKotlinModule()
                .registerBukkitSerializers()
        } catch (e: Exception) {
            throw ConfigFormatException.MissingDependencyException(
                formatName, "jackson-dataformat-properties"
            )
        }
    }

    override fun isAvailable(): Boolean {
        return ReflectionCache.isClassAvailable(PROPS_MAPPER_CLASS)
    }
}

/**
 * HOCON 格式插件（可选格式）
 */
class HoconFormatPlugin : ConfigFormatPlugin {
    override val formatName = "HOCON"
    override val supportedExtensions = setOf("conf", "hocon")
    override val isCoreFormat = false
    
    companion object {
        private const val HOCON_FACTORY_CLASS = "org.honton.chas.hocon.HoconFactory"
    }

    override fun createMapper(): ObjectMapper {
        return try {
            val hoconFactoryClass = ReflectionCache.getClass(HOCON_FACTORY_CLASS)
                ?: throw ConfigFormatException.MissingDependencyException(
                    formatName, "jackson-dataformat-hocon"
                )
            
            val factory = hoconFactoryClass.getDeclaredConstructor().newInstance()
            val mapper = ObjectMapper(factory as com.fasterxml.jackson.core.JsonFactory)
            mapper
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerKotlinModule()
                .registerBukkitSerializers()
        } catch (e: Exception) {
            throw ConfigFormatException.MissingDependencyException(
                formatName, "jackson-dataformat-hocon"
            )
        }
    }

    override fun isAvailable(): Boolean {
        return ReflectionCache.isClassAvailable(HOCON_FACTORY_CLASS)
    }
}
