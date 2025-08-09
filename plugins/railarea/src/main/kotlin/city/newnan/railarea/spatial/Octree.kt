package city.newnan.railarea.spatial

import city.newnan.core.terminable.Terminable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 八叉树空间索引实现
 * 
 * 用于高效的3D空间查询和碰撞检测。
 * 线程安全的实现，支持并发读写操作。
 *
 * @tparam T 数据负载类型
 * @param boundary 八叉树的边界范围
 * @param maxDepth 最大深度，防止无限递归
 * @param maxItems 每个节点最大项目数，超过时分割
 * @author NewNanCity
 * @since 2.0.0
 */
class Octree<T : Any>(
    private val boundary: Range3D,
    private val maxDepth: Int = 10,
    private val maxItems: Int = 10
) : Terminable {
    
    private val lock = ReentrantReadWriteLock()
    private val items = ConcurrentHashMap<Range3D, T>()
    private var children: Array<Octree<T>>? = null
    private var depth: Int = 0
    private var closed = false
    
    /**
     * 内部构造函数，用于创建子节点
     */
    private constructor(
        boundary: Range3D,
        maxDepth: Int,
        maxItems: Int,
        depth: Int
    ) : this(boundary, maxDepth, maxItems) {
        this.depth = depth
    }
    
    /**
     * 插入范围到八叉树
     * 
     * @param range 要插入的范围
     * @param data 关联的数据（可选）
     * @return 是否成功插入
     */
    fun insert(range: Range3D, data: T): Boolean {
        if (closed) return false
        
        return lock.write {
            // 检查范围是否在边界内
            if (!boundary.intersects(range)) {
                return@write false
            }
            
            // 如果有子节点，尝试插入到子节点
            children?.let { childNodes ->
                for (child in childNodes) {
                    if (child.boundary.intersects(range)) {
                        if (child.insert(range, data)) {
                            return@write true
                        }
                    }
                }
                return@write false
            }
            
            // 添加到当前节点
            items[range] = data
            
            // 检查是否需要分割
            if (items.size > maxItems && depth < maxDepth) {
                subdivide()
            }
            
            true
        }
    }
    
    /**
     * 移除范围
     * 
     * @param range 要移除的范围
     * @return 是否成功移除
     */
    fun remove(range: Range3D): Boolean {
        if (closed) return false
        
        return lock.write {
            // 先尝试从当前节点移除
            if (items.remove(range) != null) {
                return@write true
            }
            
            // 尝试从子节点移除
            children?.let { childNodes ->
                for (child in childNodes) {
                    if (child.boundary.intersects(range)) {
                        if (child.remove(range)) {
                            return@write true
                        }
                    }
                }
            }
            
            false
        }
    }
    
    /**
     * 查询包含指定点的第一个范围
     * 
     * @param point 查询点
     * @return 包含该点的第一个范围，如果没有则返回null
     */
    fun firstRange(point: Point3D): Pair<Range3D, T>? {
        if (closed) return null
        
        return lock.read {
            // 检查点是否在边界内
            if (!boundary.contains(point)) {
                return@read null
            }
            
            // 先检查当前节点的项目
            for (entry in items) {
                if (entry.key.contains(point)) {
                    return@read entry.key to entry.value
                }
            }
            
            // 检查子节点
            children?.let { childNodes ->
                for (child in childNodes) {
                    if (child.boundary.contains(point)) {
                        child.firstRange(point)?.let { return@read it }
                    }
                }
            }
            
            null
        }
    }
    
    /**
     * 查询包含指定点的所有范围
     * 
     * @param point 查询点
     * @return 包含该点的所有范围列表
     */
    fun queryRanges(point: Point3D): List<Pair<Range3D, T>> {
        if (closed) return emptyList()
        
        return lock.read {
            val result = mutableListOf<Pair<Range3D, T>>()
            
            // 检查点是否在边界内
            if (!boundary.contains(point)) {
                return@read result
            }
            
            // 检查当前节点的项目
            for (entry in items) {
                if (entry.key.contains(point)) {
                    result.add(entry.key to entry.value)
                }
            }
            
            // 检查子节点
            children?.let { childNodes ->
                for (child in childNodes) {
                    if (child.boundary.contains(point)) {
                        result.addAll(child.queryRanges(point))
                    }
                }
            }
            
            result
        }
    }
    
    /**
     * 查询与指定范围相交的所有范围
     * 
     * @param queryRange 查询范围
     * @return 相交的所有范围列表
     */
    fun queryIntersecting(queryRange: Range3D): List<Pair<Range3D, T>> {
        if (closed) return emptyList()
        
        return lock.read {
            val result = mutableListOf<Pair<Range3D, T>>()
            
            // 检查查询范围是否与边界相交
            if (!boundary.intersects(queryRange)) {
                return@read result
            }
            
            // 检查当前节点的项目
            for (entry in items) {
                if (entry.key.intersects(queryRange)) {
                    result.add(entry.key to entry.value)
                }
            }
            
            // 检查子节点
            children?.let { childNodes ->
                for (child in childNodes) {
                    if (child.boundary.intersects(queryRange)) {
                        result.addAll(child.queryIntersecting(queryRange))
                    }
                }
            }
            
            result
        }
    }
    
    /**
     * 分割当前节点为8个子节点
     */
    private fun subdivide() {
        if (children != null || depth >= maxDepth) return
        
        val subRanges = boundary.subdivide()
        children = Array(8) { i ->
            Octree(subRanges[i], maxDepth, maxItems, depth + 1)
        }
        
        // 将当前项目重新分配到子节点
        val currentItems = items.toMap()
        items.clear()
        
        for ((range, data) in currentItems) {
            var inserted = false
            for (child in children!!) {
                if (child.boundary.intersects(range)) {
                    if (child.insert(range, data)) {
                        inserted = true
                        break
                    }
                }
            }
            // 如果无法插入到子节点，保留在当前节点
            if (!inserted) {
                items[range] = data
            }
        }
    }
    
    /**
     * 获取统计信息
     */
    fun getStats(): OctreeStats {
        return lock.read {
            var totalNodes = 1
            var totalItems = items.size
            var maxDepthReached = depth
            
            children?.let { childNodes ->
                for (child in childNodes) {
                    val childStats = child.getStats()
                    totalNodes += childStats.totalNodes
                    totalItems += childStats.totalItems
                    maxDepthReached = maxOf(maxDepthReached, childStats.maxDepth)
                }
            }
            
            OctreeStats(totalNodes, totalItems, maxDepthReached)
        }
    }
    
    /**
     * 清空八叉树
     */
    fun clear() {
        lock.write {
            items.clear()
            children?.forEach { it.close() }
            children = null
        }
    }
    
    override fun close() {
        if (closed) return
        
        lock.write {
            closed = true
            clear()
        }
    }
    
    override fun isClosed(): Boolean = closed
}

/**
 * 八叉树统计信息
 */
data class OctreeStats(
    val totalNodes: Int,
    val totalItems: Int,
    val maxDepth: Int
)
