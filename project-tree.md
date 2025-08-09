# Project Tree

建议与 README.md 一起阅读。本文件仅保留精简树形结构与关键说明，详细请参阅 docs/ 与各模块 README。

```
NewNanPlugins/
├── build.gradle.kts                # 根构建脚本（Kotlin DSL）
├── settings.gradle.kts             # 多项目设置（包含 modules/ 与 plugins/）
├── buildSrc/                       # 构建约定与版本管理（Versions.kt / Dependencies.kt）
│   └── src/main/kotlin/
│       ├── Versions.kt
│       ├── Dependencies.kt
│       └── newnancity-plugin.gradle.kts
├── docs/                           # 文档中心（core/config/gui/i18n/network/...）
├── modules/                        # 可选功能模块（均依赖 core）
│   ├── core/                       # 核心框架（BasePlugin/BaseModule/调度器/事件/消息/缓存等）
│   ├── config/                     # 多格式配置管理（Jackson）
│   ├── gui/                        # 现代 GUI 框架（Session/Page/Component）
│   ├── i18n/                       # 国际化模块
│   └── network/                    # Ktor 网络工具
├── plugins/                        # 插件集合（按需依赖 modules/*）
│   ├── external-book/              # 外部书籍管理（GUI + i18n）
│   ├── foundation/                 # 基础/经济集成（XConomy 监听）
│   ├── railarea/                   # 铁路区域/站点（仅加载已加载世界；动态管理）
│   ├── rail-express/               # 铁路物流/快递
│   ├── mcron/                      # Cron 调度
│   ├── tpa/                        # 传送请求
│   ├── feefly/                     # 付费飞行
│   ├── deathcost/                  # 死亡惩罚
│   ├── better-command-block/
│   ├── mc-patch/
│   ├── powertools/
│   ├── createarea/
│   ├── guardian/
│   ├── newnanmain/
│   └── dynamiceconomy/
├── CLAUDE.md                       # 针对 AI 协作的说明（构建/架构要点）
├── README.md                       # 仓库总览与上手指南
└── gradle.properties               # 全局 Gradle 属性
```

说明：
- 子项目清单以 settings.gradle.kts 为准；本树定期更新
- 统一使用构建约定插件（newnancity-plugin）管理仓库级配置
- 全部插件与模块统一依赖版本（见 buildSrc/Versions.kt）

