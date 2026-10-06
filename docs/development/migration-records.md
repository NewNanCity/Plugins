# 插件迁移记录

记录各插件从旧版 violet 框架迁移到现代化架构的状态。

> 📖 迁移的标准模式和技术要点见 [migration-guide.md](migration-guide.md)

## 迁移状态总览

| 插件               | 状态   | 命令框架 | 备注                        |
| ------------------ | ------ | -------- | --------------------------- |
| ExternalBook       | ✅ 完成 | Cloud    | BaseCommandRegistry 架构    |
| Foundation         | ✅ 完成 | Cloud    | BaseCommandRegistry 架构    |
| MCron              | ✅ 完成 | Cloud    | BaseCommandRegistry 架构    |
| DeathCost          | ✅ 完成 | Cloud    | 2025-07-29                  |
| Guardian           | ✅ 完成 | Cloud    | 2025-07-30，Exposed ORM     |
| BetterCommandBlock | ✅ 完成 | Cloud    | 2025-08-04，完全重写        |
| PowerTools         | ✅ 完成 | Cloud    | 头颅获取功能                |
| CreateArea         | ✅ 完成 | Cloud    | WorldEdit/Dynmap/Vault 集成 |
| NewNanMain         | ✅ 完成 | Cloud    | GUI1 模块，前缀/传送系统    |
| DynamicEconomy     | ✅ 完成 | Cloud    | 架构重构，核心算法一致      |

## 插件详情

### Guardian
- 四层架构完全重构
- 从 Ktorm 迁移到 Exposed ORM
- 权限系统保持向后兼容

### BetterCommandBlock
- 从 violet 完全重写
- 双重防火墙架构（前缀树 + 黑名单 fallback）
- 扩展命令：pick、scoreboard random、execute

### NewNanMain
- GUI1 模块现代化界面（主菜单、传送、前缀管理、管理员）
- 聊天输入编辑系统（YesInput、TeleportInput、PrefixInput、NamespaceInput）
- 完整 CRUD 操作和数据持久化

### DynamicEconomy
- 核心算法完全一致（时间衰减γ、商品价格公式、货币指数）
- 从单体重构为 WealthManager、EconomyManager、CommodityManager
- 命令从 3 个扩展到 9 个
- 主命令 `dynamicaleconomy|de`，兼容别名 `economy|eco`
