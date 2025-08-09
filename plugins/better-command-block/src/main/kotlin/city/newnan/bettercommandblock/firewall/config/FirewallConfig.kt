package city.newnan.bettercommandblock.firewall.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 防火墙配置类
 *
 * 包含命令方块防火墙的所有配置选项，支持配置热重载。
 * 使用Jackson注解进行序列化/反序列化。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class FirewallConfig(
    /**
     * 是否启用防火墙
     * 当禁用时，所有命令都会被允许执行
     */
    @JsonProperty("enabled")
    val enabled: Boolean = true,
    
    /**
     * 白名单命令列表
     * 这些命令将被允许在命令方块中执行
     */
    @JsonProperty("whitelist-commands")
    val whitelistCommands: Set<String> = setOf(
        // 基础安全命令
        "say", "minecraft:say",
        "tell", "minecraft:tell",
        "msg", "minecraft:msg",
        "tellraw", "minecraft:tellraw",
        "title", "minecraft:title",
        "playsound", "minecraft:playsound",
        "particle", "minecraft:particle",
        
        // 传送命令（会被坐标验证器验证）
        "tp", "minecraft:tp",
        "teleport", "minecraft:teleport",
        
        // 物品命令（会被物品验证器验证）
        "give", "minecraft:give",
        
        // 方块命令（会被坐标验证器验证）
        "setblock", "minecraft:setblock",
        "fill", "minecraft:fill",
        
        // 实体命令（会被验证器验证）
        "summon", "minecraft:summon",
        
        // 时间和天气命令
        "time", "minecraft:time",
        "weather", "minecraft:weather",
        
        // 游戏规则命令（限制性的）
        "gamerule", "minecraft:gamerule",
        
        // 经验命令
        "xp", "minecraft:xp",
        "experience", "minecraft:experience",
        
        // 效果命令
        "effect", "minecraft:effect",
        
        // 记分板命令（基础）
        "scoreboard", "minecraft:scoreboard",
        
        // 执行命令（会被ExecuteValidator验证）
        "execute", "minecraft:execute"
    ),
    
    /**
     * 安全物品列表
     * 用于ItemValidator验证
     */
    @JsonProperty("safe-items")
    val safeItems: Set<String> = setOf(
        // 基础方块
        "minecraft:dirt", "dirt",
        "minecraft:grass_block", "grass_block",
        "minecraft:stone", "stone",
        "minecraft:cobblestone", "cobblestone",
        "minecraft:oak_log", "oak_log",
        "minecraft:oak_planks", "oak_planks",
        "minecraft:sand", "sand",
        "minecraft:gravel", "gravel",
        "minecraft:clay", "clay",
        
        // 建筑材料
        "minecraft:bricks", "bricks",
        "minecraft:stone_bricks", "stone_bricks",
        "minecraft:oak_stairs", "oak_stairs",
        "minecraft:oak_slab", "oak_slab",
        "minecraft:glass", "glass",
        "minecraft:wool", "wool",
        "minecraft:white_wool", "white_wool",
        "minecraft:concrete", "concrete",
        
        // 工具和武器（基础）
        "minecraft:wooden_sword", "wooden_sword",
        "minecraft:wooden_pickaxe", "wooden_pickaxe",
        "minecraft:wooden_axe", "wooden_axe",
        "minecraft:wooden_shovel", "wooden_shovel",
        "minecraft:wooden_hoe", "wooden_hoe",
        "minecraft:stone_sword", "stone_sword",
        "minecraft:stone_pickaxe", "stone_pickaxe",
        "minecraft:stone_axe", "stone_axe",
        "minecraft:stone_shovel", "stone_shovel",
        "minecraft:stone_hoe", "stone_hoe",
        
        // 食物
        "minecraft:bread", "bread",
        "minecraft:apple", "apple",
        "minecraft:cooked_beef", "cooked_beef",
        "minecraft:cooked_pork", "cooked_pork",
        "minecraft:cooked_chicken", "cooked_chicken",
        "minecraft:carrot", "carrot",
        "minecraft:potato", "potato",
        "minecraft:wheat", "wheat",
        
        // 装饰物品
        "minecraft:flower_pot", "flower_pot",
        "minecraft:painting", "painting",
        "minecraft:item_frame", "item_frame",
        "minecraft:torch", "torch",
        "minecraft:lantern", "lantern"
    ),
    
    /**
     * 最大物品数量
     * 用于ItemValidator验证
     */
    @JsonProperty("max-item-quantity")
    val maxItemQuantity: Int = 64,
    
    /**
     * 是否允许自定义命名空间的物品
     * 用于ItemValidator验证
     */
    @JsonProperty("allow-custom-namespaces")
    val allowCustomNamespaces: Boolean = false,
    
    /**
     * 最大坐标范围
     * 用于CoordinateValidator验证
     */
    @JsonProperty("max-coordinate-range")
    val maxCoordinateRange: Double = 1000.0,
    
    /**
     * 是否允许相对坐标（~）
     * 用于CoordinateValidator验证
     */
    @JsonProperty("allow-relative-coordinates")
    val allowRelativeCoordinates: Boolean = true,
    
    /**
     * 是否允许局部坐标（^）
     * 用于CoordinateValidator验证
     */
    @JsonProperty("allow-local-coordinates")
    val allowLocalCoordinates: Boolean = true,
    
    /**
     * 允许的选择器类型
     * 用于SelectorValidator验证
     */
    @JsonProperty("allowed-selectors")
    val allowedSelectors: Set<String> = setOf("@s"),
    
    /**
     * 选择器最大范围
     * 用于SelectorValidator验证
     */
    @JsonProperty("max-selector-range")
    val maxSelectorRange: Double = 100.0,
    
    /**
     * 是否允许直接使用玩家名
     * 用于SelectorValidator验证
     */
    @JsonProperty("allow-player-names")
    val allowPlayerNames: Boolean = true,
    
    /**
     * 最大目标数量
     * 用于SelectorValidator验证
     */
    @JsonProperty("max-target-count")
    val maxTargetCount: Int = 1,
    
    /**
     * Execute命令最大递归深度
     * 用于ExecuteValidator验证
     */
    @JsonProperty("max-execute-depth")
    val maxExecuteDepth: Int = 10,
    
    /**
     * 是否销毁被阻止的命令方块
     * 当命令被阻止时，是否将命令方块替换为空气
     */
    @JsonProperty("destroy-blocked-command-blocks")
    val destroyBlockedCommandBlocks: Boolean = true,
    
    /**
     * 性能配置
     */
    @JsonProperty("performance")
    val performance: PerformanceConfig = PerformanceConfig(),
    
    /**
     * 监控配置
     */
    @JsonProperty("monitoring")
    val monitoring: MonitoringConfig = MonitoringConfig()
)

/**
 * 性能配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PerformanceConfig(
    /**
     * 验证超时时间（毫秒）
     * 如果验证时间超过此值，将记录警告
     */
    @JsonProperty("validation-timeout-ms")
    val validationTimeoutMs: Long = 100L,
    
    /**
     * 命令缓存大小
     * 缓存最近验证过的命令结果
     */
    @JsonProperty("command-cache-size")
    val commandCacheSize: Int = 1000,
    
    /**
     * 缓存过期时间（秒）
     */
    @JsonProperty("cache-expire-seconds")
    val cacheExpireSeconds: Long = 300L,
    
    /**
     * 是否启用异步验证
     * 对于复杂的验证可以异步执行
     */
    @JsonProperty("async-validation")
    val asyncValidation: Boolean = false
)

/**
 * 监控配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MonitoringConfig(
    /**
     * 是否启用统计收集
     */
    @JsonProperty("enable-statistics")
    val enableStatistics: Boolean = true,
    
    /**
     * 统计更新间隔（秒）
     */
    @JsonProperty("statistics-interval-seconds")
    val statisticsIntervalSeconds: Long = 30L,
    
    /**
     * 是否记录详细日志
     */
    @JsonProperty("detailed-logging")
    val detailedLogging: Boolean = false,
    
    /**
     * 是否记录允许的命令
     * 通常只记录被阻止的命令，启用此选项会记录所有命令
     */
    @JsonProperty("log-allowed-commands")
    val logAllowedCommands: Boolean = false,
    
    /**
     * 日志文件最大大小（MB）
     */
    @JsonProperty("max-log-file-size-mb")
    val maxLogFileSizeMb: Int = 10,
    
    /**
     * 保留的日志文件数量
     */
    @JsonProperty("log-file-retention-count")
    val logFileRetentionCount: Int = 5
)
