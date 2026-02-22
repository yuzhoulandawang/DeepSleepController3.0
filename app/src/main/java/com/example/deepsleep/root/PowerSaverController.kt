package com.example.deepsleep.root

import com.example.deepsleep.data.LogRepository

object PowerSaverController {
    private const val TAG = "PowerSaverController"

    suspend fun enablePowerSaver(): Boolean {
        return try {
            LogRepository.info(TAG, "开启系统省电模式")
            RootCommander.exec("settings put global low_power 1").isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "开启系统省电模式失败: ${e.message}")
            false
        }
    }

    suspend fun disablePowerSaver(): Boolean {
        return try {
            LogRepository.info(TAG, "关闭系统省电模式")
            RootCommander.exec("settings put global low_power 0").isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "关闭系统省电模式失败: ${e.message}")
            false
        }
    }

    suspend fun isEnabled(): Boolean {
        return try {
            val result = RootCommander.exec("settings get global low_power")
            result.out.any { it.trim() == "1" }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun enableAggressiveMode(): Boolean {
        return try {
            LogRepository.info(TAG, "启用激进省电模式")
            val commands = listOf(
                "settings put global low_power 1 2>/dev/null",
                "settings put global adaptive_battery_management_enabled 1 2>/dev/null",
                "cmd power set-adaptive-battery-enabled true 2>/dev/null"
            )
            RootCommander.execBatch(commands).isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "启用激进省电模式失败: ${e.message}")
            false
        }
    }

    suspend fun restoreDefaults(): Boolean {
        return try {
            LogRepository.info(TAG, "恢复默认省电设置")
            val commands = listOf(
                "settings put global low_power 0 2>/dev/null",
                "settings put global adaptive_battery_management_enabled 0 2>/dev/null",
                "cmd power set-adaptive-battery-enabled false 2>/dev/null"
            )
            RootCommander.execBatch(commands).isSuccess
        } catch (e: Exception) {
            LogRepository.error(TAG, "恢复默认省电设置失败: ${e.message}")
            false
        }
    }
}