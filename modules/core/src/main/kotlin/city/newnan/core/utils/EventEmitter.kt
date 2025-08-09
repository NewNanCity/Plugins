package city.newnan.core.utils

class EventEmitter<T> {
    // 用 volatile 保证可见性
    @Volatile
    private var listeners: Array<(T) -> Unit> = emptyArray()

    /**
     * 添加监听器
     *
     * @param listener 监听器函数
     */
    fun addListener(listener: (T) -> Unit) {
        listeners = listeners + listener
    }

    /**
     * 移除监听器
     *
     * @param listener 监听器函数
     */
    fun removeListener(listener: (T) -> Unit) {
        listeners = listeners.filter { it !== listener }.toTypedArray()
    }

    /**
     * 获取当前监听器数量
     *
     * @return 监听器数量
     */
    val size: Int
        get() = listeners.size

    /**
     * 清除所有监听器
     */
    fun clear() {
        listeners = emptyArray()
    }

    /**
     * 触发事件
     *
     * @param value 事件值
     */
    fun emit(value: T) {
        val snapshot = listeners           // 取一次快照，之后数组不会被修改
        for (i in snapshot.indices) {
            snapshot[i](value)
        }
    }
}