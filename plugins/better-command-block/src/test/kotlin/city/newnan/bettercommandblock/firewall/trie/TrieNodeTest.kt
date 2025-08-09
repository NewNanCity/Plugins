package city.newnan.bettercommandblock.firewall.trie

import city.newnan.bettercommandblock.firewall.validators.Validator
import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * TrieNode单元测试
 *
 * 测试前缀树节点的各种功能，包括：
 * - 子节点管理
 * - 元数据操作
 * - 验证器设置
 * - 树结构操作
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class TrieNodeTest {
    
    private lateinit var rootNode: TrieNode
    private lateinit var mockValidator: Validator
    
    @BeforeEach
    fun setUp() {
        rootNode = TrieNode()
        mockValidator = object : Validator {
            override fun validate(scanner: CommandScanner): Boolean = true
            override fun getName(): String = "MockValidator"
        }
    }
    
    @Test
    fun `test initial state`() {
        val node = TrieNode()
        
        assertFalse(node.isEnd)
        assertNull(node.validator)
        assertEquals(0, node.depth)
        assertTrue(node.children.isEmpty())
        assertTrue(node.metadata.isEmpty())
        assertTrue(node.isLeaf())
        assertEquals(0, node.getChildCount())
    }
    
    @Test
    fun `test addChild`() {
        val child = TrieNode()
        val addedChild = rootNode.addChild("test", child)
        
        assertSame(child, addedChild)
        assertEquals(1, child.depth)
        assertTrue(rootNode.hasChild("test"))
        assertEquals(child, rootNode.getChild("test"))
        assertFalse(rootNode.isLeaf())
        assertEquals(1, rootNode.getChildCount())
    }
    
    @Test
    fun `test getChild`() {
        val child = TrieNode()
        rootNode.addChild("test", child)
        
        assertEquals(child, rootNode.getChild("test"))
        assertNull(rootNode.getChild("nonexistent"))
    }
    
    @Test
    fun `test hasChild`() {
        val child = TrieNode()
        rootNode.addChild("test", child)
        
        assertTrue(rootNode.hasChild("test"))
        assertFalse(rootNode.hasChild("nonexistent"))
    }
    
    @Test
    fun `test removeChild`() {
        val child = TrieNode()
        rootNode.addChild("test", child)
        
        assertTrue(rootNode.hasChild("test"))
        
        val removedChild = rootNode.removeChild("test")
        assertEquals(child, removedChild)
        assertFalse(rootNode.hasChild("test"))
        assertTrue(rootNode.isLeaf())
        
        // 移除不存在的子节点
        assertNull(rootNode.removeChild("nonexistent"))
    }
    
    @Test
    fun `test getChildTokens`() {
        rootNode.addChild("say", TrieNode())
        rootNode.addChild("give", TrieNode())
        rootNode.addChild("tp", TrieNode())
        
        val tokens = rootNode.getChildTokens()
        assertEquals(3, tokens.size)
        assertTrue(tokens.contains("say"))
        assertTrue(tokens.contains("give"))
        assertTrue(tokens.contains("tp"))
    }
    
    @Test
    fun `test isLeaf`() {
        assertTrue(rootNode.isLeaf())
        
        rootNode.addChild("test", TrieNode())
        assertFalse(rootNode.isLeaf())
        
        rootNode.removeChild("test")
        assertTrue(rootNode.isLeaf())
    }
    
    @Test
    fun `test getChildCount`() {
        assertEquals(0, rootNode.getChildCount())
        
        rootNode.addChild("test1", TrieNode())
        assertEquals(1, rootNode.getChildCount())
        
        rootNode.addChild("test2", TrieNode())
        assertEquals(2, rootNode.getChildCount())
        
        rootNode.removeChild("test1")
        assertEquals(1, rootNode.getChildCount())
    }
    
    @Test
    fun `test metadata operations`() {
        rootNode.setMetadata("key1", "value1")
        rootNode.setMetadata("key2", 42)
        rootNode.setMetadata("key3", true)
        
        assertEquals("value1", rootNode.getMetadata("key1"))
        assertEquals(42, rootNode.getMetadata("key2"))
        assertEquals(true, rootNode.getMetadata("key3"))
        assertNull(rootNode.getMetadata("nonexistent"))
    }
    
    @Test
    fun `test typed metadata operations`() {
        rootNode.setMetadata("stringKey", "test")
        rootNode.setMetadata("intKey", 123)
        rootNode.setMetadata("boolKey", false)
        
        assertEquals("test", rootNode.getMetadata("stringKey", String::class.java))
        assertEquals(123, rootNode.getMetadata("intKey", Integer::class.java))
        assertEquals(false, rootNode.getMetadata("boolKey", Boolean::class.java))
        
        // 类型不匹配应该返回null
        assertNull(rootNode.getMetadata("stringKey", Integer::class.java))
        assertNull(rootNode.getMetadata("intKey", String::class.java))
        
        // 不存在的键应该返回null
        assertNull(rootNode.getMetadata("nonexistent", String::class.java))
    }
    
    @Test
    fun `test validator operations`() {
        assertNull(rootNode.validator)
        
        rootNode.validator = mockValidator
        assertEquals(mockValidator, rootNode.validator)
        
        rootNode.validator = null
        assertNull(rootNode.validator)
    }
    
    @Test
    fun `test isEnd flag`() {
        assertFalse(rootNode.isEnd)
        
        rootNode.isEnd = true
        assertTrue(rootNode.isEnd)
        
        rootNode.isEnd = false
        assertFalse(rootNode.isEnd)
    }
    
    @Test
    fun `test depth tracking`() {
        assertEquals(0, rootNode.depth)
        
        val child1 = TrieNode()
        rootNode.addChild("level1", child1)
        assertEquals(1, child1.depth)
        
        val child2 = TrieNode()
        child1.addChild("level2", child2)
        assertEquals(2, child2.depth)
    }
    
    @Test
    fun `test clearChildren`() {
        rootNode.addChild("test1", TrieNode())
        rootNode.addChild("test2", TrieNode())
        rootNode.addChild("test3", TrieNode())
        
        assertEquals(3, rootNode.getChildCount())
        assertFalse(rootNode.isLeaf())
        
        rootNode.clearChildren()
        
        assertEquals(0, rootNode.getChildCount())
        assertTrue(rootNode.isLeaf())
        assertTrue(rootNode.children.isEmpty())
    }
    
    @Test
    fun `test getSubtreeSize`() {
        // 单个节点
        assertEquals(1, rootNode.getSubtreeSize())
        
        // 添加子节点
        val child1 = TrieNode()
        rootNode.addChild("child1", child1)
        assertEquals(2, rootNode.getSubtreeSize())
        
        // 添加孙子节点
        val grandchild = TrieNode()
        child1.addChild("grandchild", grandchild)
        assertEquals(3, rootNode.getSubtreeSize())
        
        // 添加更多子节点
        rootNode.addChild("child2", TrieNode())
        assertEquals(4, rootNode.getSubtreeSize())
    }
    
    @Test
    fun `test deepCopy`() {
        // 设置原始节点的状态
        rootNode.isEnd = true
        rootNode.validator = mockValidator
        rootNode.setMetadata("key1", "value1")
        rootNode.setMetadata("key2", 42)
        
        // 添加子节点
        val child = TrieNode()
        child.isEnd = true
        child.setMetadata("childKey", "childValue")
        rootNode.addChild("child", child)
        
        // 执行深拷贝
        val copy = rootNode.deepCopy()
        
        // 验证拷贝结果
        assertEquals(rootNode.isEnd, copy.isEnd)
        assertEquals(rootNode.validator, copy.validator)
        assertEquals(rootNode.depth, copy.depth)
        assertEquals(rootNode.getChildCount(), copy.getChildCount())
        
        // 验证元数据拷贝
        assertEquals("value1", copy.getMetadata("key1"))
        assertEquals(42, copy.getMetadata("key2"))
        
        // 验证子节点拷贝
        assertTrue(copy.hasChild("child"))
        val copiedChild = copy.getChild("child")!!
        assertEquals(child.isEnd, copiedChild.isEnd)
        assertEquals("childValue", copiedChild.getMetadata("childKey"))
        
        // 验证是深拷贝而不是浅拷贝
        assertNotSame(rootNode, copy)
        assertNotSame(child, copiedChild)
    }
    
    @Test
    fun `test toString`() {
        rootNode.depth = 2
        rootNode.isEnd = true
        rootNode.validator = mockValidator
        rootNode.addChild("test", TrieNode())
        
        val stringRepresentation = rootNode.toString()
        assertTrue(stringRepresentation.contains("TrieNode"))
        assertTrue(stringRepresentation.contains("depth=2"))
        assertTrue(stringRepresentation.contains("isEnd=true"))
        assertTrue(stringRepresentation.contains("childCount=1"))
        assertTrue(stringRepresentation.contains("hasValidator=true"))
    }
    
    @Test
    fun `test equals and hashCode`() {
        val node1 = TrieNode()
        val node2 = TrieNode()
        
        // 初始状态应该相等
        assertEquals(node1, node2)
        assertEquals(node1.hashCode(), node2.hashCode())
        
        // 修改一个节点
        node1.isEnd = true
        assertNotEquals(node1, node2)
        
        // 使另一个节点相同
        node2.isEnd = true
        assertEquals(node1, node2)
        assertEquals(node1.hashCode(), node2.hashCode())
        
        // 添加子节点
        node1.addChild("test", TrieNode())
        assertNotEquals(node1, node2)
        
        node2.addChild("test", TrieNode())
        assertEquals(node1, node2)
    }
    
    @Test
    fun `test complex tree structure`() {
        // 构建复杂的树结构: say -> hello -> world
        val sayNode = TrieNode()
        rootNode.addChild("say", sayNode)
        
        val helloNode = TrieNode()
        sayNode.addChild("hello", helloNode)
        
        val worldNode = TrieNode()
        worldNode.isEnd = true
        helloNode.addChild("world", worldNode)
        
        // 验证结构
        assertEquals(4, rootNode.getSubtreeSize()) // root + say + hello + world
        assertTrue(rootNode.hasChild("say"))
        assertTrue(sayNode.hasChild("hello"))
        assertTrue(helloNode.hasChild("world"))
        assertTrue(worldNode.isEnd)
        assertTrue(worldNode.isLeaf())
        
        // 验证深度
        assertEquals(0, rootNode.depth)
        assertEquals(1, sayNode.depth)
        assertEquals(2, helloNode.depth)
        assertEquals(3, worldNode.depth)
    }
}
