package city.newnan.bettercommandblock.firewall.trie

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import city.newnan.bettercommandblock.firewall.validators.Validator
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 命令前缀树管理器
 *
 * 管理命令匹配的前缀树结构，提供高效的命令验证功能。
 * 支持以下特性：
 * - 线程安全：支持并发读取和安全的写入操作
 * - 动态更新：支持运行时添加和移除命令规则
 * - 流式匹配：与CommandScanner集成，支持流式命令验证
 * - 统计信息：提供匹配统计和性能监控
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandTrie {
    
    /**
     * 根节点
     */
    private val root = TrieNode()
    
    /**
     * 读写锁，用于线程安全
     */
    private val lock = ReentrantReadWriteLock()
    
    /**
     * 统计信息
     */
    private var totalMatches = 0L
    private var totalValidations = 0L
    private var totalRejections = 0L
    
    /**
     * 添加命令到前缀树
     *
     * @param tokens 命令token列表
     * @param validator 可选的验证器
     * @param metadata 可选的元数据
     */
    fun addCommand(tokens: List<String>, validator: Validator? = null, metadata: Map<String, Any> = emptyMap()) {
        if (tokens.isEmpty()) return
        
        lock.write {
            var current = root
            
            for ((index, token) in tokens.withIndex()) {
                if (!current.hasChild(token)) {
                    current.addChild(token, TrieNode())
                }
                current = current.getChild(token)!!
                
                // 如果是最后一个token，标记为结束节点
                if (index == tokens.size - 1) {
                    current.isEnd = true
                    current.validator = validator
                    metadata.forEach { (key, value) ->
                        current.setMetadata(key, value)
                    }
                }
            }
        }
    }
    
    /**
     * 添加命令字符串到前缀树
     *
     * @param command 命令字符串
     * @param validator 可选的验证器
     * @param metadata 可选的元数据
     */
    fun addCommand(command: String, validator: Validator? = null, metadata: Map<String, Any> = emptyMap()) {
        val scanner = CommandScanner(command)
        val tokens = mutableListOf<String>()
        
        while (true) {
            val token = scanner.nextToken() ?: break
            tokens.add(token)
        }
        
        addCommand(tokens, validator, metadata)
    }
    
    /**
     * 移除命令从前缀树
     *
     * @param tokens 命令token列表
     * @return 如果成功移除则返回true
     */
    fun removeCommand(tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return false
        
        return lock.write {
            removeCommandRecursive(root, tokens, 0)
        }
    }
    
    /**
     * 移除命令字符串从前缀树
     *
     * @param command 命令字符串
     * @return 如果成功移除则返回true
     */
    fun removeCommand(command: String): Boolean {
        val scanner = CommandScanner(command)
        val tokens = mutableListOf<String>()
        
        while (true) {
            val token = scanner.nextToken() ?: break
            tokens.add(token)
        }
        
        return removeCommand(tokens)
    }
    
    /**
     * 检查命令是否安全
     *
     * @param command 要检查的命令字符串
     * @return 如果命令安全则返回true
     */
    fun isCommandSafe(command: String): Boolean {
        val scanner = CommandScanner(command)
        return lock.read {
            totalValidations++
            val result = matchTokens(scanner, root)
            if (result) {
                totalMatches++
            } else {
                totalRejections++
            }
            result
        }
    }
    
    /**
     * 使用扫描器匹配token
     *
     * @param scanner 命令扫描器
     * @param node 当前节点
     * @return 如果匹配成功则返回true
     */
    private fun matchTokens(scanner: CommandScanner, node: TrieNode): Boolean {
        var currentNode = node

        while (true) {
            val token = scanner.nextToken() ?: break
            
            // 检查当前节点是否有匹配的子节点
            val nextNode = currentNode.getChild(token) ?: return false

            currentNode = nextNode
            
            // 遇到特殊规则节点：触发验证器
            if (currentNode.validator != null) {
                return currentNode.validator!!.validate(scanner)
            }

            // 比如 tp xxx 匹配了规则前缀 tp
            if (currentNode.isEnd) {
                return true
            }
        }
        
        // 检查是否在有效的结束节点
        return currentNode.isEnd
    }
    
    /**
     * 递归移除命令
     */
    private fun removeCommandRecursive(node: TrieNode, tokens: List<String>, index: Int): Boolean {
        if (index == tokens.size) {
            // 到达目标节点
            if (!node.isEnd) return false
            
            node.isEnd = false
            node.validator = null
            node.metadata.clear()
            
            // 如果节点没有子节点，可以删除
            return node.isLeaf()
        }
        
        val token = tokens[index]
        val child = node.getChild(token) ?: return false
        
        val shouldDeleteChild = removeCommandRecursive(child, tokens, index + 1)
        
        if (shouldDeleteChild) {
            node.removeChild(token)
        }
        
        // 如果当前节点不是结束节点且没有子节点，可以删除
        return !node.isEnd && node.isLeaf()
    }
    
    /**
     * 清空所有命令
     */
    fun clear() {
        lock.write {
            root.clearChildren()
            root.isEnd = false
            root.validator = null
            root.metadata.clear()
            
            // 重置统计信息
            totalMatches = 0L
            totalValidations = 0L
            totalRejections = 0L
        }
    }
    
    /**
     * 获取前缀树大小（节点总数）
     *
     * @return 节点总数
     */
    fun size(): Int {
        return lock.read {
            root.getSubtreeSize()
        }
    }
    
    /**
     * 检查前缀树是否为空
     *
     * @return 如果为空则返回true
     */
    fun isEmpty(): Boolean {
        return lock.read {
            root.isLeaf() && !root.isEnd
        }
    }
    
    /**
     * 获取所有命令路径
     *
     * @return 命令路径列表
     */
    fun getAllCommands(): List<List<String>> {
        return lock.read {
            val commands = mutableListOf<List<String>>()
            collectCommands(root, mutableListOf(), commands)
            commands
        }
    }
    
    /**
     * 递归收集所有命令路径
     */
    private fun collectCommands(node: TrieNode, currentPath: MutableList<String>, result: MutableList<List<String>>) {
        if (node.isEnd) {
            result.add(currentPath.toList())
        }
        
        for ((token, child) in node.children) {
            currentPath.add(token)
            collectCommands(child, currentPath, result)
            currentPath.removeAt(currentPath.size - 1)
        }
    }
    
    /**
     * 获取统计信息
     *
     * @return 统计信息映射
     */
    fun getStatistics(): Map<String, Long> {
        return lock.read {
            mapOf(
                "totalValidations" to totalValidations,
                "totalMatches" to totalMatches,
                "totalRejections" to totalRejections,
                "treeSize" to size().toLong()
            )
        }
    }
    
    /**
     * 重置统计信息
     */
    fun resetStatistics() {
        lock.write {
            totalMatches = 0L
            totalValidations = 0L
            totalRejections = 0L
        }
    }
    
    override fun toString(): String {
        return lock.read {
            "CommandTrie(size=${size()}, validations=$totalValidations, matches=$totalMatches, rejections=$totalRejections)"
        }
    }
}
