package city.newnan.core.terminable

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * CompositeTerminable的抽象实现
 * 
 * 使用ConcurrentLinkedDeque来存储子terminable，支持并发访问。
 * 使用ReentrantLock来保证关闭操作的线程安全。
 * 
 * 特性：
 * - 线程安全的添加和移除操作
 * - LIFO关闭顺序
 * - 异常收集和处理
 * - 自动清理已关闭的资源
 */
internal class AbstractCompositeTerminable : CompositeTerminable {
    
    // 使用双端队列存储terminable，支持高效的头部和尾部操作
    private val terminables = ConcurrentLinkedDeque<AutoCloseable>()
    
    // 用于同步关闭操作的锁
    private val closeLock = ReentrantLock()
    
    // 标记是否已关闭
    @Volatile
    private var closed = false
    
    override fun with(autoCloseable: AutoCloseable): CompositeTerminable {
        if (closed) {
            // 如果已经关闭，直接关闭新添加的资源
            autoCloseable.closeSilently()
            return this
        }
        
        // 添加到队列头部，确保LIFO顺序
        terminables.addFirst(autoCloseable)
        return this
    }
    
    @Throws(CompositeClosingException::class)
    override fun close() {
        closeLock.withLock {
            if (closed) {
                return
            }
            closed = true
            
            val exceptions = mutableListOf<Exception>()
            
            // 按LIFO顺序关闭所有terminable
            while (true) {
                val terminable = terminables.pollFirst() ?: break
                
                try {
                    terminable.close()
                } catch (e: Exception) {
                    exceptions.add(e)
                }
            }
            
            // 如果有异常，抛出组合异常
            if (exceptions.isNotEmpty()) {
                throw CompositeClosingException(exceptions)
            }
        }
    }
    
    override fun isClosed(): Boolean = closed
    
    override fun cleanup() {
        if (closed) {
            return
        }
        
        // 创建一个新的列表来存储仍然有效的terminable
        val validTerminables = mutableListOf<AutoCloseable>()
        
        // 检查每个terminable是否仍然有效
        while (true) {
            val terminable = terminables.pollFirst() ?: break
            
            val shouldKeep = when (terminable) {
                is Terminable -> !terminable.isClosed()
                else -> true // 对于非Terminable的AutoCloseable，我们无法检查状态，所以保留
            }
            
            if (shouldKeep) {
                validTerminables.add(terminable)
            }
        }
        
        // 将仍然有效的terminable重新添加到队列中
        validTerminables.reversed().forEach { terminables.addFirst(it) }
    }
    
    /**
     * 获取当前绑定的terminable数量
     * 
     * @return terminable数量
     */
    fun size(): Int = terminables.size
    
    /**
     * 检查是否为空
     * 
     * @return 如果没有绑定任何terminable则返回true
     */
    fun isEmpty(): Boolean = terminables.isEmpty()
}

/**
 * AutoCloseable的扩展函数，提供静默关闭功能
 */
private fun AutoCloseable.closeSilently(): Exception? {
    return try {
        close()
        null
    } catch (e: Exception) {
        e
    }
}
