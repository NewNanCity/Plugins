package city.newnan.core.terminable

/**
 * 组合Terminable接口
 * 
 * 表示一个由多个其他Terminable组成的Terminable。
 * close()方法按照LIFO（后进先出）顺序关闭子terminable。
 * 
 * Terminable可以重用。实例在每次调用close()时会被有效清空。
 * 
 * 主要特性：
 * - 管理多个子terminable的生命周期
 * - LIFO关闭顺序
 * - 异常收集和处理
 * - 自动清理已关闭的资源
 * - 支持弱引用模式
 * 
 * @see Terminable
 * @see TerminableConsumer
 */
interface CompositeTerminable : Terminable, TerminableConsumer {
    
    companion object {
        /**
         * 创建一个新的独立CompositeTerminable
         * 
         * @return 新的CompositeTerminable实例
         */
        fun create(): CompositeTerminable = AbstractCompositeTerminable()
        
        /**
         * 创建一个新的独立CompositeTerminable，使用弱引用包装子terminable
         * 
         * 弱引用模式下，如果子terminable只被此CompositeTerminable引用，
         * 它们可能会被垃圾回收器回收。
         * 
         * @return 新的弱引用CompositeTerminable实例
         */
        fun createWeak(): CompositeTerminable = AbstractWeakCompositeTerminable()
    }
    
    /**
     * 关闭此组合terminable
     * 
     * 按照LIFO顺序关闭所有子terminable。
     * 如果任何子terminable在关闭时抛出异常，这些异常会被收集并包装在CompositeClosingException中。
     * 
     * @throws CompositeClosingException 如果任何子terminable在关闭时抛出异常
     */
    @Throws(CompositeClosingException::class)
    override fun close()
    
    /**
     * 静默关闭此组合terminable
     * 
     * @return 如果关闭过程中有异常则返回CompositeClosingException，否则返回null
     */
    override fun closeSilently(): CompositeClosingException? {
        return try {
            close()
            null
        } catch (e: CompositeClosingException) {
            e
        }
    }
    
    /**
     * 关闭并报告异常
     * 
     * 如果关闭过程中有异常，会打印所有异常的堆栈跟踪
     */
    override fun closeAndReportException() {
        try {
            close()
        } catch (e: CompositeClosingException) {
            e.printAllStackTraces()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 绑定一个AutoCloseable到此组合terminable
     * 
     * 注意：实现不会跟踪重复的terminable。如果同一个AutoCloseable被添加两次，
     * 它会被关闭两次。
     * 
     * @param autoCloseable 要绑定的closeable
     * @return this（用于链式调用）
     * @throws NullPointerException 如果closeable为null
     */
    fun with(autoCloseable: AutoCloseable): CompositeTerminable
    
    override fun <T : AutoCloseable> bind(terminable: T): T {
        with(terminable)
        return terminable
    }
    
    /**
     * 清理已经终止的实例
     * 
     * 遍历所有绑定的terminable，移除那些已经关闭的实例。
     * 这有助于防止内存泄漏。
     */
    fun cleanup()
}
