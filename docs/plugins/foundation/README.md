# Foundation 插件使用指南

Foundation 是一个基于 NewNanPlugins 框架开发的 Minecraft 基金会管理插件。它提供了完整的基金会资金管理系统，包括捐赠记录、拨款分配、排行榜展示等功能。

## 📋 目录

- [核心功能](#核心功能)
- [安装配置](#安装配置)
- [权限系统](#权限系统)
- [指令系统](#指令系统)
- [配置说明](#配置说明)
- [功能详解](#功能详解)
- [GUI 界面](#gui-界面)
- [故障排除](#故障排除)

## 🚀 核心功能

Foundation 插件提供以下核心功能：

### 资金管理
- **捐赠记录**：自动记录玩家主动捐赠和被动捐赠
- **拨款分配**：管理员可以向玩家分配基金会资金
- **余额查询**：查看基金会当前余额和统计信息
- **排行榜**：展示捐赠排行榜，支持分页浏览

### 自动转账检测
- **EssentialsX 集成**：自动检测通过 EssentialsX 进行的转账
- **XConomy 集成**：自动检测通过 XConomy 进行的转账
- **智能分类**：自动区分主动捐赠和被动捐赠
- **重复过滤**：防止重复记录同一笔转账

### 数据存储
- **灵活存储**：支持 CSV 文件和数据库两种存储方式
- **数据同步**：支持多服务器数据同步
- **热重载**：支持配置热重载，无需重启服务器

## 🛠 安装配置

### 前置条件
- Minecraft 服务器版本：1.16+
- Java 版本：17+
- 必需插件：
  - Vault (经济系统接口)
  - 经济插件（如 EssentialsX 或 XConomy）
- 可选插件：
  - LuckPerms (权限管理)

### 安装步骤
1. 下载 Foundation 插件文件
2. 将插件放入服务器的 `plugins` 目录
3. 重启服务器或使用 `/plugman load Foundation`
4. 根据需要修改配置文件
5. 设置基金会目标账户

### 首次配置
1. 编辑 `plugins/Foundation/config.yml` 文件
2. 设置基金会目标账户
3. 配置数据存储方式
4. 重载配置：`/foundation reload`

## 🔐 权限系统

Foundation 插件使用细粒度的权限控制系统：

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

### 权限配置示例

#### LuckPerms 配置
```bash
# 给予所有玩家基础权限
lp group default permission set foundation.donate true
lp group default permission set foundation.top true

# 给予管理员完整权限
lp group admin permission set foundation.* true
```

#### 权限插件配置
```yaml
groups:
  default:
    permissions:
      - foundation.donate
      - foundation.top
  admin:
    permissions:
      - foundation.*
```

## 💬 指令系统

Foundation 插件使用基于 Cloud 框架的现代化指令系统，支持自动补全和参数验证。

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
- **参数**：
  - `金额`：捐赠金额，必须为正数
  - `玩家`：可选，代其他玩家捐赠时指定目标玩家
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
- **特点**：
  - 分页显示所有捐赠记录
  - 高亮显示当前玩家
  - 点击查看详细信息
  - 实时统计数据

### 管理员指令

#### 余额查询
```bash
/foundation balance
```
- **功能**：查看基金会账户余额和统计信息
- **权限**：`foundation.balance`
- **显示内容**：
  - 基金会账户名称
  - 当前余额
  - 主动捐赠总额
  - 被动捐赠总额

#### 拨款指令
```bash
/foundation allocate <玩家> <金额> <原因>
```
- **功能**：从基金会向玩家拨款
- **权限**：`foundation.allocate`
- **参数**：
  - `玩家`：拨款目标玩家
  - `金额`：拨款金额，必须为正数
  - `原因`：拨款原因说明
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
- **显示内容**：
  - 玩家主动捐赠总额
  - 玩家被动捐赠总额
  - 捐赠总计

#### 重载指令
```bash
/foundation reload
```
- **功能**：重载插件配置和语言文件
- **权限**：`foundation.reload`
- **特点**：
  - 热重载，无需重启服务器
  - 自动重新连接数据库
  - 重新注册事件监听器

## ⚙️ 配置说明

Foundation 插件的配置文件位于 `plugins/Foundation/config.yml`：

### 基础配置
```yaml
# 插件元信息
plugin:
  name: "Foundation"
  version: "2.0.0"

# 目标账户设置
target_account: "FoundationAccount"  # 基金会账户名称

# 国际化设置
locale: "zh_CN"  # 语言设置，支持 zh_CN 和 en_US
```

### 转账检测配置
```yaml
transfer_detection:
  # 是否启用自动转账检测
  enabled: true

  # 转账匹配超时时间（毫秒）
  # 用于匹配玩家扣费和账户入账事件
  expire_milliseconds: 5000
```

### 数据存储配置
```yaml
data_storage:
  # 存储模式：file（文件）或 database（数据库）
  mode: "file"

  # 文件存储设置
  file:
    # 转账记录文件路径
    transfer_records: "data/transfer_records.csv"
    # 拨款日志文件路径
    allocation_logs: "data/allocation_logs.csv"

  # 数据库存储设置
  database:
    # 数据库连接 URL
    url: "jdbc:mysql://localhost:3306/foundation"
    # 数据库用户名
    username: "foundation_user"
    # 数据库密码
    password: "password"
    # 连接池设置
    pool_size: 10
```

### 完整配置示例
```yaml
plugin:
  name: "Foundation"
  version: "2.0.0"

target_account: "FoundationAccount"
locale: "zh_CN"

transfer_detection:
  enabled: true
  expire_milliseconds: 5000

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

## 🎯 功能详解

### 捐赠系统

#### 主动捐赠
玩家通过 `/foundation donate` 指令主动向基金会捐赠：
1. 验证玩家余额是否足够
2. 从玩家账户扣除金额
3. 向基金会账户存入金额
4. 记录为主动捐赠
5. 发送成功通知

#### 被动捐赠
通过其他插件（如死亡扣费）自动向基金会转账：
1. 监听经济插件的余额变化事件
2. 检测是否为向基金会的转账
3. 自动分类为被动捐赠
4. 避免重复记录

#### 转账检测机制
```
玩家余额减少 + 基金会余额增加（同金额）= 被动捐赠
仅基金会余额增加 = 未匹配的转账
仅玩家余额减少 = 未匹配的扣费
```

### 拨款系统

#### 拨款流程
1. 验证拨款金额和原因
2. 检查基金会余额是否足够
3. 从基金会账户扣除金额
4. 向目标玩家存入金额
5. 记录拨款日志
6. 通知相关人员

#### 拨款日志
每次拨款都会记录详细信息：
- 拨款时间
- 操作员（玩家/控制台/命令方块）
- 目标玩家
- 拨款金额
- 拨款原因

### 排行榜系统

#### 排名计算
- **主动捐赠**：通过指令主动捐赠的总金额
- **被动捐赠**：通过其他方式被扣费转入基金会的总金额
- **总捐赠**：主动捐赠 + 被动捐赠
- **排名**：按总捐赠金额降序排列

#### 特殊显示
- 前三名有特殊标记（附魔效果）
- 当前玩家高亮显示
- 支持分页浏览
- 点击查看详细统计

## 🖥 GUI 界面

### 排行榜 GUI

#### 界面布局
```
[关闭]                                [统计]
┌─────────────────────────────────────┐
│  [玩家头像]  [玩家头像]  [玩家头像]  │
│  排名 #1     排名 #2     排名 #3    │
│                                     │
│  [玩家头像]  [玩家头像]  [玩家头像]  │
│  排名 #4     排名 #5     排名 #6    │
└─────────────────────────────────────┘
[上一页]                          [下一页]
```

#### 功能特性
- **头像显示**：使用玩家头像作为物品图标
- **信息展示**：悬停查看详细捐赠信息
- **交互功能**：点击头像查看详细统计
- **分页导航**：支持前后翻页
- **实时更新**：数据实时从数据库加载

#### 物品信息
每个玩家头像显示：
- 玩家名称和排名
- 主动捐赠金额
- 被动捐赠金额
- 总捐赠金额
- 点击提示

### 统计按钮
点击统计按钮显示全服统计：
- 总参与人数
- 主动捐赠总额
- 被动捐赠总额
- 捐赠总计
- 平均捐赠金额

## 🐛 故障排除

### 常见问题

#### 1. 插件无法加载
**症状**：服务器启动时 Foundation 插件加载失败

**解决方法**：
1. 检查 Java 版本是否为 17+
2. 确认 Vault 插件已正确安装
3. 检查是否有经济插件（EssentialsX/XConomy）
4. 查看服务器日志中的错误信息

#### 2. 转账检测不工作
**症状**：玩家被扣费但未记录为捐赠

**解决方法**：
1. 检查配置文件中 `transfer_detection.enabled` 是否为 `true`
2. 确认目标账户已正确设置
3. 检查经济插件版本兼容性
4. 查看插件日志了解检测过程

#### 3. 数据库连接失败
**症状**：使用数据库模式时无法连接

**解决方法**：
1. 检查数据库服务是否正常运行
2. 验证连接信息（URL、用户名、密码）
3. 确认数据库表已正确创建
4. 检查网络连接和防火墙设置

#### 4. 权限问题
**症状**：玩家无法使用某些指令

**解决方法**：
1. 检查权限插件配置
2. 确认权限节点拼写正确
3. 重载权限插件配置
4. 测试权限继承关系

### 日志分析

#### 启用调试日志
在配置文件中添加：
```yaml
debug: true
```

#### 重要日志位置
- **服务器日志**：`logs/latest.log`
- **插件日志**：通过服务器日志查看
- **转账检测日志**：包含详细的事件处理信息

#### 日志关键信息
- 转账检测事件的详细处理过程
- 数据库连接和查询状态
- 权限检查结果
- 错误堆栈信息

### 性能优化

#### 数据库优化
1. 为经常查询的字段添加索引
2. 定期清理过期的日志数据
3. 调整连接池大小
4. 使用 SSD 存储提高 I/O 性能

#### 内存优化
1. 调整转账记录的过期时间
2. 限制排行榜加载的记录数量
3. 定期清理内存中的临时数据

## 📚 开发者信息

- **开发者**：NewNanCity
- **版本**：2.0.0
- **开源协议**：MIT License
- **支持版本**：Minecraft 1.16+
- **依赖框架**：NewNanPlugins Core

## 🔗 相关链接

- [NewNanPlugins 框架文档](../core/)
- [GUI 模块使用指南](../gui/)
- [国际化模块说明](../i18n/)
- [配置系统文档](../config/)

---

*本文档更新时间：2024年*
