package city.newnan.bettercommandblock.firewall.trie

import city.newnan.bettercommandblock.firewall.validators.Validator

/**
 * 前缀树节点
 *
 * 用于构建命令匹配的前缀树结构。每个节点代表命令路径中的一个token，
 * 支持以下特性：
 * - 子节点映射：存储到下一级token的路径
 * - 结束标记：标识完整命令的结束点
 * - 验证器：在特定节点触发深度验证逻辑
 * - 元数据：存储节点相关的额外信息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class TrieNode {
    
    /**
     * 子节点映射
     * Key: token字符串，Value: 对应的子节点
     */
    val children: MutableMap<String, TrieNode> = HashMap()
    
    /**
     * 结束标记
     * 当为true时，表示从根节点到当前节点构成一个完整的有效命令
     */
    var isEnd: Boolean = false
    
    /**
     * 特殊规则验证器
     * 当匹配到此节点时，会触发验证器进行深度验证
     */
    var validator: Validator? = null
    
    /**
     * 节点元数据
     * 存储与此节点相关的额外信息，如命令描述、权限要求等
     */
    val metadata: MutableMap<String, Any> = HashMap()
    
    /**
     * 节点深度
     * 从根节点到当前节点的距离
     */
    var depth: Int = 0
    
    /**
     * 添加子节点
     *
     * @param token 子节点对应的token
     * @param child 子节点实例
     * @return 添加的子节点
     */
    fun addChild(token: String, child: TrieNode): TrieNode {
        child.depth = this.depth + 1
        children[token] = child
        return child
    }
    
    /**
     * 获取子节点
     *
     * @param token 要查找的token
     * @return 对应的子节点，如果不存在则返回null
     */
    fun getChild(token: String): TrieNode? {
        return children[token]
    }
    
    /**
     * 检查是否有指定的子节点
     *
     * @param token 要检查的token
     * @return 如果存在对应子节点则返回true
     */
    fun hasChild(token: String): Boolean {
        return children.containsKey(token)
    }
    
    /**
     * 移除子节点
     *
     * @param token 要移除的子节点对应的token
     * @return 被移除的子节点，如果不存在则返回null
     */
    fun removeChild(token: String): TrieNode? {
        return children.remove(token)
    }
    
    /**
     * 获取所有子节点的token
     *
     * @return 子节点token的集合
     */
    fun getChildTokens(): Set<String> {
        return children.keys.toSet()
    }
    
    /**
     * 检查是否为叶子节点
     *
     * @return 如果没有子节点则返回true
     */
    fun isLeaf(): Boolean {
        return children.isEmpty()
    }
    
    /**
     * 获取子节点数量
     *
     * @return 子节点的数量
     */
    fun getChildCount(): Int {
        return children.size
    }
    
    /**
     * 设置元数据
     *
     * @param key 元数据键
     * @param value 元数据值
     */
    fun setMetadata(key: String, value: Any) {
        metadata[key] = value
    }
    
    /**
     * 获取元数据
     *
     * @param key 元数据键
     * @return 元数据值，如果不存在则返回null
     */
    fun getMetadata(key: String): Any? {
        return metadata[key]
    }
    
    /**
     * 获取指定类型的元数据
     *
     * @param key 元数据键
     * @param clazz 期望的数据类型
     * @return 指定类型的元数据值，如果不存在或类型不匹配则返回null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getMetadata(key: String, clazz: Class<T>): T? {
        val value = metadata[key]
        return if (clazz.isInstance(value)) {
            value as T
        } else {
            null
        }
    }
    
    /**
     * 清空所有子节点
     */
    fun clearChildren() {
        children.clear()
    }
    
    /**
     * 递归计算以此节点为根的子树大小
     *
     * @return 子树中节点的总数（包括当前节点）
     */
    fun getSubtreeSize(): Int {
        var size = 1 // 当前节点
        for (child in children.values) {
            size += child.getSubtreeSize()
        }
        return size
    }
    
    /**
     * 创建节点的深拷贝
     *
     * @return 新的TrieNode实例
     */
    fun deepCopy(): TrieNode {
        val copy = TrieNode()
        copy.isEnd = this.isEnd
        copy.validator = this.validator
        copy.depth = this.depth
        copy.metadata.putAll(this.metadata)
        
        // 递归复制子节点
        for ((token, child) in this.children) {
            copy.children[token] = child.deepCopy()
        }
        
        return copy
    }
    
    override fun toString(): String {
        return "TrieNode(depth=$depth, isEnd=$isEnd, childCount=${children.size}, hasValidator=${validator != null})"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrieNode) return false
        
        return isEnd == other.isEnd &&
                depth == other.depth &&
                children == other.children &&
                validator == other.validator
    }
    
    override fun hashCode(): Int {
        var result = children.hashCode()
        result = 31 * result + isEnd.hashCode()
        result = 31 * result + (validator?.hashCode() ?: 0)
        result = 31 * result + depth
        return result
    }
}
