package city.newnan.core.terminable

/**
 * Terminable消费者接口
 *
 * 接受AutoCloseable（包括Terminable）的绑定。
 * 这是terminable体系中的核心接口，负责管理多个资源的生命周期。
 *
 * 主要功能：
 * - 绑定AutoCloseable资源
 * - 统一管理资源生命周期
 *
 * @see Terminable
 * @see CompositeTerminable
 */
@FunctionalInterface
interface TerminableConsumer {

    /**
     * 绑定一个terminable资源
     *
     * 将指定的terminable添加到此consumer的管理范围内。
     * 当consumer关闭时，所有绑定的terminable也会被关闭。
     *
     * @param terminable 要绑定的terminable
     * @param T terminable的类型
     * @return 返回相同的terminable实例，便于链式调用
     */
    fun <T : AutoCloseable> bind(terminable: T): T
}
