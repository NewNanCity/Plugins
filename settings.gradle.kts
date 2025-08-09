rootProject.name = "NewNanPlugins"

// 启用并行构建
gradle.startParameter.isParallelProjectExecutionEnabled = true

// 包含子项目
include(
    // 核心模块
    ":core",

    // 功能模块
    ":modules:config",
    ":modules:network",
    ":modules:gui",
    ":modules:i18n",

    // 插件项目
    ":plugins:tpa",
    ":plugins:mcron",
    ":plugins:rail-express",
    ":plugins:railarea",
    ":plugins:feefly",
    ":plugins:deathcost",
    ":plugins:foundation",
    ":plugins:external-book",
    ":plugins:guardian",
    ":plugins:better-command-block",
    ":plugins:mc-patch",
    ":plugins:powertools",
    ":plugins:createarea",
    ":plugins:newnanmain",
    ":plugins:dynamiceconomy"
)

// 设置子项目目录
project(":core").projectDir = file("modules/core")

// 功能模块目录
project(":modules:i18n").projectDir = file("modules/i18n")
project(":modules:config").projectDir = file("modules/config")
project(":modules:network").projectDir = file("modules/network")
project(":modules:gui").projectDir = file("modules/gui")

// 插件目录
project(":plugins:tpa").projectDir = file("plugins/tpa")
project(":plugins:mcron").projectDir = file("plugins/mcron")
project(":plugins:rail-express").projectDir = file("plugins/rail-express")
project(":plugins:railarea").projectDir = file("plugins/railarea")
project(":plugins:feefly").projectDir = file("plugins/feefly")
project(":plugins:deathcost").projectDir = file("plugins/deathcost")
project(":plugins:foundation").projectDir = file("plugins/foundation")
project(":plugins:external-book").projectDir = file("plugins/external-book")
project(":plugins:guardian").projectDir = file("plugins/guardian")
project(":plugins:better-command-block").projectDir = file("plugins/better-command-block")
project(":plugins:mc-patch").projectDir = file("plugins/mc-patch")
project(":plugins:powertools").projectDir = file("plugins/powertools")
project(":plugins:createarea").projectDir = file("plugins/createarea")
