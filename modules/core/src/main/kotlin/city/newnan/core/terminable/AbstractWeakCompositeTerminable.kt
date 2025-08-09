package city.newnan.core.terminable

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 使用弱引用的CompositeTerminable实现
 * 
 * 与AbstractCompositeTerminable类似，但使用WeakReference来包装子terminable。
 * 这意味着如果子terminable只被此CompositeTerminable引用，它们可能会被垃圾回收器回收。
 * 
 * 适用场景：
 * - 当你不确定子terminable的生命周期时
 * - 当你希望避免循环引用导致的内存泄漏时
 * - 当子terminable可能在其他地方被管理时
 * 
 * 注意：由于使用弱引用，某些terminable可能在关闭之前就被垃圾回收了。
 */
internal class AbstractWeakCompositeTerminable : CompositeTerminable {
    
    // 使用弱引用存储terminable
    private val terminables = ConcurrentLinkedDeque<WeakReference<AutoCloseable>>()
    
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
        
        // 使用弱引用包装并添加到队列头部
        terminables.addFirst(WeakReference(autoCloseable))
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
                val weakRef = terminables.pollFirst() ?: break
                val terminable = weakRef.get()
                
                if (terminable != null) {
                    try {
                        terminable.close()
                    } catch (e: Exception) {
                        exceptions.add(e)
                    }
                }
                // 如果terminable为null，说明已经被垃圾回收了，跳过
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
        val validTerminables = mutableListOf<WeakReference<AutoCloseable>>()
        
        // 检查每个terminable是否仍然有效
        while (true) {
            val weakRef = terminables.pollFirst() ?: break
            val terminable = weakRef.get()
            
            if (terminable != null) {
                val shouldKeep = when (terminable) {
                    is Terminable -> !terminable.isClosed()
                    else -> true // 对于非Terminable的AutoCloseable，我们无法检查状态，所以保留
                }
                
                if (shouldKeep) {
                    validTerminables.add(weakRef)
                }
            }
            // 如果terminable为null（已被垃圾回收）或已关闭，则不重新添加
        }
        
        // 将仍然有效的terminable重新添加到队列中
        validTerminables.reversed().forEach { terminables.addFirst(it) }
    }
    
    /**
     * 获取当前有效的terminable数量
     * 
     * 注意：这个方法会触发清理操作，移除已被垃圾回收的弱引用
     * 
     * @return 有效的terminable数量
     */
    fun size(): Int {
        cleanup()
        return terminables.size
    }
    
    /**
     * 检查是否为空
     * 
     * 注意：这个方法会触发清理操作
     * 
     * @return 如果没有有效的terminable则返回true
     */
    fun isEmpty(): Boolean {
        cleanup()
        return terminables.isEmpty()
    }
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
