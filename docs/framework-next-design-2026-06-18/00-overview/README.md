# Overview

本目录放框架整体设计、需求反推、性能目标和路线。这里回答“为什么要这样设计”和“整体如何拼起来”，不写单个 Runtime 的完整 Interface。

## 文档

- [unified-framework-design.md](unified-framework-design.md): 主设计入口。
- [api-surface-map.md](api-surface-map.md): 当前 core/modules 暴露能力到下一代 Runtime 的归属图。
- [current-decisions.md](current-decisions.md): 已经拍板的设计决策。
- [decision-recommendations.md](decision-recommendations.md): 2026-06-19 拍板结果和框架推荐方案。
- [inferred-requirements.md](inferred-requirements.md): 从现有代码反推出的真实需求。
- [performance-and-cost.md](performance-and-cost.md): 性能、运行成本、缓存和 GC 目标。
- [iteration-roadmap.md](iteration-roadmap.md): 文档和草案拆分路线。

## 使用规则

- 新的整体决策写入 `current-decisions.md`。
- 集中拍板和框架推荐方案写入 `decision-recommendations.md`。
- 当前 public Interface 归属和开发入口写入 `api-surface-map.md`。
- 跨 Runtime 的设计原则写入 `unified-framework-design.md`。
- 单个 Runtime 的 Interface 草案写入 `../02-runtime/`。
- 开发阶段计划写入 `../03-implementation/`。
- 调研来源和对外传播素材写入 `../01-research/`。
