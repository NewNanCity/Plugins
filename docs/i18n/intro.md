# I18n 模块介绍

## 🎯 什么是 I18n 模块？

I18n 模块是一个基于 Config 模块的完整多语言支持系统，为 Minecraft 插件提供现代化、类型安全的国际化解决方案。它支持多种配置格式，提供模板变量替换，并与 BasePlugin 和 MessageManager 完美集成。

**5分钟快速了解：** I18n 模块解决了传统多语言支持中语言管理困难、模板替换复杂、回退机制缺失等问题，通过多格式支持、智能回退、模板系统和缓存优化，让国际化变得简单而强大。

## ⚠️ 关键概念 - 动态字段说明

**重要**：I18n 模块默认**不包含**任何内置的动态字段。文档中看到的所有变量（如 `<%player%>`、`<%amount%>`、`<%server_name%>` 等）都需要通过 **Provider** 显式注册才能使用。

- ✅ **内置功能**：基础文本替换、多语言回退、缓存机制
- ❌ **非内置功能**：任何动态字段（需要Provider支持）
- 🔧 **扩展方式**：通过 `registerTemplateProvider` 注册自定义变量

## 🔍 解决的问题

### 传统国际化的痛点

1. **语言管理困难** - 手动管理多个语言文件，容易出错
2. **模板替换复杂** - 字符串模板替换逻辑复杂，性能差
3. **回退机制缺失** - 缺少语言找不到时的回退策略
4. **格式限制** - 只支持单一配置格式，缺乏灵活性
5. **性能问题** - 频繁读取语言文件影响性能
6. **用户体验差** - 无法为不同用户提供个性化语言

### I18n 模块的解决方案

✅ **多格式支持** - 继承Config模块的多格式支持（JSON、YAML、TOML等）
✅ **模板替换** - 支持`<%key%>`格式的高性能模板变量替换
✅ **三级回退** - 主语言 → 默认语言 → 原文本的智能回退机制
✅ **动态重载** - 支持运行时重新加载语言文件
✅ **缓存优化** - 内置缓存机制大幅提升性能
✅ **用户语言** - 支持每个玩家的个性化语言设置

## 🆚 技术对比

### 与传统国际化方案对比

| 特性     | 传统方案 | I18n 模块      |
| -------- | -------- | -------------- |
| 格式支持 | 单一格式 | 多格式支持     |
| 模板替换 | 手动实现 | 内置高性能模板 |
| 回退机制 | 无或简单 | 三级智能回退   |
| 缓存机制 | 无       | 多级缓存       |
| 用户语言 | 全局统一 | 个性化设置     |
| 动态重载 | 不支持   | 完整支持       |

### 与其他国际化库对比

| 库                   | 优势                          | 劣势               |
| -------------------- | ----------------------------- | ------------------ |
| **I18n模块**         | 轻量级、高性能、Minecraft集成 | 功能相对专一       |
| Java ResourceBundle  | 标准库、稳定                  | 功能有限、性能一般 |
| ICU4J                | 功能强大、标准完整            | 体积大、复杂度高   |
| Spring MessageSource | 企业级、功能丰富              | 重量级、依赖多     |

## 🚀 快速示例

### 传统国际化方案
```java
public class OldI18nManager {
    private Map<String, Properties> languages = new HashMap<>();

    public void loadLanguages() {
        // 手动加载每个语言文件
        try {
            Properties zhCN = new Properties();
            zhCN.load(new FileInputStream("lang/zh_CN.properties"));
            languages.put("zh_CN", zhCN);

            Properties enUS = new Properties();
            enUS.load(new FileInputStream("lang/en_US.properties"));
            languages.put("en_US", enUS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMessage(String key, String language) {
        Properties props = languages.get(language);
        if (props == null) {
            props = languages.get("en_US"); // 硬编码回退
        }

        String message = props.getProperty(key);
        if (message == null) {
            return key; // 简单回退
        }

        return message;
    }

    // 手动模板替换
    public String formatMessage(String template, Object... args) {
        return String.format(template, args);
    }
}
```

### I18n 模块方案
```kotlin
class ModernI18nManager : BasePlugin() {
    override fun onPluginEnable() {
        // 简单的语言管理器配置
        setupLanguageManager(
             languageFiles = mapOf(
                 Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                 Locale.US to "lang/en_US.yml",
                 Locale.JAPAN to "lang/ja_JP.yml"
             ),
             mergeWithTemplate = true,
             createBackup = false,
             majorLanguage = Locale.SIMPLIFIED_CHINESE,
             defaultLanguage = Locale.US
         )

        // 使用多语言消息
        messager.info(player, "<%welcome.message%>", player.name, server.name)
        // 结果：欢迎 Steve 来到 我的服务器！
    }
}
```

## 🏗️ 核心架构

### 1. 多格式语言文件

```yaml
# zh_CN.yml (简体中文)
welcome:
  message: "欢迎 {0} 来到 {1}！"
  first_join: "这是 {0} 第一次加入服务器"

economy:
  balance: "您的余额：{0} 金币"
  insufficient: "余额不足，需要 {0} 金币"
```

```json
// en_US.json (English)
{
  "welcome": {
    "message": "Welcome {0} to {1}!",
    "first_join": "This is {0}'s first time joining"
  },
  "economy": {
    "balance": "Your balance: {0} coins",
    "insufficient": "Insufficient balance, need {0} coins"
  }
}
```

### 2. 三级回退机制
```kotlin
// 回退顺序：
// 1. 玩家设置的语言（如果有）
// 2. 主语言（服务器默认语言）
// 3. 默认语言（通常是英语）
// 4. 原始文本（如果都找不到）

val message = languageManager.provideLanguage("<%welcome.message%>", player)
// 查找过程：
// 1. 检查玩家语言设置 -> zh_CN
// 2. 如果zh_CN中没有，检查主语言 -> zh_CN
// 3. 如果主语言没有，检查默认语言 -> en_US
// 4. 如果都没有，返回 "<%welcome.message%>"
```

### 4. 缓存优化系统
```kotlin
// 配置缓存策略
languageManager.setCacheConfig(
    type = CacheType.LRU,
    capacity = 1000,
    expireAfterAccess = Duration.ofMinutes(30)
)

// 缓存层级：
// 1. 模板替换结果缓存
// 2. 语言文件内容缓存
// 3. 玩家语言设置缓存
```

## 📊 语言文件示例

### 完整的多语言配置
```yaml
# zh_CN.yml
system:
  plugin_enabled: "插件已启用"
  plugin_disabled: "插件已禁用"
  config_reloaded: "配置已重新加载"

player:
  join: "欢迎 {0} 加入服务器！"
  quit: "{0} 离开了服务器"
  first_join: "欢迎新玩家 {0}！这是您第一次加入"

economy:
  balance: "余额：{0} 金币"
  pay_success: "成功转账 {0} 金币给 {1}"
  pay_insufficient: "余额不足，您有 {0} 金币，需要 {1} 金币"

error:
  permission_denied: "权限不足"
  player_not_found: "玩家 {0} 不存在"
  invalid_amount: "无效的金额：{0}"
```

```json
// en_US.json
{
  "system": {
    "plugin_enabled": "Plugin enabled",
    "plugin_disabled": "Plugin disabled",
    "config_reloaded": "Configuration reloaded"
  },
  "player": {
    "join": "Welcome {0} to the server!",
    "quit": "{0} left the server",
    "first_join": "Welcome new player {0}! This is your first time joining"
  },
  "economy": {
    "balance": "Balance: {0} coins",
    "pay_success": "Successfully transferred {0} coins to {1}",
    "pay_insufficient": "Insufficient balance, you have {0} coins, need {1} coins"
  },
  "error": {
    "permission_denied": "Permission denied",
    "player_not_found": "Player {0} not found",
    "invalid_amount": "Invalid amount: {0}"
  }
}
```

## 🎯 适用场景

### ✅ 推荐使用
- 面向国际用户的Minecraft插件
- 需要多语言支持的服务器
- 有大量文本内容的插件
- 需要个性化语言设置的应用
- 要求高性能的多语言系统

### ⚠️ 考虑因素
- 维护多个语言文件的成本
- 翻译质量和准确性
- 团队对国际化的重视程度

## 🔄 迁移路径

### 从硬编码文本迁移
1. **提取文本** - 将硬编码的文本提取到语言文件
2. **创建语言管理器** - 配置I18n模块
3. **替换文本引用** - 使用语言键替换硬编码文本
4. **添加模板变量** - 使用模板系统替换字符串拼接

### 从其他国际化方案迁移
1. **分析现有结构** - 了解当前的语言文件结构
2. **转换格式** - 将现有格式转换为支持的格式
3. **配置回退** - 设置合适的语言回退策略
4. **性能优化** - 启用缓存和性能监控

---

**准备开始？** → [🚀 快速开始](quick-start.md)
