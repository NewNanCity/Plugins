package city.newnan.gui.util

import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.gui.manager.GuiManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * GUI物品工具类
 *
 * 集成core模块的ItemBuilder，为GUI组件提供便利的物品创建和管理功能。
 * 提供GUI专用的物品样式和常用物品模板。
 */
class ItemUtil(guiManager: GuiManager) {

    val factory: ItemBuilderFactory = ItemBuilderFactory(guiManager)

    /**
     * 创建ItemBuilder
     */
    fun builder(material: Material, function: ItemBuilder.() -> Unit = {}): ItemBuilder {
        return factory.of(material).apply(function)
    }

    /**
     * 创建ItemBuilder（从现有物品）
     */
    fun builder(itemStack: ItemStack, function: ItemBuilder.() -> Unit = {}): ItemBuilder {
        return factory.of(itemStack).apply(function)
    }

    /**
     * 创建简单物品
     */
    fun create(material: Material, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material, function).build()

    /**
     * 创建简单物品
     */
    fun create(material: Material, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material) {
            name(name)
            lore(lore)
            function()
        }.build()

    /**
     * 创建简单物品（Component版本）
     */
    fun create(material: Material, name: Component? = null, lore: List<Component>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material) {
            name(name)
            lore(lore)
            function()
        }.build()

    // GUI专用物品模板

    /**
     * 创建分隔符物品
     */
    fun separator(material: Material = Material.GRAY_STAINED_GLASS_PANE, name: String = " ", function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material) {
            name(Component.text(name).color(NamedTextColor.GRAY))
            function()
        }.build()

    /**
     * 创建边框物品
     */
    fun border(material: Material = Material.BLACK_STAINED_GLASS_PANE, function: ItemBuilder.() -> Unit = {}): ItemStack =
        separator(material, " ", function)

    /**
     * 创建按钮物品
     */
    fun button(material: Material, name: String, lore: List<String> = emptyList(), function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material) {
            name(Component.text(name).color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true))
            lore(lore)
            function()
        }.build()

    /**
     * 创建信息物品
     */
    fun info(material: Material, name: String, lore: List<String> = emptyList(), function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material) {
            name(Component.text(name).color(NamedTextColor.AQUA))
            lore(lore)
            function()
        }.build()

    /**
     * 创建警告物品
     */
    fun warning(material: Material = Material.BARRIER, name: String, lore: List<String> = emptyList(), function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material) {
            name(Component.text(name).color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true))
            lore(lore)
            function()
        }.build()

    /**
     * 创建成功物品
     */
    fun success(material: Material = Material.LIME_DYE, name: String, lore: List<String> = emptyList(), function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material) {
            name(Component.text(name).color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true))
            lore(lore)
            function()
        }.build()

    /**
     * 创建导航按钮
     */
    fun navigation(material: Material, name: String, description: String? = null, function: ItemBuilder.() -> Unit = {}): ItemStack {
        val loreList = mutableListOf<Component>()
        if (description != null) {
            loreList.add(Component.text(description).color(NamedTextColor.GRAY))
        }
        loreList.add(Component.text("点击进入").color(NamedTextColor.YELLOW))

        return builder(material) {
            name(Component.text(name).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
            lore(loreList)
            function()
        }.build()
    }

    /**
     * 创建翻页按钮
     */
    fun pageButton(isNext: Boolean, currentPage: Int, totalPages: Int, function: ItemBuilder.() -> Unit = {}): ItemStack {
        val material = Material.ARROW
        val name = if (isNext) "下一页" else "上一页"
        val lore = listOf(
            "当前页: $currentPage",
            "总页数: $totalPages",
            if (isNext) "点击查看下一页" else "点击查看上一页"
        )

        return builder(material) {
            name(Component.text(name).color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true))
            lore(lore)
            function()
        }.build()
    }

    /**
     * 创建关闭按钮
     */
    fun closeButton(name: String = "关闭", function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(Material.BARRIER) {
            name(Component.text(name).color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true))
            lore(listOf(Component.text("点击关闭界面").color(NamedTextColor.GRAY)))
            function()
        }.build()

    /**
     * 创建返回按钮
     */
    fun backButton(name: String = "返回", function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(Material.ARROW) {
            name(Component.text(name).color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true))
            lore(listOf(Component.text("点击返回上一页").color(NamedTextColor.GRAY)))
            function()
        }.build()

    /**
     * 创建确认按钮
     */
    fun confirmButton(name: String = "确认", function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(Material.LIME_DYE) {
            name(Component.text(name).color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true))
            lore(listOf(Component.text("点击确认操作").color(NamedTextColor.GRAY)))
            function()
        }.build()

    /**
     * 创建取消按钮
     */
    fun cancelButton(name: String = "取消", function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(Material.RED_DYE) {
            name(Component.text(name).color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true))
            lore(listOf(Component.text("点击取消操作").color(NamedTextColor.GRAY)))
            function()
        }.build()

    /**
     * 创建加载中物品
     */
    fun loading(name: String = "加载中...", function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(Material.CLOCK) {
            name(Component.text(name).color(NamedTextColor.YELLOW))
            lore(listOf(Component.text("请稍候...").color(NamedTextColor.GRAY)))
            function()
        }.build()

    /**
     * 创建错误物品
     */
    fun error(name: String = "加载失败", description: String = "点击重试", function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(Material.BARRIER) {
            name(Component.text(name).color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true))
            lore(listOf(Component.text(description).color(NamedTextColor.GRAY)))
            function()
        }.build()

    /**
     * 创建空槽位物品
     */
    fun emptySlot(material: Material = Material.LIGHT_GRAY_STAINED_GLASS_PANE, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material) {
            name(Component.text(" "))
            function()
        }.build()

    /**
     * 创建发光物品
     */
    fun glowing(itemStack: ItemStack, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(itemStack) {
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
            function()
        }.build()

    /**
     * 创建发光物品（从材料）
     */
    fun glowing(material: Material, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material) {
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
            name(name)
            lore(lore)
            function()
        }.build()

    /**
     * 创建数量显示物品
     */
    fun amount(material: Material, amount: Int, name: String? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(material, function).amount(amount).name(name).build()

    /**
     * 创建头颅物品（使用OfflinePlayer方法设置头颅）
     */
    fun skull(player: OfflinePlayer, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(factory.createSkull(player, name, lore), function).build()

    /**
     * 创建头颅物品（使用UUID方法设置头颅）
     */
    fun skull(uuid: UUID, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(factory.createSkull(uuid, name, lore), function).build()

    /**
     * 创建头颅物品（使用玩家名称设置头颅）
     */
    fun skull(playerName: String, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(factory.createSkull(playerName, name, lore), function).build()

    /**
     * 创建自定义头颅物品（Base64纹理）
     */
    fun customSkull(base64Texture: String, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(factory.createCustomSkull(base64Texture, name, lore), function).build()

    /**
     * 创建URL头颅物品（通过材质URL）
     */
    fun urlSkull(textureUrlOrHash: String, name: String? = null, lore: List<String>? = null, function: ItemBuilder.() -> Unit = {}): ItemStack =
        builder(factory.createUrlSkull(textureUrlOrHash, name, lore), function).build()
}
