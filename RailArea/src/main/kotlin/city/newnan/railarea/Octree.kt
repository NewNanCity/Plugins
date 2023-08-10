package city.newnan.railarea

data class Point3D(val x: Int, val y: Int, val z: Int)

data class Range3D(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int) {
    fun contains(point: Point3D): Boolean =
        point.x in minX..maxX && point.y in minY .. maxY && point.z in minZ .. maxZ

    fun intersects(other: Range3D): Boolean =
        !(other.maxX < minX || other.minX > maxX || other.maxY < minY
                || other.minY > maxY || other.maxZ < minZ || other.minZ > maxZ)
}

class Octree(private val range: Range3D, private val depth: Int = 0, dividableInit: Boolean? = null) {
    // 非叶子节点最多则包含八个子树；叶子节点包含的是 range 节点
    private var children = mutableListOf<Octree>()
    // 是否是一个可分的结点, 达到最深层次或者为 range 节点时不可分
    private var dividable = dividableInit ?: (depth < MAX_DEPTH)
    // 这是否是一个叶子结点
    private var leaf = true
    // 包含 range 节点的数量
    private var size = 0

    fun insert(range: Range3D) {
        if (!this.range.intersects(range)) return
        size++
        if (!dividable) {
            // 不可分节点，下面全都是 range 节点
            if (children.all { it.range != range })
                children.add(Octree(range, -1, false))
            return
        }
        if (leaf) {
            // 叶子结点
            if (size <= MAX_LEAF_SIZE) {
                // 叶子结点未满，直接插入
                if (children.all { it.range != range })
                    children.add(Octree(range, -1, false))
                return
            } else {
                // 叶子结点已满，分割
                val ranges = children
                dividable = divide()
                if (!dividable) {
                    // 分割失败，不再细分
                    if (children.all { it.range != range })
                        children.add(Octree(range, -1, false))
                    return
                }
                leaf = false
                // 分割成功，将原来的叶子结点插入
                children.forEach { child -> ranges.forEach { child.insert(it.range) } }
            }
        }
        // 非叶子结点
        children.forEach { it.insert(range) }
    }

    fun remove(range: Range3D) {
        if (!this.range.intersects(range)) return
        size--
        if (!leaf) {
            // 非叶子结点
            if (size < MIN_LEAF_SIZE) {
                // range 节点数量小于最小数量，合并
                val ranges = mutableSetOf<Octree>()
                children.forEach { it.merge(ranges) }
                children = ranges.toMutableList()
                leaf = true
            } else {
                // range 节点数量大于最小数量，直接递归删除
                children.forEach { it.remove(range) }
                return
            }
        }
        // 叶子结点
        children.removeIf { it.range == range }
    }

    fun contains(point: Point3D): Boolean {
        //println(if (depth >= 0) "${"  ".repeat(depth)}|$range" else range)
        if (!range.contains(point)) return false
        if (depth < 0) return true
        return children.any { it.contains(point) }
    }

    fun ranges(point: Point3D, set: MutableSet<Range3D> = mutableSetOf()): Set<Range3D> {
        //println(if (depth >= 0) "${"  ".repeat(depth)}|$range" else range)
        if (!range.contains(point)) return set
        if (depth < 0) set.add(range)
        else children.forEach { it.ranges(point, set) }
        return set
    }

    fun firstRange(point: Point3D): Range3D? {
        //println(if (depth >= 0) "${"  ".repeat(depth)}|$range" else range)
        if (!range.contains(point)) return null
        if (depth < 0) return range
        for (child in children) {
            val range = child.firstRange(point)
            if (range != null) return range
        }
        return null
    }

    private fun divide(): Boolean {
        val flag = (if ((range.maxX - range.minX) > 15) 0b100 else 0) or
                   (if ((range.maxY - range.minY) > 15) 0b010 else 0) or
                   (if ((range.maxZ - range.minZ) > 15) 0b001 else 0)

        //println("${"  ".repeat(depth)}flag:${Integer.toBinaryString(flag)}   range:$range  size:(${
        //    range.maxX - range.minX}, ${range.maxY - range.minY}, ${range.maxZ - range.minZ})")

        if (flag == 0) return false

        val d = depth + 1
        val midX = if (flag and 0b100 == 0b100) ((range.minX + range.maxX) shr 1) else range.maxX
        val midY = if (flag and 0b010 == 0b010) ((range.minY + range.maxY) shr 1) else range.maxY
        val midZ = if (flag and 0b001 == 0b001) ((range.minZ + range.maxZ) shr 1) else range.maxZ
        val midXp1 = midX + 1
        val midYp1 = midY + 1
        val midZp1 = midZ + 1

        children = mutableListOf()
        children.add(Octree(Range3D(range.minX, range.minY, range.minZ, midX, midY, midZ), d))
        if (flag and 0b100 == 0b100)
            children.add(Octree(Range3D(midXp1, range.minY, range.minZ, range.maxX, midY, midZ), d))
        if (flag and 0b010 == 0b010)
            children.add(Octree(Range3D(range.minX, midYp1, range.minZ, midX, range.maxY, midZ), d))
        if (flag and 0b001 == 0b001)
            children.add(Octree(Range3D(range.minX, range.minY, midZp1, midX, midY, range.maxZ), d))
        if (flag and 0b110 == 0b110)
            children.add(Octree(Range3D(midXp1, midYp1, range.minZ, range.maxX, range.maxY, midZ), d))
        if (flag and 0b101 == 0b101)
            children.add(Octree(Range3D(midXp1, range.minY, midZp1, range.maxX, midY, range.maxZ), d))
        if (flag and 0b011 == 0b011)
            children.add(Octree(Range3D(range.minX, midYp1, midZp1, midX, range.maxY, range.maxZ), d))
        if (flag and 0b111 == 0b111)
            children.add(Octree(Range3D(midXp1, midYp1, midZp1, range.maxX, range.maxY, range.maxZ), d))

        return true
    }

    private fun merge(ranges: MutableSet<Octree>) {
        if (leaf) ranges.addAll(children)
        else children.forEach { it.merge(ranges) }
    }

    companion object {
        const val MAX_DEPTH = 16
        const val MAX_LEAF_SIZE = 12
        const val MIN_LEAF_SIZE = 8
    }
}
