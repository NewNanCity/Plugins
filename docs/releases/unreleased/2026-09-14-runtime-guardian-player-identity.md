---
type: fix
scope: plugin
audience: developer
summary: Guardian 权限和保护名单改用稳定玩家 ID并拒绝歧义查询
breaking: false
demo_ready: false
tests: Kotlin standalone TownPermissions assertions (6 invariants); Gradle build blocked by Paper repository TLS handshake
artifacts:
  - plugins/guardian/src/main/kotlin/city/newnan/guardian/gui/Town.kt
  - plugins/guardian/src/main/kotlin/city/newnan/guardian/gui/TownPermissions.kt
  - plugins/guardian/src/main/kotlin/city/newnan/guardian/modules/PlayerManager.kt
  - plugins/guardian/src/test/kotlin/city/newnan/guardian/gui/TownPermissionsTest.kt
---

## What changed

Guardian 的城镇编辑权限、玩家保护名单和响应关联使用稳定玩家 ID；本地名称缓存已移除，等价名称解析遇到未知或多条记录时失败关闭。

## Why it matters

玩家改名、大小写变化或名称复用不会把权限和受保护对象指向旧记录，名称歧义也不会继续执行绑定、封禁或城镇写入。

## Demo posture / limitations

纯权限不变量已通过 6 个断言；完整 Gradle/Paper 构建因官方依赖仓库 TLS 握手失败未完成，生产 JAR 和服务器参数尚未回读，本条目不代表已发布。
