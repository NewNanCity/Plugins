# Guardian 玩家身份与城镇权限

Guardian 通过 NewNanManager 的稳定玩家 ID 关联响应、城镇成员和权限判断。名称只作为查询入口，不能作为权限所有权的比较键；查询返回歧义或失败时，调用方必须停止写操作。

## 本轮变更

- 城镇权限使用后端返回的 `leaderId` 与当前玩家 ID，保留显式管理员覆盖。
- 玩家名称查询移除易过期的本地名称缓存，避免改名或名称复用命中旧 ID。
- 保护名单按后端等价名称解析，未知或多结果时关闭管理写操作。

## 验证

`Guardian-1.0.6.jar` 已通过完整 Gradle Shadow 构建和 5 个 JUnit 测试（鉴权头、session 长度、TPS 边界、权限不变量），SHA-256 为 `25b35f779e1612b550cbe5230c00739a1825d040f3c686c0ea6bdcc1f8560069`，并从 Home Server 事实源回读后加载到主服。生产 session、heartbeat 与 player validate 连续返回 2xx。服务器上的 `1.0.5` 和原始 `1.0.4` 包仍作为回滚副本保留；authlib-injector、online-mode 和跨服顶号演练仍需单独验收。
