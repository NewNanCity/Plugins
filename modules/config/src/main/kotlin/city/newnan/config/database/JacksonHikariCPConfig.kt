package city.newnan.config.database

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.zaxxer.hikari.HikariConfig

@JsonInclude(JsonInclude.Include.NON_NULL)
open class JacksonHikariCPConfig(
    @JsonAlias("url")
    @JsonProperty("jdbcUrl")
    val jdbcUrl: String? = null,

    @JsonProperty("username")
    val username: String? = null,

    @JsonProperty("password")
    val password: String? = null,

    @JsonProperty("driver-class-name")
    val driverClassName: String? = null,

    @JsonProperty("data-source-class-name")
    val dataSourceClassName: String? = null,

    @JsonProperty("data-source-jndi")
    val dataSourceJndi: String? = null,

    @JsonProperty("exception-override-class")
    val exceptionOverrideClassName: String? = null,

    @JsonProperty("pool-name")
    val poolName: String? = null,

    @JsonProperty("schema")
    val schema: String? = null,

    @JsonProperty("transaction-isolation")
    val transactionIsolation: String? = null,

    @JsonProperty("is-auto-commit")
    val isAutoCommit: Boolean? = null,

    @JsonProperty("is-read-only")
    val isReadOnly: Boolean? = null,

    @JsonProperty("is-isolate-internal-queries")
    val isIsolateInternalQueries: Boolean? = null,

    @JsonProperty("is-register-mbeans")
    val isRegisterMbeans: Boolean? = null,

    @JsonProperty("is-allow-pool-suspension")
    val isAllowPoolSuspension: Boolean? = null,

    @JsonProperty("datasource-properties")
    val dataSourceProperties: Map<String, String> = emptyMap(),

    @JsonProperty("initialization-fail-timeout")
    val initializationFailTimeout: Long? = null,

    @JsonProperty("connection-init-sql")
    val connectionInitSql: String? = null,

    @JsonProperty("connection-test-query")
    val connectionTestQuery: String? = null,

    @JsonProperty("max-pool-size")
    val maxPoolSize: Int? = null,

    @JsonProperty("min-idle")
    val minIdle: Int? = null,

    @JsonProperty("connection-timeout")
    val connectionTimeout: Long? = null,

    @JsonProperty("validation-timeout")
    val validationTimeout: Long? = null,

    @JsonProperty("idle-timeout")
    val idleTimeout: Long? = null,

    @JsonProperty("max-lifetime")
    val maxLifetime: Long? = null,

    @JsonProperty("leak-detection-threshold")
    val leakDetectionThreshold: Long? = null,

    @JsonProperty("keepalive-time")
    val keepaliveTime: Long? = null
) {
    fun toHikariConfig(): HikariConfig {
        return HikariConfig().also { config ->
            jdbcUrl?.let { config.jdbcUrl = it }
            username?.let { config.username = it }
            password?.let { config.password = it }
            driverClassName?.let { config.driverClassName = it }
            dataSourceClassName?.let { config.dataSourceClassName = it }
            dataSourceJndi?.let { config.dataSourceJNDI = it }
            exceptionOverrideClassName?.let { config.exceptionOverrideClassName = it }
            poolName?.let { config.poolName = it }
            schema?.let { config.schema = it }
            transactionIsolation?.let { config.transactionIsolation = it }
            isAutoCommit?.let { config.isAutoCommit = it }
            isReadOnly?.let { config.isReadOnly = it }
            isIsolateInternalQueries?.let { config.isIsolateInternalQueries = it }
            isRegisterMbeans?.let { config.isRegisterMbeans = it }
            isAllowPoolSuspension?.let { config.isAllowPoolSuspension = it }
            dataSourceProperties.forEach { (key, value) -> config.addDataSourceProperty(key, value) }
            initializationFailTimeout?.let { config.initializationFailTimeout = it }
            connectionInitSql?.let { config.connectionInitSql = it }
            connectionTestQuery?.let { config.connectionTestQuery = it }
            maxPoolSize?.let { config.maximumPoolSize = it }
            minIdle?.let { config.minimumIdle = it }
            connectionTimeout?.let { config.connectionTimeout = it }
            validationTimeout?.let { config.validationTimeout = it }
            idleTimeout?.let { config.idleTimeout = it }
            maxLifetime?.let { config.maxLifetime = it }
            leakDetectionThreshold?.let { config.leakDetectionThreshold = it }
            keepaliveTime?.let { config.keepaliveTime = it }
        }
    }
}