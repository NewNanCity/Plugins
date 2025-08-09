package city.newnan.config.serializers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

/**
 * 序列化器测试类
 *
 * 测试所有自定义序列化器的功能
 *
 * @author Gk0Wk
 * @since 1.0.0
 */
class SerializersTest {

    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerBukkitSerializers()
    }

    @Test
    fun testPositionSerialization() {
        val position = Position(10.5, 64.0, -20.3, "world")
        
        // 序列化
        val json = objectMapper.writeValueAsString(position)
        assertNotNull(json)
        assertTrue(json.contains("10.5"))
        assertTrue(json.contains("64.0"))
        assertTrue(json.contains("-20.3"))
        assertTrue(json.contains("world"))
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, Position::class.java)
        assertEquals(position, deserialized)
    }

    @Test
    fun testBlockPositionSerialization() {
        val blockPosition = BlockPosition(10, 64, -20, "world")
        
        // 序列化
        val json = objectMapper.writeValueAsString(blockPosition)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, BlockPosition::class.java)
        assertEquals(blockPosition, deserialized)
    }

    @Test
    fun testChunkPositionSerialization() {
        val chunkPosition = ChunkPosition(5, -3, "world")
        
        // 序列化
        val json = objectMapper.writeValueAsString(chunkPosition)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, ChunkPosition::class.java)
        assertEquals(chunkPosition, deserialized)
    }

    @Test
    fun testDirectionSerialization() {
        val direction = Direction(90.0f, -45.0f)
        
        // 序列化
        val json = objectMapper.writeValueAsString(direction)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, Direction::class.java)
        assertEquals(direction, deserialized)
    }

    @Test
    fun testPointSerialization() {
        val position = Position(10.5, 64.0, -20.3, "world")
        val direction = Direction(90.0f, -45.0f)
        val point = Point(position, direction)
        
        // 序列化
        val json = objectMapper.writeValueAsString(point)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, Point::class.java)
        assertEquals(point, deserialized)
    }

    @Test
    fun testRegionSerialization() {
        val pos1 = Position(0.0, 0.0, 0.0, "world")
        val pos2 = Position(10.0, 10.0, 10.0, "world")
        val region = Region.of(pos1, pos2)
        
        // 序列化
        val json = objectMapper.writeValueAsString(region)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, Region::class.java)
        assertEquals(region, deserialized)
    }

    @Test
    fun testBlockRegionSerialization() {
        val pos1 = BlockPosition(0, 0, 0, "world")
        val pos2 = BlockPosition(10, 10, 10, "world")
        val region = BlockRegion.of(pos1, pos2)
        
        // 序列化
        val json = objectMapper.writeValueAsString(region)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, BlockRegion::class.java)
        assertEquals(region, deserialized)
    }

    @Test
    fun testChunkRegionSerialization() {
        val pos1 = ChunkPosition(0, 0, "world")
        val pos2 = ChunkPosition(5, 5, "world")
        val region = ChunkRegion.of(pos1, pos2)
        
        // 序列化
        val json = objectMapper.writeValueAsString(region)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, ChunkRegion::class.java)
        assertEquals(region, deserialized)
    }

    @Test
    fun testCircularRegionSerialization() {
        val center = Position(0.0, 64.0, 0.0, "world")
        val region = CircularRegion.of(center, 50.0)
        
        // 序列化
        val json = objectMapper.writeValueAsString(region)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, CircularRegion::class.java)
        assertEquals(region, deserialized)
    }

    @Test
    fun testVector2dSerialization() {
        val vector = Vector2d(1.5, 2.5)
        
        // 序列化
        val json = objectMapper.writeValueAsString(vector)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, Vector2d::class.java)
        assertEquals(vector, deserialized)
    }

    @Test
    fun testVector3dSerialization() {
        val vector = Vector3d(1.5, 2.5, 3.5)
        
        // 序列化
        val json = objectMapper.writeValueAsString(vector)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, Vector3d::class.java)
        assertEquals(vector, deserialized)
    }

    @Test
    fun testVectorPointSerialization() {
        val position = Vector3d(10.5, 64.0, -20.3)
        val direction = Vector2f(90.0f, -45.0f)
        val vectorPoint = VectorPoint(position, direction)
        
        // 序列化
        val json = objectMapper.writeValueAsString(vectorPoint)
        assertNotNull(json)
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, VectorPoint::class.java)
        assertEquals(vectorPoint, deserialized)
    }

    @Test
    fun testSerializableInventorySerialization() {
        val inventory = SerializableInventory("Test Inventory", "test-data")
        
        // 序列化
        val json = objectMapper.writeValueAsString(inventory)
        assertNotNull(json)
        assertTrue(json.contains("Test Inventory"))
        assertTrue(json.contains("test-data"))
        
        // 反序列化
        val deserialized = objectMapper.readValue(json, SerializableInventory::class.java)
        assertEquals(inventory, deserialized)
    }

    @Test
    fun testRegionContains() {
        val pos1 = Position(0.0, 0.0, 0.0, "world")
        val pos2 = Position(10.0, 10.0, 10.0, "world")
        val region = Region.of(pos1, pos2)
        
        assertTrue(region.contains(Position(5.0, 5.0, 5.0, "world")))
        assertFalse(region.contains(Position(15.0, 5.0, 5.0, "world")))
        assertFalse(region.contains(Position(5.0, 5.0, 5.0, "other_world")))
    }

    @Test
    fun testCircularRegionContains() {
        val center = Position(0.0, 64.0, 0.0, "world")
        val region = CircularRegion.of(center, 10.0)
        
        assertTrue(region.contains(Position(5.0, 64.0, 5.0, "world")))
        assertFalse(region.contains(Position(15.0, 64.0, 0.0, "world")))
        assertFalse(region.contains(Position(5.0, 64.0, 5.0, "other_world")))
    }

    @Test
    fun testVectorOperations() {
        val v1 = Vector3d(1.0, 2.0, 3.0)
        val v2 = Vector3d(4.0, 5.0, 6.0)
        
        val sum = v1 + v2
        assertEquals(Vector3d(5.0, 7.0, 9.0), sum)
        
        val diff = v2 - v1
        assertEquals(Vector3d(3.0, 3.0, 3.0), diff)
        
        val scaled = v1 * 2.0
        assertEquals(Vector3d(2.0, 4.0, 6.0), scaled)
        
        val dot = v1.dot(v2)
        assertEquals(32.0, dot, 0.001)
    }
}
