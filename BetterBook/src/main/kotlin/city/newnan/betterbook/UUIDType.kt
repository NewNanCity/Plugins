package city.newnan.betterbook

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.nio.ByteBuffer
import java.util.*

class UUIDDataType : PersistentDataType<ByteArray, UUID> {
    override fun toPrimitive(complex: UUID, context: PersistentDataAdapterContext): ByteArray {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(complex.mostSignificantBits)
        bb.putLong(complex.leastSignificantBits)
        return bb.array()
    }

    override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext): UUID {
        val bb: ByteBuffer = ByteBuffer.wrap(primitive)
        val firstLong: Long = bb.long
        val secondLong: Long = bb.long
        return UUID(firstLong, secondLong)
    }

    override fun getPrimitiveType(): Class<ByteArray> = ByteArray::class.java

    override fun getComplexType(): Class<UUID> = UUID::class.java
}