---
type: fix
scope: plugin
audience: developer
summary: 修复 Guardian 鉴权、TPS 和 session 长度兼容性
breaking: false
demo_ready: false
tests: .\gradlew.bat :plugins:guardian:test :plugins:guardian:shadowJar --no-daemon -I .local-gradle-mirror-20260923.gradle --rerun-tasks
artifacts:
  - plugins/guardian/build.gradle.kts
  - plugins/guardian/src/main/kotlin/city/newnan/guardian/sdk/http/HttpClient.kt
  - plugins/guardian/src/main/kotlin/city/newnan/guardian/modules/HeartbeatManager.kt
  - plugins/guardian/src/test/kotlin/city/newnan/guardian/sdk/http/HttpClientAuthenticationTest.kt
  - plugins/guardian/src/test/kotlin/city/newnan/guardian/modules/HeartbeatManagerTest.kt

---

## What changed

Guardian `1.0.6` 的 HTTP SDK 只发送 `X-API-Token`，新增回归测试锁定单一鉴权头；session ID 遵循 canonical 的 32–64 字符范围；心跳把有限 TPS 限制到 `[0, 20]`，对 `NaN` 和无穷值省略字段。模块显式启用测试并补齐 JUnit Platform launcher，避免根工程默认禁用测试造成假绿。

## Why it matters

生产旧包同时发送 `Authorization` 与 `X-API-Token`，在后端严格鉴权下全部得到 401；Paper 启动期间的异常 TPS 又会使合法心跳得到 400。修复后生产 session、heartbeat 和 player validate 连续返回 2xx。

## Demo posture / limitations

本条已在 Home Server 的主服实例加载并完成一个以上心跳周期的生产验收；`1.0.4` 和 `1.0.5` 包保留为服务器回滚副本。其他插件的既有告警、全服双服顶号和长期性能压测不在本条范围内。
