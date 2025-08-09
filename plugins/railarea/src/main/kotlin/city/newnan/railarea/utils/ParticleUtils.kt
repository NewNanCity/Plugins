package city.newnan.railarea.utils

import city.newnan.railarea.spatial.Range3D
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player

/**
 * 粒子效果工具类
 * 
 * 提供区域边界显示等粒子效果功能
 */
object ParticleUtils {
    
    /**
     * 显示区域边界粒子效果
     * 
     * @param player 玩家
     * @param world 世界
     * @param range3D 区域范围
     * @param color 粒子颜色（可选，默认为绿色）
     */
    fun showRegionBoundary(
        player: Player,
        world: World,
        range3D: Range3D,
        color: Color = Color.GREEN
    ) {
        // 计算区域的8个顶点
        val minX = range3D.minX.toDouble()
        val minY = range3D.minY.toDouble()
        val minZ = range3D.minZ.toDouble()
        val maxX = range3D.maxX.toDouble()
        val maxY = range3D.maxY.toDouble()
        val maxZ = range3D.maxZ.toDouble()
        
        // 创建粒子数据
        val dustOptions = Particle.DustOptions(color, 1.0f)
        
        // 显示边界线条
        showBoundaryLines(player, world, minX, minY, minZ, maxX, maxY, maxZ, dustOptions)
    }
    
    /**
     * 显示区域边界线条
     */
    private fun showBoundaryLines(
        player: Player,
        world: World,
        minX: Double, minY: Double, minZ: Double,
        maxX: Double, maxY: Double, maxZ: Double,
        dustOptions: Particle.DustOptions
    ) {
        val step = 0.5 // 粒子间距
        
        // 底面四条边
        drawLine(player, world, minX, minY, minZ, maxX, minY, minZ, step, dustOptions) // 前边
        drawLine(player, world, maxX, minY, minZ, maxX, minY, maxZ, step, dustOptions) // 右边
        drawLine(player, world, maxX, minY, maxZ, minX, minY, maxZ, step, dustOptions) // 后边
        drawLine(player, world, minX, minY, maxZ, minX, minY, minZ, step, dustOptions) // 左边
        
        // 顶面四条边
        drawLine(player, world, minX, maxY, minZ, maxX, maxY, minZ, step, dustOptions) // 前边
        drawLine(player, world, maxX, maxY, minZ, maxX, maxY, maxZ, step, dustOptions) // 右边
        drawLine(player, world, maxX, maxY, maxZ, minX, maxY, maxZ, step, dustOptions) // 后边
        drawLine(player, world, minX, maxY, maxZ, minX, maxY, minZ, step, dustOptions) // 左边
        
        // 四条竖直边
        drawLine(player, world, minX, minY, minZ, minX, maxY, minZ, step, dustOptions) // 前左
        drawLine(player, world, maxX, minY, minZ, maxX, maxY, minZ, step, dustOptions) // 前右
        drawLine(player, world, maxX, minY, maxZ, maxX, maxY, maxZ, step, dustOptions) // 后右
        drawLine(player, world, minX, minY, maxZ, minX, maxY, maxZ, step, dustOptions) // 后左
    }
    
    /**
     * 在两点之间绘制粒子线条
     */
    private fun drawLine(
        player: Player,
        world: World,
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        step: Double,
        dustOptions: Particle.DustOptions
    ) {
        val distance = kotlin.math.sqrt(
            (x2 - x1) * (x2 - x1) + 
            (y2 - y1) * (y2 - y1) + 
            (z2 - z1) * (z2 - z1)
        )
        
        val steps = (distance / step).toInt()
        if (steps == 0) return
        
        val dx = (x2 - x1) / steps
        val dy = (y2 - y1) / steps
        val dz = (z2 - z1) / steps
        
        for (i in 0..steps) {
            val x = x1 + dx * i
            val y = y1 + dy * i
            val z = z1 + dz * i
            
            val location = Location(world, x, y, z)
            
            // 只对指定玩家显示粒子效果
            player.spawnParticle(Particle.REDSTONE, location, 1, dustOptions)
        }
    }
    
    /**
     * 显示停靠点标记
     * 
     * @param player 玩家
     * @param world 世界
     * @param x 停靠点X坐标
     * @param y 停靠点Y坐标
     * @param z 停靠点Z坐标
     * @param color 粒子颜色（可选，默认为黄色）
     */
    fun showStopPointMarker(
        player: Player,
        world: World,
        x: Int, y: Int, z: Int,
        color: Color = Color.YELLOW
    ) {
        val location = Location(world, x + 0.5, y + 1.0, z + 0.5)
        val dustOptions = Particle.DustOptions(color, 2.0f)
        
        // 显示一个圆形标记
        for (i in 0..360 step 10) {
            val radians = Math.toRadians(i.toDouble())
            val offsetX = kotlin.math.cos(radians) * 0.5
            val offsetZ = kotlin.math.sin(radians) * 0.5
            
            val particleLocation = location.clone().add(offsetX, 0.0, offsetZ)
            player.spawnParticle(Particle.REDSTONE, particleLocation, 1, dustOptions)
        }
        
        // 显示向上的粒子柱
        for (i in 0..10) {
            val particleLocation = location.clone().add(0.0, i * 0.2, 0.0)
            player.spawnParticle(Particle.REDSTONE, particleLocation, 1, dustOptions)
        }
    }
}
