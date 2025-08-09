package city.newnan.gui.dsl

import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.util.ItemBuilder
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * GUI物品DSL扩展
 *
 * 为GUI组件提供便利的物品创建DSL方法，
 * 集成core模块的ItemBuilder和GUI专用的ItemUtil。
 */

/**
 * 创建物品构建器
 */
fun BaseComponent<*>.item(material: Material): ItemBuilder =
    guiManager.itemUtil.builder(material)

/**
 * 创建物品构建器（从现有物品）
 */
fun BaseComponent<*>.item(itemStack: ItemStack): ItemBuilder =
    guiManager.itemUtil.builder(itemStack)

/**
 * 创建物品构建器并应用配置
 */
fun BaseComponent<*>.item(material: Material, builder: ItemBuilder.() -> Unit): ItemStack =
    guiManager.itemUtil.builder(material, builder).build()

/**
 * 创建物品构建器并应用配置（从现有物品）
 */
fun BaseComponent<*>.item(itemStack: ItemStack, builder: ItemBuilder.() -> Unit): ItemStack =
    guiManager.itemUtil.builder(itemStack, builder).build()

/**
 * 创建简单物品
 */
fun BaseComponent<*>.simpleItem(material: Material, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.create(material, name, lore, function)

/**
 * 创建简单物品（Component版本）
 */
fun BaseComponent<*>.simpleItem(material: Material, name: Component? = null, lore: List<Component>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.create(material, name, lore, function)

// GUI专用物品DSL

/**
 * 创建分隔符物品
 */
fun BaseComponent<*>.separator(material: Material = Material.GRAY_STAINED_GLASS_PANE, name: String = " ", function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.separator(material, name, function)

/**
 * 创建边框物品
 */
fun BaseComponent<*>.border(material: Material = Material.BLACK_STAINED_GLASS_PANE, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.border(material, function)

/**
 * 创建按钮物品
 */
fun BaseComponent<*>.button(material: Material, name: String, lore: List<String> = emptyList(), function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.button(material, name, lore, function)

/**
 * 创建信息物品
 */
fun BaseComponent<*>.info(material: Material, name: String, lore: List<String> = emptyList(), function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.info(material, name, lore, function)

/**
 * 创建警告物品
 */
fun BaseComponent<*>.warning(material: Material = Material.BARRIER, name: String, lore: List<String> = emptyList(), function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.warning(material, name, lore, function)

/**
 * 创建成功物品
 */
fun BaseComponent<*>.success(material: Material = Material.LIME_DYE, name: String, lore: List<String> = emptyList(), function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.success(material, name, lore, function)

/**
 * 创建导航按钮
 */
fun BaseComponent<*>.navigation(material: Material, name: String, description: String? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.navigation(material, name, description, function)

/**
 * 创建翻页按钮
 */
fun BaseComponent<*>.pageButton(isNext: Boolean, currentPage: Int, totalPages: Int, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.pageButton(isNext, currentPage, totalPages, function)

/**
 * 创建下一页按钮
 */
fun BaseComponent<*>.nextPageButton(currentPage: Int, totalPages: Int, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.pageButton(true, currentPage, totalPages, function)

/**
 * 创建上一页按钮
 */
fun BaseComponent<*>.previousPageButton(currentPage: Int, totalPages: Int, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.pageButton(false, currentPage, totalPages, function)

/**
 * 创建关闭按钮
 */
fun BaseComponent<*>.closeButton(name: String = "关闭", function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.closeButton(name, function)

/**
 * 创建返回按钮
 */
fun BaseComponent<*>.backButton(name: String = "返回", function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.backButton(name, function)

/**
 * 创建确认按钮
 */
fun BaseComponent<*>.confirmButton(name: String = "确认", function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.confirmButton(name, function)

/**
 * 创建取消按钮
 */
fun BaseComponent<*>.cancelButton(name: String = "取消", function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.cancelButton(name, function)

/**
 * 创建加载中物品
 */
fun BaseComponent<*>.loading(name: String = "加载中...", function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.loading(name, function)

/**
 * 创建错误物品
 */
fun BaseComponent<*>.error(name: String = "加载失败", description: String = "点击重试", function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.error(name, description, function)

/**
 * 创建空槽位物品
 */
fun BaseComponent<*>.emptySlot(material: Material = Material.LIGHT_GRAY_STAINED_GLASS_PANE, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.emptySlot(material, function)

/**
 * 创建发光物品（从材料）
 */
fun BaseComponent<*>.glowing(material: Material, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.glowing(material, name, lore, function)

/**
 * 创建数量显示物品
 */
fun BaseComponent<*>.amount(material: Material, amount: Int, name: String? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.amount(material, amount, name, function)

/**
 * 创建头颅物品（使用OfflinePlayer方法设置头颅）
 */
fun BaseComponent<*>.skull(player: OfflinePlayer, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.skull(player, name, lore, function)

/**
 * 创建头颅物品（使用UUID方法设置头颅）
 */
fun BaseComponent<*>.skull(uuid: UUID, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.skull(uuid, name, lore, function)

/**
 * 创建头颅物品（使用玩家名称设置头颅）
 */
fun BaseComponent<*>.skull(playerName: String, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.skull(playerName, name, lore, function)

/**
 * 创建自定义头颅物品（Base64纹理）
 */
fun BaseComponent<*>.customSkull(base64Texture: String, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.customSkull(base64Texture, name, lore, function)

/**
 * 创建URL头颅物品（通过材质URL）
 */
fun BaseComponent<*>.urlSkull(textureUrlOrHash: String, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
    guiManager.itemUtil.urlSkull(textureUrlOrHash, name, lore, function)

// 常用物品组合
/**
 * 创建标准导航栏（包含返回、关闭等按钮）
 */
fun BaseComponent<*>.standardNavigation(): Map<String, ItemStack> {
    return mapOf(
        "back" to backButton(),
        "close" to closeButton(),
        "confirm" to confirmButton(),
        "cancel" to cancelButton()
    )
}

/**
 * 创建分页导航栏
 */
fun BaseComponent<*>.paginationNavigation(currentPage: Int, totalPages: Int): Map<String, ItemStack> {
    return mapOf(
        "previous" to previousPageButton(currentPage, totalPages),
        "next" to nextPageButton(currentPage, totalPages),
        "close" to closeButton()
    )
}

/**
 * 创建状态指示器
 */
fun BaseComponent<*>.statusIndicator(isSuccess: Boolean, successName: String = "成功", errorName: String = "失败", function: ItemBuilder.() -> Unit = {}): ItemStack {
    return if (isSuccess) {
        success(Material.LIME_DYE, successName, emptyList(), function)
    } else {
        warning(Material.RED_DYE, errorName, emptyList(), function)
    }
}

/**
 * 创建进度指示器
 */
fun BaseComponent<*>.progressIndicator(progress: Int, total: Int, name: String = "进度", function: ItemBuilder.() -> Unit = {}): ItemStack {
    val percentage = if (total > 0) (progress * 100) / total else 0
    val lore = listOf(
        "进度: $progress/$total",
        "完成度: $percentage%"
    )

    val material = when {
        percentage >= 100 -> Material.LIME_DYE
        percentage >= 50 -> Material.YELLOW_DYE
        else -> Material.RED_DYE
    }

    return info(material, name, lore, function)
}

/**
 * 创建开关指示器
 */
fun BaseComponent<*>.toggleIndicator(isEnabled: Boolean, enabledName: String = "已启用", disabledName: String = "已禁用", function: ItemBuilder.() -> Unit = {}): ItemStack {
    return if (isEnabled) {
        success(Material.LIME_DYE, enabledName, listOf("点击禁用"), function)
    } else {
        warning(Material.RED_DYE, disabledName, listOf("点击启用"), function)
    }
}
