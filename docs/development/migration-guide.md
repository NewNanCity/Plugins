# 迁移指南

从旧版 violet 框架迁移到现代化 BasePlugin + Cloud 命令框架的标准模式。

## 迁移模式总结

### 1. 主类
继承 `BasePlugin`，实现 `reloadPlugin()`。

### 2. 配置
创建 Config 数据类，实现 `getCoreConfig()`。

### 3. 语言
LanguageKeys 常量类 + 中英文 yml 文件。

### 4. 命令
CommandRegistry + Cloud 注解命令类。

### 5. 模块
BaseModule 子类，**手动调用 `init()`**。

### 6. GUI
使用 GUI1 模块的现代化界面。

### 7. 构建
使用 `newnancity-plugin` 约定，Shadow 打包。

## 迁移技术要点

- **配置管理**：使用 Jackson 多格式配置，支持 `touchWithMerge` 自动补全
- **命令系统**：Cloud 框架注解驱动，完全替代 ACF / CommandAPI
- **事件处理**：BaseModule 的 `subscribeEvent` DSL
- **资源管理**：自动绑定生命周期，无需手动清理
- **国际化**：五层架构语言键分类，MiniMessage 格式支持
- **依赖管理**：软依赖处理（WorldEdit、Dynmap、Vault）

## GUI 迁移模式

### 架构变更
- **旧系统**：violet.gui.PlayerGuiSession + triumph-gui
- **新系统**：GUI1 模块 + openPage DSL + 组件化设计

### 关键迁移步骤

1. **导入更新**：`import city.newnan.gui.dsl.*` + 组件特定导入
2. **页面创建**：`plugin.openPage(InventoryType, size, player, title)` 替代旧的 session.open
3. **组件使用**：
   - `slotComponent(x, y)` 替代直接设置物品
   - `paginatedComponent()` 用于分页显示
   - `render {}` 块定义物品渲染逻辑
   - `onLeftClick {}` 处理点击事件
4. **物品创建**：
   - `item(Material) {}` 创建基础物品
   - `skull(player, name, lore)` 创建头颅
   - `urlSkull(hash)` 创建自定义材质头颅
5. **导航管理**：
   - `back()` 返回上一页
   - `close()` 关闭当前页面
   - `show()` 刷新当前页面

### 功能对应关系

| 旧系统           | 新系统               |
| ---------------- | -------------------- |
| debounce         | 内置防重复点击       |
| pageGui          | `paginatedComponent` |
| PlayerGuiSession | 自动会话栈管理       |
| ItemBuilder      | `item {}` DSL        |

## 命令系统迁移

### 从 CommandAPI / ACF 到 Cloud

**移除**：
- BaseCommand、CommandValidator、CommandPermissions、CommandMessages
- CommandAPI 依赖和相关代码

**新增**：
- Cloud 框架依赖（cloud-paper、cloud-annotations、cloud-minecraft-extras）
- 注解驱动的命令类

**保留**：
- LanguageKeys 用于 i18n 集成

### Cloud 命令架构分层

```
基础层：LanguageKeys（i18n 常量）
逻辑层：业务服务（如 TransferManager, PlayerService）
事务层：注解命令类（AdminCommands, UserCommands）
主插件层：CommandRegistry 统一注册
```

> 📖 完整命令系统文档见 [docs/core/commands.md](../core/commands.md)

## 相关文档

- [插件开发规范](plugin-conventions.md)
- [迁移记录](migration-records.md)（各插件迁移状态）
