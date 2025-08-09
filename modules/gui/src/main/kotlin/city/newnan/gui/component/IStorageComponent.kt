package city.newnan.gui.component

import org.bukkit.inventory.ItemStack

/**
 * 存储组件接口
 * 
 * 标识可以进行物品交互的组件
 * 只有实现此接口的组件才允许玩家进行物品操作（放入、取出、拖拽等）
 * 
 * 这是GUI系统物品保护机制的核心接口
 */
interface IStorageComponent {
    
    /**
     * 检查是否为存储组件
     * 
     * @return true表示允许物品交互，false表示禁止物品交互
     */
    fun isStorageComponent(): Boolean = true
    
    /**
     * 检查是否可以接受指定物品
     * 
     * @param item 要检查的物品
     * @return true表示可以接受，false表示拒绝
     */
    fun canAcceptItem(item: org.bukkit.inventory.ItemStack?): Boolean = true
    
    /**
     * 检查是否可以取出指定物品
     * 
     * @param item 要检查的物品
     * @return true表示可以取出，false表示拒绝
     */
    fun canTakeItem(item: org.bukkit.inventory.ItemStack?): Boolean = true

    fun handleItemChange(slot: Int, oldItem: org.bukkit.inventory.ItemStack?, newItem: org.bukkit.inventory.ItemStack?)

    /**
     * 获取当前存储的物品
     */
    fun getStoredItem(slot: Int): ItemStack?

    /**
     * 设置存储的物品
     */
    fun setStoredItem(slot: Int, item: ItemStack?)
}
