package com.example.deepsleep.root

import com.example.deepsleep.data.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object DeepSleepController {
    private const val TAG = "DeepSleepController"
    private var isInDeepSleep = false
    private var checkJob: Job? = null
    
    // 进入深度睡眠
    suspend fun enterDeepSleep(
        blockExit: Boolean,
        checkIntervalSeconds: Int
    ): Boolean {
        return try {
            LogRepository.info(TAG, "准备进入深度睡眠...")
            LogRepository.debug(TAG, "参数: blockExit=$blockExit, checkInterval=$checkIntervalSeconds")
            
            // 1. 进入 Doze 模式
            val dozeSuccess = DozeController.forceIdle()
            if (!dozeSuccess) {
                LogRepository.error(TAG, "Doze 进入失败")
                return false
            }
            LogRepository.success(TAG, "Doze 模式已进入")
            
            // 2. 屏蔽自动退出
            if (blockExit) {
                LogRepository.info(TAG, "屏蔽移动检测...")
                RootCommander.exec("settings put global motion_detection_enabled 0")
                LogRepository.success(TAG, "移动检测已屏蔽")
                
                LogRepository.info(TAG, "限制唤醒源...")
                RootCommander.exec("echo 0 > /sys/power/autosleep")
                LogRepository.success(TAG, "唤醒源已限制")
            }
            
            // 3. 标记状态
            RootCommander.exec("echo 'active' > /data/local/tmp/deep_sleep_status")
            LogRepository.success(TAG, "深度睡眠已进入")
            
            isInDeepSleep = true
            
            // 4. 启动状态检查
            if (blockExit) {
                startStatusCheck(checkIntervalSeconds)
            }
            
            true
        } catch (e: Exception) {
            // 使用 fatal 方法传递异常堆栈
            LogRepository.fatal(TAG, "进入深度睡眠失败: ${e.message}", e.stackTraceToString())
            false
        }
    }
    
    // 退出深度睡眠
    suspend fun exitDeepSleep(): Boolean {
        return try {
            LogRepository.info(TAG, "准备退出深度睡眠...")
            
            // 1. 停止状态检查
            checkJob?.cancel()
            checkJob = null
            
            // 2. 恢复移动检测
            LogRepository.info(TAG, "恢复移动检测...")
            RootCommander.exec("settings put global motion_detection_enabled 1")
            
            // 3. 恢复唤醒源
            LogRepository.info(TAG, "恢复唤醒源...")
            RootCommander.exec("echo 1 > /sys/power/autosleep")
            
            // 4. 清除标记
            RootCommander.exec("rm -f /data/local/tmp/deep_sleep_status")
            
            // 5. 退出 Doze 模式
            val dozeSuccess = DozeController.step()
            if (dozeSuccess) {
                LogRepository.success(TAG, "已退出深度睡眠")
            } else {
                LogRepository.warning(TAG, "退出 Doze 模式失败")
            }
            
            isInDeepSleep = false
            
            true
        } catch (e: Exception) {
            LogRepository.fatal(TAG, "退出深度睡眠失败: ${e.message}", e.stackTraceToString())
            false
        }
    }
    
    // 检查是否在深度睡眠状态（版本1）
    fun isInDeepSleep(): Boolean {
        return isInDeepSleep
    }
    
    // 启动状态检查循环
    private fun startStatusCheck(intervalSeconds: Int) {
        checkJob?.cancel()
        checkJob = CoroutineScope(Dispatchers.IO).launch {
            var checkCount = 0
            while (isActive) {
                delay(intervalSeconds * 1000L)
                checkCount++
                
                val isIdle = checkDozeStatus()
                
                if (isIdle) {
                    if (checkCount % 10 == 0) {  // 每10次检查记录一次，避免日志过多
                        LogRepository.debug(TAG, "状态检查 #$checkCount: 正常")
                    }
                } else {
                    LogRepository.warning(TAG, "状态检查 #$checkCount: 检测到意外退出，正在重新进入...")
                    // 重新进入，使用相同参数
                    enterDeepSleep(blockExit = true, checkIntervalSeconds = intervalSeconds)
                    LogRepository.success(TAG, "已重新进入深度睡眠")
                    checkCount = 0  // 重置计数
                }
            }
        }
    }
    
    // 检查 Doze 状态
    private suspend fun checkDozeStatus(): Boolean {
        return try {
            val result = RootCommander.exec("dumpsys deviceidle")
            result.out.contains("IDLE")
        } catch (e: Exception) {
            false
        }
    }
    
    // 停止所有检查
    fun stopAll() {
        checkJob?.cancel()
        checkJob = null
        isInDeepSleep = false
    }

    // 检查深度睡眠状态（使用 exec 替代 execute）
    fun checkDeepSleepStatus(): Boolean {
        try {
            val powerState = RootCommander.exec("cat /sys/power/state").out
            val isInDeepSleep = powerState.any { it.contains("mem") || it.contains("freeze") }
            
            if (isInDeepSleep) {
                LogRepository.info(TAG, "当前处于深度睡眠状态")
                return true
            } else {
                LogRepository.debug(TAG, "当前未处于深度睡眠状态")
                return false
            }
        } catch (e: Exception) {
            LogRepository.error(TAG, "检查状态失败: ${e.message}")
            return false
        }
    }
    
    // 移除重复的 isInDeepSleep() 函数，保留上面的一个
    
    fun forceMaintainDeepSleep() {
        try {
            if (!checkDeepSleepStatus()) {
                LogRepository.warning(TAG, "重新进入深度睡眠")
                // 需要提供参数，这里使用默认值
                // 注意：enterDeepSleep 是挂起函数，不能在非挂起函数中直接调用
                // 这里需要启动一个协程，但 forceMaintainDeepSleep 不是挂起函数，可能导致问题。
                // 临时改为启动协程，但调用者需确保有合适的作用域。
                CoroutineScope(Dispatchers.IO).launch {
                    enterDeepSleep(blockExit = true, checkIntervalSeconds = 10)
                }
                return
            }
            
            RootCommander.exec("echo 'mem' > /sys/power/state", timeout = 5)
            
            val wakeupProcesses = listOf("alarmd", "netd", "system_server", "com.android.systemui")
            for (process in wakeupProcesses) {
                try {
                    RootCommander.exec("renice 19 \$(pidof $process) 2>/dev/null", timeout = 3)
                } catch (e: Exception) {
                    // 忽略单个进程失败
                }
            }
            
            LogRepository.success(TAG, "已强制维持深度睡眠")
        } catch (e: Exception) {
            LogRepository.error(TAG, "维持失败: ${e.message}")
        }
    }
    
    fun getDeepSleepStatusInfo(): String {
        try {
            val powerState = RootCommander.exec("cat /sys/power/state").out.firstOrNull()?.trim() ?: "unknown"
            val wakeupCount = RootCommander.exec("cat /sys/power/wakeup_count").out.firstOrNull()?.trim() ?: "unknown"
            val hookStatus = if (checkDeepSleepStatus()) "已激活" else "未激活"
            
            return "深度睡眠状态信息:\n电源状态: $powerState\n唤醒计数: $wakeupCount\nHook状态: $hookStatus"
        } catch (e: Exception) {
            return "获取失败: ${e.message}"
        }
    }

    /**
     * 检查当前是否处于深度睡眠状态
     */
    suspend fun isDeepSleeping(): Boolean {
        return try {
            val result = RootCommander.exec("dumpsys deviceidle | grep 'Idle'")
            result.out.any { it.contains("IDLE") || it.contains("true") }
        } catch (e: Exception) {
            false
        }
    }
}
