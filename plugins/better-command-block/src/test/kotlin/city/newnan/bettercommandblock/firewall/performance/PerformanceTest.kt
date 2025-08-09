package city.newnan.bettercommandblock.firewall.performance

import city.newnan.bettercommandblock.firewall.scanner.CommandScanner
import city.newnan.bettercommandblock.firewall.trie.CommandTrie
import city.newnan.bettercommandblock.firewall.validators.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * 防火墙性能测试
 *
 * 测试防火墙各组件的性能表现，包括：
 * - 命令扫描器性能
 * - 前缀树匹配性能
 * - 验证器性能
 * - 内存使用情况
 * - 并发性能
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class PerformanceTest {
    
    private lateinit var commandTrie: CommandTrie
    private lateinit var itemValidator: ItemValidator
    private lateinit var coordinateValidator: CoordinateValidator
    private lateinit var selectorValidator: SelectorValidator
    private lateinit var executeValidator: ExecuteValidator
    
    @BeforeEach
    fun setUp() {
        commandTrie = CommandTrie()
        
        // 设置验证器
        itemValidator = ItemValidator()
        coordinateValidator = CoordinateValidator()
        selectorValidator = SelectorValidator()
        executeValidator = ExecuteValidator(commandTrie)
        
        // 构建测试命令树
        buildTestCommandTrie()
    }
    
    private fun buildTestCommandTrie() {
        // 添加基础命令
        val basicCommands = listOf(
            "say", "tell", "msg", "tellraw", "title", "playsound", "particle",
            "time", "weather", "gamerule", "xp", "experience", "effect", "scoreboard"
        )
        
        basicCommands.forEach { command ->
            commandTrie.addCommand(command)
            commandTrie.addCommand("minecraft:$command")
        }
        
        // 添加带验证器的命令
        commandTrie.addCommand("give", itemValidator)
        commandTrie.addCommand("minecraft:give", itemValidator)
        commandTrie.addCommand("summon", itemValidator)
        commandTrie.addCommand("minecraft:summon", itemValidator)
        
        commandTrie.addCommand("tp", coordinateValidator)
        commandTrie.addCommand("minecraft:tp", coordinateValidator)
        commandTrie.addCommand("teleport", coordinateValidator)
        commandTrie.addCommand("minecraft:teleport", coordinateValidator)
        commandTrie.addCommand("setblock", coordinateValidator)
        commandTrie.addCommand("minecraft:setblock", coordinateValidator)
        commandTrie.addCommand("fill", coordinateValidator)
        commandTrie.addCommand("minecraft:fill", coordinateValidator)
        
        commandTrie.addCommand("execute", executeValidator)
        commandTrie.addCommand("minecraft:execute", executeValidator)
    }
    
    @Test
    fun `test command scanner performance`() {
        val testCommands = listOf(
            "say hello world",
            "give @s minecraft:diamond_sword{Enchantments:[{id:sharpness,lvl:5}]} 1",
            "execute as @s at @s positioned ~ ~1 ~ run tp @s ~ ~ ~",
            "fill ~-10 ~-10 ~-10 ~10 ~10 ~10 minecraft:air replace minecraft:stone",
            "summon minecraft:armor_stand ~ ~ ~ {CustomName:\"Test\",NoGravity:1b}"
        )
        
        val iterations = 10000
        var totalTime = 0L
        
        for (command in testCommands) {
            val time = measureNanoTime {
                repeat(iterations) {
                    val scanner = CommandScanner(command)
                    while (scanner.nextToken() != null) { }
                }
            }
            totalTime += time
            
            println("Command: $command")
            println("Time per scan: ${time / iterations}ns")
            println("Scans per second: ${(iterations * 1_000_000_000L) / time}")
            println()
        }
        
        val averageTime = totalTime / (testCommands.size * iterations)
        println("Average scan time: ${averageTime}ns")
        
        // 性能断言：每次扫描应该在1微秒内完成
        assertTrue(averageTime < 1000, "Command scanning should be faster than 1μs per command")
    }
    
    @Test
    fun `test trie matching performance`() {
        val testCommands = listOf(
            "say hello",
            "give @s dirt",
            "tp 0 64 0",
            "execute as @s run say hello",
            "dangerous_command",
            "another_unsafe_command",
            "setblock ~ ~ ~ air",
            "fill ~ ~ ~ ~10 ~10 ~10 stone"
        )
        
        val iterations = 100000
        var totalTime = 0L
        
        for (command in testCommands) {
            val time = measureNanoTime {
                repeat(iterations) {
                    commandTrie.isCommandSafe(command)
                }
            }
            totalTime += time
            
            println("Command: $command")
            println("Time per match: ${time / iterations}ns")
            println("Matches per second: ${(iterations * 1_000_000_000L) / time}")
            println()
        }
        
        val averageTime = totalTime / (testCommands.size * iterations)
        println("Average match time: ${averageTime}ns")
        
        // 性能断言：每次匹配应该在500纳秒内完成
        assertTrue(averageTime < 500, "Trie matching should be faster than 500ns per command")
    }
    
    @Test
    fun `test validator performance`() {
        val testCases = mapOf(
            "ItemValidator" to listOf(
                "minecraft:dirt 32",
                "minecraft:diamond_sword{Enchantments:[{id:sharpness,lvl:5}]} 1",
                "minecraft:command_block 1"
            ),
            "CoordinateValidator" to listOf(
                "0 64 0",
                "~ ~ ~",
                "^10 ^-5 ^20",
                "2000 64 0"
            ),
            "SelectorValidator" to listOf(
                "@s",
                "@p[distance=..10]",
                "@a",
                "player123"
            )
        )
        
        val iterations = 50000
        
        for ((validatorName, commands) in testCases) {
            val validator = when (validatorName) {
                "ItemValidator" -> itemValidator
                "CoordinateValidator" -> coordinateValidator
                "SelectorValidator" -> selectorValidator
                else -> continue
            }
            
            var totalTime = 0L
            
            for (command in commands) {
                val time = measureNanoTime {
                    repeat(iterations) {
                        val scanner = CommandScanner(command)
                        validator.validate(scanner)
                    }
                }
                totalTime += time
                
                println("$validatorName - Command: $command")
                println("Time per validation: ${time / iterations}ns")
                println("Validations per second: ${(iterations * 1_000_000_000L) / time}")
                println()
            }
            
            val averageTime = totalTime / (commands.size * iterations)
            println("$validatorName average validation time: ${averageTime}ns")
            println()
        }
    }
    
    @Test
    fun `test execute validator performance`() {
        val testCommands = listOf(
            "as @s run say hello",
            "as @s at @s run tp ~ ~ ~",
            "positioned 0 64 0 run give @s dirt",
            "as @s positioned ~ ~1 ~ facing ~ ~ ~ run say test",
            "as @s at @s positioned ~ ~1 ~ rotated ~ ~ run tp @s ~ ~ ~"
        )
        
        val iterations = 10000
        var totalTime = 0L
        
        for (command in testCommands) {
            val time = measureNanoTime {
                repeat(iterations) {
                    val scanner = CommandScanner(command)
                    executeValidator.validate(scanner)
                }
            }
            totalTime += time
            
            println("Execute command: $command")
            println("Time per validation: ${time / iterations}ns")
            println("Validations per second: ${(iterations * 1_000_000_000L) / time}")
            println()
        }
        
        val averageTime = totalTime / (testCommands.size * iterations)
        println("Execute validator average validation time: ${averageTime}ns")
        
        // 性能断言：Execute验证应该在10微秒内完成
        assertTrue(averageTime < 10000, "Execute validation should be faster than 10μs per command")
    }
    
    @Test
    fun `test memory usage`() {
        val runtime = Runtime.getRuntime()
        
        // 测试前的内存使用
        runtime.gc()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // 创建大量命令树
        val largeTrie = CommandTrie()
        repeat(10000) { i ->
            largeTrie.addCommand("command$i test arg1 arg2")
        }
        
        // 测试后的内存使用
        runtime.gc()
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        
        val memoryUsed = memoryAfter - memoryBefore
        val memoryPerCommand = memoryUsed / 10000.0
        
        println("Memory used: ${memoryUsed / 1024}KB")
        println("Memory per command: ${memoryPerCommand}bytes")
        println("Trie size: ${largeTrie.size()} nodes")
        
        // 内存断言：每个命令的内存使用应该合理
        assertTrue(memoryPerCommand < 1000, "Memory usage per command should be less than 1KB")
    }
    
    @Test
    fun `test concurrent performance`() {
        val testCommands = listOf(
            "say hello",
            "give @s dirt",
            "tp 0 64 0",
            "execute as @s run say hello",
            "dangerous_command"
        )
        
        val threadCount = 10
        val iterationsPerThread = 10000
        
        val time = measureTimeMillis {
            val threads = (1..threadCount).map { threadId ->
                Thread {
                    repeat(iterationsPerThread) { iteration ->
                        val command = testCommands[iteration % testCommands.size]
                        commandTrie.isCommandSafe(command)
                    }
                }
            }
            
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }
        
        val totalOperations = threadCount * iterationsPerThread
        val operationsPerSecond = (totalOperations * 1000.0) / time
        
        println("Concurrent test results:")
        println("Threads: $threadCount")
        println("Operations per thread: $iterationsPerThread")
        println("Total operations: $totalOperations")
        println("Total time: ${time}ms")
        println("Operations per second: $operationsPerSecond")
        
        // 性能断言：并发性能应该达到一定标准
        assertTrue(operationsPerSecond > 100000, "Concurrent performance should exceed 100k ops/sec")
    }
    
    @Test
    fun `test large command tree performance`() {
        val largeTrie = CommandTrie()
        
        // 构建大型命令树
        val buildTime = measureTimeMillis {
            repeat(1000) { i ->
                largeTrie.addCommand("category$i command$i arg1 arg2")
                largeTrie.addCommand("category$i command$i arg1 arg3")
                largeTrie.addCommand("category$i command$i arg2 arg1")
            }
        }
        
        println("Large trie build time: ${buildTime}ms")
        println("Large trie size: ${largeTrie.size()} nodes")
        println("Large trie commands: ${largeTrie.getAllCommands().size}")
        
        // 测试查找性能
        val testCommands = listOf(
            "category500 command500 arg1 arg2",
            "category999 command999 arg2 arg1",
            "nonexistent command",
            "category0 command0 arg1 arg2"
        )
        
        val iterations = 10000
        var totalTime = 0L
        
        for (command in testCommands) {
            val time = measureNanoTime {
                repeat(iterations) {
                    largeTrie.isCommandSafe(command)
                }
            }
            totalTime += time
        }
        
        val averageTime = totalTime / (testCommands.size * iterations)
        println("Large trie average lookup time: ${averageTime}ns")
        
        // 性能断言：即使在大型树中，查找也应该很快
        assertTrue(averageTime < 1000, "Large trie lookup should be faster than 1μs")
    }
    
    @Test
    fun `test statistics performance impact`() {
        val iterations = 100000
        
        // 测试不收集统计信息的性能
        commandTrie.resetStatistics()
        val timeWithoutStats = measureNanoTime {
            repeat(iterations) {
                commandTrie.isCommandSafe("say hello")
            }
        }
        
        // 测试收集统计信息的性能
        val timeWithStats = measureNanoTime {
            repeat(iterations) {
                commandTrie.isCommandSafe("say hello")
                commandTrie.getStatistics() // 获取统计信息
            }
        }
        
        val overhead = timeWithStats - timeWithoutStats
        val overheadPercentage = (overhead.toDouble() / timeWithoutStats) * 100
        
        println("Time without stats: ${timeWithoutStats / iterations}ns per operation")
        println("Time with stats: ${timeWithStats / iterations}ns per operation")
        println("Statistics overhead: ${overhead / iterations}ns per operation (${String.format("%.2f", overheadPercentage)}%)")
        
        // 性能断言：统计信息的开销应该很小
        assertTrue(overheadPercentage < 50, "Statistics overhead should be less than 50%")
    }
}
