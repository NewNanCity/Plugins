package city.newnan.core.terminable

/**
 * 组合关闭异常
 * 
 * 当CompositeTerminable关闭其子terminable时，如果有多个子terminable抛出异常，
 * 这些异常会被收集到此异常中。
 * 
 * 这个异常提供了便利方法来处理多个异常的情况。
 */
class CompositeClosingException(
    /**
     * 关闭过程中收集到的所有异常
     */
    val exceptions: List<Exception>
) : Exception("在关闭组合terminable时发生了 ${exceptions.size} 个异常") {
    
    /**
     * 打印所有异常的堆栈跟踪
     * 
     * 这个方法会遍历所有收集到的异常，并打印它们的堆栈跟踪。
     * 适用于调试和错误报告。
     */
    fun printAllStackTraces() {
        println("CompositeClosingException: 关闭过程中发生了 ${exceptions.size} 个异常:")
        exceptions.forEachIndexed { index, exception ->
            println("异常 ${index + 1}:")
            exception.printStackTrace()
            println("---")
        }
    }
    
    /**
     * 获取第一个异常
     * 
     * @return 第一个异常，如果没有异常则返回null
     */
    fun getFirstException(): Exception? = exceptions.firstOrNull()
    
    /**
     * 检查是否包含指定类型的异常
     * 
     * @param exceptionClass 要检查的异常类型
     * @return 如果包含指定类型的异常则返回true
     */
    fun <T : Exception> hasExceptionOfType(exceptionClass: Class<T>): Boolean {
        return exceptions.any { exceptionClass.isInstance(it) }
    }
    
    /**
     * 获取指定类型的所有异常
     * 
     * @param exceptionClass 要获取的异常类型
     * @return 指定类型的异常列表
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Exception> getExceptionsOfType(exceptionClass: Class<T>): List<T> {
        return exceptions.filter { exceptionClass.isInstance(it) } as List<T>
    }
}
