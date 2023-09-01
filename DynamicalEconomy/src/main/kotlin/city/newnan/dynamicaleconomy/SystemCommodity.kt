package city.newnan.dynamicaleconomy

import city.newnan.dynamicaleconomy.config.Commodity
import me.lucko.helper.serialize.InventorySerialization
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.function.Consumer
import kotlin.math.log10
import kotlin.math.pow


const val EPSILON = 0.001

class SystemCommodity(private val config: Commodity, val name: String) {
    var itemStack: ItemStack? = null
    var buyValue = 0.0
    var sellValue = 0.0
    // var shopList: ArrayList<Shop> = ArrayList<Shop>()

    init {
        // 两种不同的构造方式。{开头的是JSON格式，反之是base64格式
        itemStack = if (config.data[0] == '{') {
            // TODO: 2021/7/31 从JSON转化为ItemStack
            null
        } else {
            InventorySerialization.decodeItemStack(config.data)
        }
        updatePrice()
    }

    /**
     * 收购：官方 <- 玩家
     *
     * @param amount 收购数量
     */
    fun buy(amount: Long) {
        // 维持国库所有者的虚拟存款在500000以上
        val d = PluginMain.INSTANCE.economy.getBalance(PluginMain.INSTANCE.owner)
        if (d < 500000.0) PluginMain.INSTANCE.economy.depositPlayer(
            PluginMain.INSTANCE.owner, 500000.0 - d
        )
        config.amount += amount
        val curTime = System.currentTimeMillis()
        // 计算γ
        val gamma = if (config.lastBuyTime == 0L) 0.0 else 10.0 / (10.0 + log10((1 + curTime - config.lastBuyTime).toDouble()))

        // 更新时间
        config.lastBuyTime = curTime

        // 更新响应量
        config.buyResponseVolume = amount + gamma * config.buyResponseVolume

        // 更新商品价值
        updatePrice()

        // 刷新所有商店
        // updateShops()
    }

    /**
     * 售卖：官方 -> 玩家
     *
     * @param amount 售卖数量
     */
    fun sell(amount: Long) {
        config.amount -= amount.toInt()

        // 小于0检查，虽然一般不可能出现这种情况，但是还是检测一下
        if (config.amount < 0L) config.amount = 0L
        val curTime = System.currentTimeMillis()
        // 计算γ
        val gamma = if (config.lastSellTime == 0L) 0.0 else 10.0 / (10.0 + log10((1 + curTime - config.lastSellTime).toDouble()))

        // 更新时间
        config.lastSellTime = curTime

        // 更新响应量
        config.sellResponseVolume = amount + gamma * config.sellResponseVolume

        // 更新商品价值
        updatePrice()

        // 刷新所有商店
        // updateShops()
    }

    fun updatePrice() {
        // 计算响应比
        var ratio = (config.amount + config.sellResponseVolume + EPSILON) / (config.amount + config.buyResponseVolume + EPSILON)
        if (ratio > 10) {
            ratio = 10.0
        } else if (ratio < 1.0) {
            ratio = 1.0
        }
        buyValue = config.value * ratio.pow(0.8)
        sellValue = config.value * ratio.pow(1.2)
    }

//    fun updateShops() {
//        shopList.forEach(Consumer<Shop> { shop: Shop -> updateShop(shop) })
//    }
//
//    fun updateShop(shop: Shop) {
//        // 更新库存
//        Objects.requireNonNull((shop as ContainerShop).getInventory()).clear()
//        shop.add(itemStack, config.amount)
//
//        //更新价格
//        if (shop.getShopType().equals(ShopType.BUYING)) {
//            // 收购商店
//            shop.setPrice(dynamicEconomy.buyCurrencyIndex * buyValue)
//        } else {
//            // 售卖商店
//            shop.setPrice(dynamicEconomy.sellCurrencyIndex * sellValue)
//        }
//    }
}