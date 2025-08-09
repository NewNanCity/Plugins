package city.newnan.core.scheduler

/**
 * 内部任务处理器接口
 * 
 * 继承用户接口，添加调度器内部需要的管理功能
 * 仅供Scheduler内部使用，不暴露给用户
 */
interface IInternalTaskHandler<T> : ITaskHandler<T> {
    /**
     * 启动任务执行
     *
     * @param forced 是否强制启动，忽略依赖检查
     */
    fun start(forced: Boolean = false)
}