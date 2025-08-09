package city.newnan.core.terminable

/**
 * 可终止资源接口
 *
 * 扩展了AutoCloseable，提供更丰富的资源生命周期管理功能。
 * 这是整个terminable体系的核心接口。
 *
 * 主要特性：
 * - 资源自动清理
 * - 异常安全的关闭操作
 * - 状态检查
 * - 与TerminableConsumer的绑定
 *
 * @see TerminableConsumer
 * @see CompositeTerminable
 */
@FunctionalInterface
interface Terminable : AutoCloseable {

    companion object {
        /**
         * 空的Terminable实例，什么都不做
         */
        val EMPTY: Terminable = object : Terminable {
            override fun close() {}
        }
    }

    /**
     * 关闭此资源
     *
     * 实现类应该确保此方法是幂等的，即多次调用不会产生副作用
     */
    @Throws(Exception::class)
    override fun close()

    /**
     * 检查此资源是否已经永久关闭
     *
     * @return 如果资源已永久关闭则返回true
     */
    fun isClosed(): Boolean = false

    /**
     * 静默关闭此资源，返回可能抛出的异常
     *
     * 此方法不会抛出异常，而是将异常作为返回值返回
     *
     * @return 如果关闭过程中抛出异常则返回该异常，否则返回null
     */
    fun closeSilently(): Exception? {
        return try {
            close()
            null
        } catch (e: Exception) {
            e
        }
    }

    /**
     * 关闭此资源，如果抛出异常则打印到控制台
     *
     * 这是一个便利方法，适用于不需要处理异常的场景
     */
    fun closeAndReportException() {
        try {
            close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 将此terminable绑定到指定的consumer
     *
     * 这是一个便利方法，等价于 consumer.bind(this)
     *
     * @param consumer 要绑定到的consumer
     */
    fun bindWith(consumer: TerminableConsumer) {
        consumer.bind(this)
    }
}
