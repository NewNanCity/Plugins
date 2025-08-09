package city.newnan.railarea.utils

import city.newnan.core.utils.text.toComponent
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.toHexString
import city.newnan.railarea.i18n.LanguageKeys
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration

/**
 * 标题显示工具类
 * 
 * 提供各种标题显示功能
 * 
 * @author NewNanCity
 * @since 2.0.0
 */
object TitleUtils {
    
    /**
     * 标题显示模式
     */
    enum class TitleMode {
        ARRIVE,      // 到站
        UNDER_BOARD, // 站台下
        START        // 发车
    }
    
    /**
     * 显示铁路区域相关标题
     */
    fun showRailAreaTitle(
        player: Player,
        area: RailArea,
        mode: TitleMode,
        fadeIn: Int = 5,
        stay: Int = 40,
        fadeOut: Int = 5
    ) {
        val (title, subtitle) = when (mode) {
            TitleMode.ARRIVE -> createArriveTitle(area)
            TitleMode.UNDER_BOARD -> createUnderBoardTitle(area)
            TitleMode.START -> createStartTitle(area)
        }
        
        val titleObj = Title.title(
            title,
            subtitle,
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
            )
        )
        
        player.showTitle(titleObj)
    }
    
    /**
     * 创建到站标题
     */
    private fun createArriveTitle(area: RailArea): Pair<Component, Component> {
        val title = RailAreaPlugin.instance.messager.sprintf(LanguageKeys.Events.Core.ARRIVED_TITLE, area.station.name) // <gold><bold>{0}</bold></gold> 到了
        val otherLines = area.station.lines.filter { it != area.line }
            .joinToString("<gray>,</gray>") { "<color:${it.color.toHexString()}><bold>${it.name}</bold></color:${it.color.toHexString()}>" }
        val subtitle = if (otherLines.isBlank()) {
            RailAreaPlugin.instance.messager.sprintf(LanguageKeys.Events.Core.ARRIVED_SUBTITLE_SWITCHABLE, otherLines) // 可换乘 {0}
        } else {
            RailAreaPlugin.instance.messager.sprintf(LanguageKeys.Events.Core.ARRIVED_SUBTITLE_SOLO, area.line.name, area.line.color.toHexString(), otherLines) // 感谢您乘坐牛腩地铁 <color:{1}>{0}</color:{1}>
        }
        return title to subtitle
    }
    
    /**
     * 创建站台标题
     */
    private fun createUnderBoardTitle(area: RailArea): Pair<Component, Component> {
        val title = RailAreaPlugin.instance.messager.sprintf(LanguageKeys.Events.Core.UNDER_BOARD_TITLE, area.line.name, area.line.color.toHexString(), area.station.name) // 牛腩地铁 <color:{1}>{0}</color:{1}> {2}
        val destinationStationName = if (area.reverse) area.line.stations.first().name else area.line.stations.last().name
        val nextStationName = area.nextStation?.name
        val subtitle = if (nextStationName == null) {
            RailAreaPlugin.instance.messager.sprintf(LanguageKeys.Events.Core.UNDER_BOARD_SUBTITLE_TERMINAL) // <orange><bold>本站是终点站，乘车请到对侧站台</bold></orange>
        } else {
            RailAreaPlugin.instance.messager.sprintf(LanguageKeys.Events.Core.UNDER_BOARD_SUBTITLE, destinationStationName, nextStationName) // <gray><bold>开往 <purple>{0}</purple> 方向, 下一站: <purple>{1}</purple></bold></gray>
        }
        return title to subtitle
    }
    
    /**
     * 创建发车标题
     */
    private fun createStartTitle(area: RailArea): Pair<Component, Component> {
        val nextStation = area.nextStation ?: return "嗯?".toComponent() to "<red>你是怎么刷到这个的</red>".toComponent()
        val title = RailAreaPlugin.instance.messager.sprintf(LanguageKeys.Events.Core.START_TITLE, nextStation.name) // 下一站 <gold><bold>{0}</bold></gold>
        val otherLines = nextStation.lines.filter { it != area.line }
            .joinToString("<gray>,</gray>") { "<color:${it.color.toHexString()}><bold>${it.name}</bold></color:${it.color.toHexString()}>" }
        val subtitle = if (otherLines.isBlank()) {
            RailAreaPlugin.instance.messager.sprintf(LanguageKeys.Events.Core.START_SUBTITLE_SWITCHABLE, otherLines) // 可换乘 {0}
        } else {
            RailAreaPlugin.instance.messager.sprintf(LanguageKeys.Events.Core.START_SUBTITLE_SOLO, area.line.name) // 空消息，无任何内容
        }
        return title to subtitle
    }
}