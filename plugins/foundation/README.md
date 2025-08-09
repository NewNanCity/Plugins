# Foundation 基金会管理插件

基于 NewNanPlugins 框架开发的现代化基金会管理系统，提供完整的捐赠记录、拨款管理和排行榜功能。

## � 核心功能

Foundation 插件提供以下核心功能：

### � 捐赠管理
- **主动捐赠**：玩家通过指令主动向基金会捐赠
- **被动捐赠**：自动检测通过其他插件向基金会的转账
- **智能匹配**：精确匹配玩家扣费和基金会入账事件

### 💰 拨款系统
- **资金分配**：管理员可向玩家分配基金会资金
- **拨款记录**：详细记录每次拨款的时间、金额和原因
- **余额管理**：实时查看基金会账户余额和统计

### 📊 排行榜系统
- **分页显示**：GUI 界面展示捐赠排行榜
- **详细统计**：区分主动捐赠和被动捐赠金额
- **实时更新**：支持数据实时加载和更新

### 🔧 技术特性
- **多插件支持**：兼容 EssentialsX 和 XConomy 经济插件
- **灵活存储**：支持 CSV 文件和数据库两种存储方式
- **事件检测**：自动监听经济插件的余额变化事件
- **国际化**：完整的中英文双语支持

## 🚀 快速开始

### 前置要求
- Minecraft 1.16+
- Java 17+
- Vault 插件
- 经济插件（EssentialsX 或 XConomy）

### 基础配置
1. 安装插件到 `plugins` 目录
2. 重启服务器
3. 编辑 `plugins/Foundation/config.yml`：
   ```yaml
   target_account: "FoundationAccount"  # 设置基金会账户名
   transfer_detection:
     enabled: true                      # 启用自动转账检测
   ```
4. 重载配置：`/foundation reload`

### 基础使用
```bash
# 玩家捐赠
/foundation donate 100

# 查看排行榜
/foundation top

# 管理员查看余额
/foundation balance

# 管理员拨款
/foundation allocate Steve 500 建设奖励
```

## � 指令系统

### 主指令
- **基础指令**：`/foundation` 或 `/fund`
- **帮助指令**：`/foundation help [查询]`

### 用户指令

#### 捐赠指令
```bash
/foundation donate <金额> [玩家]
```
- **功能**：向基金会捐赠指定金额
- **权限**：`foundation.donate`（自己捐赠）、`foundation.donate.other`（代他人捐赠）
- **示例**：
  ```bash
  /fund donate 100          # 捐赠 100 元
  /fund donate 50 Steve     # 代 Steve 捐赠 50 元
  ```

#### 排行榜指令
```bash
/foundation top
```
- **功能**：打开捐赠排行榜 GUI 界面
- **权限**：`foundation.top`

### 管理员指令

#### 余额查询
```bash
/foundation balance
```
- **功能**：查看基金会账户余额和统计信息
- **权限**：`foundation.balance`

#### 拨款指令
```bash
/foundation allocate <玩家> <金额> <原因>
```
- **功能**：从基金会向玩家拨款
- **权限**：`foundation.allocate`
- **示例**：
  ```bash
  /fund allocate Steve 500 建设奖励
  /fund allocate Alex 200 活动奖金
  ```

#### 查询指令
```bash
/foundation query <玩家>
```
- **功能**：查询指定玩家的捐赠记录
- **权限**：`foundation.query`

#### 重载指令
```bash
/foundation reload
```
- **功能**：重载插件配置和语言文件
- **权限**：`foundation.reload`

## 🔐 权限系统

### 用户权限
| 权限节点            | 描述                 | 默认权限 |
| ------------------- | -------------------- | -------- |
| `foundation.donate` | 允许玩家捐赠给基金会 | 所有玩家 |
| `foundation.top`    | 允许查看捐赠排行榜   | 所有玩家 |

### 管理员权限
| 权限节点                  | 描述                 | 默认权限 |
| ------------------------- | -------------------- | -------- |
| `foundation.balance`      | 查看基金会余额和统计 | OP       |
| `foundation.allocate`     | 向玩家拨款           | OP       |
| `foundation.query`        | 查询玩家捐赠记录     | OP       |
| `foundation.reload`       | 重载插件配置         | OP       |
| `foundation.donate.other` | 代其他玩家捐赠       | OP       |

## ⚙️ 配置文件

### 完整配置示例
```yaml
plugin:
  name: "Foundation"
  version: "2.0.0"

# 基金会目标账户名称
target_account: "FoundationAccount"

# 语言设置
locale: "zh_CN"

# 转账检测配置
transfer_detection:
  enabled: true
  expire_milliseconds: 5000

# 数据存储配置
data_storage:
  mode: "file"
  file:
    transfer_records: "data/transfer_records.csv"
    allocation_logs: "data/allocation_logs.csv"
  database:
    url: "jdbc:mysql://localhost:3306/foundation"
    username: "foundation_user"
    password: "password"
    pool_size: 10
```

### 配置说明
- **target_account**：基金会账户名称，必须设置
- **transfer_detection.enabled**：是否启用自动转账检测
- **transfer_detection.expire_milliseconds**：转账匹配超时时间
- **data_storage.mode**：数据存储模式（file 或 database）

## 🎮 GUI 界面

### 排行榜界面
Foundation 提供了现代化的 GUI 排行榜界面：

- **分页显示**：支持大量数据的分页浏览
- **玩家头像**：使用玩家头像作为图标
- **详细信息**：悬停显示捐赠详情
- **统计数据**：实时显示全服统计
- **交互功能**：点击查看更多信息

### 界面功能
- 前三名特殊标记（附魔效果）
- 当前玩家高亮显示
- 实时数据更新
- 优雅的用户体验

## 🔧 技术特性

### 自动转账检测
- **事件监听**：监听 EssentialsX 和 XConomy 的余额变化事件
- **智能匹配**：通过时间窗口匹配玩家扣费和基金会入账
- **重复过滤**：防止同一笔转账被重复记录
- **异步处理**：所有检测逻辑在异步线程中执行

### 数据存储
- **双重模式**：支持 CSV 文件和数据库存储
- **热切换**：可在运行时切换存储模式
- **数据同步**：支持多服务器数据同步
- **备份恢复**：完整的数据备份和恢复功能

### 性能优化
- **轻量设计**：最小化对服务器性能的影响
- **异步操作**：所有耗时操作都在异步线程中执行
- **智能缓存**：合理的数据缓存机制
- **连接池**：数据库连接池优化

## 🐛 故障排除

### 常见问题

#### 转账检测不工作
1. 检查 `transfer_detection.enabled` 是否为 `true`
2. 确认目标账户已正确设置
3. 检查经济插件版本兼容性

#### 数据库连接失败
1. 验证数据库连接信息
2. 检查数据库服务状态
3. 确认网络连接正常

#### 权限问题
1. 检查权限插件配置
2. 确认权限节点正确
3. 重载权限配置

### 日志分析
插件提供详细的日志信息，帮助诊断问题：
- 转账检测过程日志
- 数据库操作日志
- 权限检查日志
- 错误堆栈信息

## 📚 相关文档

- [详细使用指南](../../docs/plugins/foundation/README.md)
- [NewNanPlugins 框架文档](../../docs/core/)
- [GUI 模块使用指南](../../docs/gui/)
- [国际化模块说明](../../docs/i18n/)

---

**开发者**：NewNanCity
**版本**：2.0.0
**许可证**：MIT License
**支持版本**：Minecraft 1.16+
---

Foundation 插件已完成分析和文档更新！基于实际代码分析，该插件提供了完整的基金会管理系统，包括捐赠记录、拨款管理、排行榜展示等功能。

**主要特性**：
- 自动转账检测（支持 EssentialsX 和 XConomy）
- 完整的捐赠和拨款管理
- 现代化的 GUI 排行榜界面
- 灵活的数据存储（CSV/数据库）
- 国际化支持

详细使用说明请参考：[Foundation 插件详细文档](../../docs/plugins/foundation/README.md)
