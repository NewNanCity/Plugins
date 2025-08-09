# 插件文档

这里包含所有插件的详细文档。每个插件都有独立的文档文件夹，包含完整的使用指南。

## 🚄 铁路系统插件

### [RailArea](railarea/) - 现代化铁路区域管理插件
- **功能**: 智能区域检测、自动矿车系统、音效管理、可视化工具
- **特色**: 八叉树算法、GUI界面、多世界支持
- **快速开始**: [安装指南](railarea/quick-start.md) | [配置说明](railarea/configuration.md)
- **文档**: [完整文档](railarea/README.md)

### [RailExpress](rail-express/) - 矿车速度控制插件
- **功能**: 根据方块类型控制矿车速度
- **特色**: 多世界配置、实时重载、权限控制
- **快速开始**: [安装指南](rail-express/quick-start.md) | [速度控制](rail-express/speed-control.md)
- **文档**: [完整文档](rail-express/README.md)

## ⏰ 自动化插件

### [MCron](mcron/) - 现代化定时任务插件
- **功能**: Cron表达式定时任务、玩家任务、服务器事件任务
- **特色**: 秒级精度、异步执行、国际化支持
- **快速开始**: [安装指南](mcron/quick-start.md) | [Cron表达式](mcron/cron-expressions.md)
- **文档**: [完整文档](mcron/README.md)

## 🎮 玩家功能插件

### [TPA](tpa/) - 传送请求插件
- **功能**: 玩家传送请求系统
- **特色**: 现代化界面、权限控制
- **快速开始**: [使用指南](tpa/index.md)
- **文档**: [完整文档](tpa/README.md)

### [FeeFly](feefly/) - 付费飞行插件
- **功能**: 基于经济系统的付费飞行功能
- **特色**: Vault集成、状态持久化、实时监控、多语言支持
- **快速开始**: [快速参考](feefly/quick-reference.md) | [配置示例](feefly/configuration-examples.md)
- **文档**: [完整文档](feefly/README.md) | [文档导航](feefly/index.md)

## 📚 文档结构说明

每个插件的文档文件夹都遵循统一的结构：

```
插件名称/
├── README.md           # 文档导航和概览
├── intro.md           # 插件介绍（5分钟快速了解）
├── quick-start.md     # 快速开始（安装配置）
├── configuration.md   # 配置指南（详细配置）
├── commands.md        # 命令系统（完整命令参考）
├── permissions.md     # 权限系统（权限配置）
├── api-reference.md   # API参考（开发者文档）
├── troubleshooting.md # 故障排除（常见问题）
├── best-practices.md  # 最佳实践（使用建议）
└── 其他功能文档...
```

## 🚀 快速导航

### 新用户推荐路径
1. 选择需要的插件
2. 阅读插件介绍 (`intro.md`)
3. 按照快速开始指南安装 (`quick-start.md`)
4. 根据需要查看详细配置 (`configuration.md`)

### 开发者推荐路径
1. 查看API参考文档 (`api-reference.md`)
2. 了解插件架构和设计
3. 参考最佳实践进行集成

### 运维人员推荐路径
1. 查看配置指南 (`configuration.md`)
2. 了解故障排除方法 (`troubleshooting.md`)
3. 参考最佳实践进行优化 (`best-practices.md`)

## 🔗 相关资源

- [项目主页](../../README.md) - 项目总体介绍
- [模块文档](../README.md) - 核心模块文档
- [开发指南](../API-Design-Guidelines.md) - API设计指南

## 📝 文档贡献

如果您发现文档中的错误或希望改进文档，欢迎：

1. 提交Issue报告问题
2. 提交Pull Request改进文档
3. 在Discord或论坛中提供反馈

## 📊 插件兼容性

| 插件        | Minecraft版本 | 服务器类型 | Java版本 | 依赖              |
| ----------- | ------------- | ---------- | -------- | ----------------- |
| RailArea    | 1.20.1+       | Paper      | 21+      | CommandAPI        |
| RailExpress | 1.21+         | Paper      | 21+      | 无                |
| MCron       | 1.21+         | Paper      | 21+      | cron-utils        |
| TPA         | 1.21+         | Paper      | 21+      | 无                |
| FeeFly      | 1.20.1+       | Paper      | 21+      | CommandAPI, Vault |

## 🎯 插件组合推荐

### 完整铁路系统
- **RailArea** + **RailExpress** + **MCron**
- 提供完整的铁路管理、速度控制和定时任务功能

### 服务器自动化
- **MCron** + 其他管理插件
- 实现服务器运维自动化

### 玩家体验增强
- **TPA** + **FeeFly** + **RailArea** + 其他便民插件
- 提升玩家游戏体验和便民功能

### 经济系统增强
- **FeeFly** + 经济插件 + 商店插件
- 构建完整的服务器经济生态
