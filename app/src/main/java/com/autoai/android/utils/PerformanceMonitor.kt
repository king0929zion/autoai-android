package com.autoai.android.utils

import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * 性能监控工具
 */
object PerformanceMonitor {
    
    private val timings = mutableMapOf<String, MutableList<Long>>()
    
    /**
     * 测量代码块执行时间
     */
    inline fun <T> measure(tag: String, block: () -> T): T {
        var result: T
        val duration = measureTimeMillis {
            result = block()
        }
        
        recordTiming(tag, duration)
        Timber.tag("Performance").d("$tag: ${duration}ms")
        
        return result
    }
    
    /**
     * 测量挂起函数执行时间
     */
    suspend inline fun <T> measureSuspend(tag: String, crossinline block: suspend () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        recordTiming(tag, duration)
        Timber.tag("Performance").d("$tag: ${duration}ms")
        
        return result
    }
    
    /**
     * 记录时间
     */
    private fun recordTiming(tag: String, duration: Long) {
        timings.getOrPut(tag) { mutableListOf() }.add(duration)
    }
    
    /**
     * 获取统计信息
     */
    fun getStats(tag: String): Stats? {
        val durations = timings[tag] ?: return null
        if (durations.isEmpty()) return null
        
        return Stats(
            tag = tag,
            count = durations.size,
            total = durations.sum(),
            average = durations.average().toLong(),
            min = durations.minOrNull() ?: 0,
            max = durations.maxOrNull() ?: 0
        )
    }
    
    /**
     * 获取所有统计信息
     */
    fun getAllStats(): List<Stats> {
        return timings.keys.mapNotNull { getStats(it) }
    }
    
    /**
     * 清除统计数据
     */
    fun clear() {
        timings.clear()
    }
    
    /**
     * 打印统计报告
     */
    fun printReport() {
        Timber.tag("Performance").d("=== 性能统计报告 ===")
        getAllStats().sortedByDescending { it.total }.forEach { stats ->
            Timber.tag("Performance").d(
                "${stats.tag}: 平均=${stats.average}ms, " +
                "最小=${stats.min}ms, 最大=${stats.max}ms, " +
                "总计=${stats.total}ms (${stats.count}次)"
            )
        }
    }
    
    data class Stats(
        val tag: String,
        val count: Int,
        val total: Long,
        val average: Long,
        val min: Long,
        val max: Long
    )
}
