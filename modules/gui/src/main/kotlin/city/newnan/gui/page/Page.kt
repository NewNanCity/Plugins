package city.newnan.gui.page

import city.newnan.gui.component.IComponent
import city.newnan.gui.manager.logging.GuiLogger
import city.newnan.gui.manager.GuiManager
import city.newnan.gui.manager.scheduler.GuiScheduler
import city.newnan.gui.session.Session
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * GUI页面接口
 *
 * Page是GUI界面的最小单位，每个page对应session栈中的一个元素。
 * Page不能独立存在，必须属于某个session。
 *
 * 生命周期：
 * 1. 创建（Create）：实例化page对象
 * 2. 打开（Open）：显示给玩家
 * 3. 关闭（Close）：隐藏界面
 * 4. 销毁（Destroy）：释放资源
 */
interface Page {

    /**
     * 页面所属的玩家
     */
    val player: Player

    /**
     * 页面所属的session
     */
    val session: Session

    /**
     * 页面标题
     */
    val title: net.kyori.adventure.text.Component

    /**
     * 容器类型
     */
    val inventoryType: InventoryType

    /**
     * 容器大小（仅对CHEST类型有效）
     */
    val size: Int?

    /**
     * 页面中的所有组件
     */
    val components: List<IComponent<*>>

    /**
     * 页面是否当前可见
     */
    val isVisible: Boolean

    /**
     * GUI日志
     */
    val logger: GuiLogger

    /**
     * 任务调度器
     */
    val scheduler: GuiScheduler

    /**
     * GUI管理器
     */
    val guiManager: GuiManager

    /**
     * 刷新页面内容
     * 重新渲染所有组件
     */
    fun update()

    /**
     * 渲染指定的槽位列表
     * 页面遍历槽位，获取每个槽位负责的组件，调用组件的渲染方法
     *
     * @param slots 要渲染的槽位列表
     */
    fun renderSlots(slots: List<Int>)

    /**
     * 添加组件到页面
     */
    fun addComponent(component: IComponent<*>)

    /**
     * 移除组件
     */
    fun removeComponent(component: IComponent<*>)

    /**
     * 根据槽位获取组件
     */
    fun getComponentBySlot(slot: Int): IComponent<*>?

    /**
     * 清空所有组件
     */
    fun clearComponents()

    /**
     * 计算页面在session中的位置
     * 配合session.goto使用
     */
    fun getPositionInSession(): Int

    /**
     * 获取聊天输入
     * @param hide 是否隐藏当前GUI
     * @param handler 获取到输入后的回调函数，返回true则结束获取输入，返回false则继续获取输入并处理
     * @return 如果先前已经有其他输入请求，则不会开始获取输入，而返回false，反之则返回true，开始等待输入
     */
    fun chatInput(hide: Boolean = true, handler: (input: String) -> Boolean): Boolean

    /**
     * 处理页面级别的事件
     * @param context 事件上下文
     */
    fun handleEvent(context: city.newnan.gui.event.EventContext<*>)

    /**
     * 内部初始化方法，由Session调用
     * 仅在页面创建时调用一次
     */
    fun initInternal()

    /**
     * 内部销毁方法
     */
    fun destroyInternal()

    /**
     * 内部显示方法，由Session调用
     */
    fun showInternal()

    /**
     * 内部隐藏方法，由Session调用
     */
    fun hideInternal()

    fun show() {
        if (session.current() != this) {
            session.goto(getPositionInSession())
        }
        session.show()
    }

    fun hide() {
        if (session.current() == this) {
            session.hide()
        }
    }

    fun close() {
        if (session.current() == this) {
            session.pop()
        }
    }

    fun back() {
        close()
    }
}
