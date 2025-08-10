# Config 模块支持的格式

> 📋 **状态**: 文档规划中，内容正在完善

## 支持的配置格式

Config 模块基于 Jackson 提供多格式配置支持，让您可以选择最适合的配置格式。

### YAML 格式 (推荐)

YAML 是推荐的配置格式，具有良好的可读性：

```yaml
# config.yml
server:
  name: "我的服务器"
  port: 25565
  max-players: 100

features:
  pvp: true
  flight: false
  
database:
  host: "localhost"
  port: 3306
  name: "minecraft"
```

### JSON 格式

适合程序化生成和 API 交互：

```json
{
  "server": {
    "name": "我的服务器",
    "port": 25565,
    "maxPlayers": 100
  },
  "features": {
    "pvp": true,
    "flight": false
  }
}
```

### TOML 格式

专为配置文件设计的格式：

```toml
[server]
name = "我的服务器"
port = 25565
max-players = 100

[features]
pvp = true
flight = false
```

### HOCON 格式

人性化配置对象表示法：

```hocon
server {
  name = "我的服务器"
  port = 25565
  max-players = 100
}

features {
  pvp = true
  flight = false
}
```

### Properties 格式

传统的 Java 属性文件：

```properties
server.name=我的服务器
server.port=25565
server.max-players=100
features.pvp=true
features.flight=false
```

## 格式选择建议

| 格式 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| YAML | 手动编辑配置 | 可读性强，支持注释 | 缩进敏感 |
| JSON | API 交互 | 标准格式，工具支持好 | 不支持注释 |
| TOML | 复杂配置 | 结构清晰，类型明确 | 相对较新 |
| HOCON | 大型配置 | 灵活性强，支持引用 | 学习成本高 |
| Properties | 简单配置 | 简单直接，兼容性好 | 不支持嵌套 |

## 格式转换

Config 模块支持格式间的自动转换：

```kotlin
// 从 YAML 加载，保存为 JSON
val config = configManager.load<MyConfig>("config.yml")
configManager.save(config, "config.json")
```

## 相关文档

- [🚀 快速开始](quick-start.md) - 基本使用方法
- [💡 基础概念](concepts.md) - 配置系统概念
- [📋 API 参考](api-reference.md) - 完整 API 文档

---

**📝 注意**: 此文档正在完善中，更多格式特性请参考 [最佳实践](best-practices.md)。
