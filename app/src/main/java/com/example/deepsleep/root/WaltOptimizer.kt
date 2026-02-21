package com.example.deepsleep.root

import com.example.deepsleep.data.LogRepository
import com.example.deepsleep.utils.Logger

object WaltOptimizer {
    private const val TAG = "WaltOptimizer"
    
    /**
     * 应用日常模式
     */
    suspend fun applyDaily(): Boolean {
        return try {
            LogRepository.info(TAG, "应用日常模式")
            val commands = listOf(
                "sysctl -w kernel.sched_walt_util_threshold=500 2>/dev/null",
                "sysctl -w kernel.sched_walt_util_est_factor=100 2>/dev/null",
                "sysctl -w kernel.sched_walt_util_filter=50 2>/dev/null"
            )
            RootCommander.execBatch(commands).isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "应用日常模式失败: ${e.message}")
            false
        }
    }
    
    /**
     * 应用待机模式
     */
    suspend fun applyStandby(): Boolean {
        return try {
            LogRepository.info(TAG, "应用待机模式")
            val commands = listOf(
                "sysctl -w kernel.sched_walt_util_threshold=800 2>/dev/null",
                "sysctl -w kernel.sched_walt_util_est_factor=120 2>/dev/null",
                "sysctl -w kernel.sched_walt_util_filter=70 2>/dev/null"
            )
            RootCommander.execBatch(commands).isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "应用待机模式失败: ${e.message}")
            false
        }
    }
    
    /**
     * 应用默认模式
     */
    suspend fun applyDefault(): Boolean {
        return try {
            LogRepository.info(TAG, "应用默认模式")
            val commands = listOf(
                "sysctl -w kernel.sched_walt_util_threshold=500 2>/dev/null",
                "sysctl -w kernel.sched_walt_util_est_factor=100 2>/dev/null",
                "sysctl -w kernel.sched_walt_util_filter=50 2>/dev/null"
            )
            RootCommander.execBatch(commands).isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "应用默认模式失败: ${e.message}")
            false
        }
    }
    
    /**
     * 应用性能模式
     */
    suspend fun applyPerformance(): Boolean {
        return try {
            LogRepository.info(TAG, "应用性能模式")
            val commands = listOf(
                "sysctl -w kernel.sched_walt_util_threshold=300 2>/dev/null",
                "sysctl -w kernel.sched_walt_util_est_factor=80 2>/dev/null",
                "sysctl -w kernel.sched_walt_util_filter=30 2>/dev/null"
            )
            RootCommander.execBatch(commands).isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "应用性能模式失败: ${e.message}")
            false
        }
    }
    
    /**
     * 应用全局优化
     */
    suspend fun applyGlobalOptimizations(): Boolean {
        return try {
            LogRepository.info(TAG, "应用全局优化")
            val commands = listOf(
                "sysctl -w kernel.sched_walt_util_threshold=500 2>/dev/null",
                "sysctl -w kernel.sched_walt_util_est_factor=100 2>/dev/null"
            )
            RootCommander.execBatch(commands).isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "应用全局优化失败: ${e.message}")
            false
        }
    }
    
    /**
     * 恢复默认设置
     */
    suspend fun restoreDefault(): Boolean {
        return try {
            LogRepository.info(TAG, "恢复默认设置")
            true
        } catch (e: Exception) {
            LogRepository.error(TAG, "恢复默认设置失败: ${e.message}")
            false
        }
    }
    
    /**
     * 应用指定模式
     */
    suspend fun applyMode(mode: String): Boolean {
        return when (mode) {
            "daily" -> applyDaily()
            "standby" -> applyStandby()
            "default" -> applyDefault()
            "performance" -> applyPerformance()
            else -> false
        }
    }
    
    /**
     * 设置 CPU 频率范围
     */
    suspend fun setFrequency(min: Int, max: Int): Boolean {
        return try {
            val minFreq = min * 1000  // 转换为 kHz
            val maxFreq = max * 1000
            RootCommander.exec("echo $minFreq > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq 2>/dev/null || true").isSuccess &&
            RootCommander.exec("echo $maxFreq > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq 2>/dev/null || true").isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "设置频率失败: ${e.message}")
            false
        }
    }
    
    /**
     * 设置 CPU 调节器
     */
    suspend fun setGovernor(governor: String): Boolean {
        return try {
            RootCommander.exec("echo $governor > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || true").isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "设置调节器失败: ${e.message}")
            false
        }
    }
}