package com.example.deepsleep.model

data class AppSettings(
    // ========== 深度 Doze 配置 ==========
    val deepDozeEnabled: Boolean = true,
    val deepDozeDelaySeconds: Int = 5,
    val deepDozeForceMode: Boolean = false,
    
    // ========== 深度睡眠配置（Hook 版本） ==========
    val deepSleepHookEnabled: Boolean = true,
    val deepSleepDelaySeconds: Int = 1,
    val deepSleepBlockExit: Boolean = true,
    val deepSleepCheckInterval: Int = 10,
    
    // ========== 系统省电模式联动 ==========
    val enablePowerSaverOnSleep: Boolean = false,
    val disablePowerSaverOnWake: Boolean = true,
    
    // ========== CPU 调度优化配置 ==========
    val cpuOptimizationEnabled: Boolean = false,
    val cpuModeOnScreen: String = "daily",
    val cpuModeOnScreenOff: String = "standby",
    val autoSwitchCpuMode: Boolean = true,
    val allowManualCpuMode: Boolean = true,
    
    // CPU 参数 - 日常模式
    val dailyUpRateLimit: Int = 1000,
    val dailyDownRateLimit: Int = 500,
    val dailyHiSpeedLoad: Int = 85,
    val dailyTargetLoads: Int = 80,
    
    // CPU 参数 - 待机模式
    val standbyUpRateLimit: Int = 5000,
    val standbyDownRateLimit: Int = 0,
    val standbyHiSpeedLoad: Int = 95,
    val standbyTargetLoads: Int = 90,
    
    // CPU 参数 - 默认模式
    val defaultUpRateLimit: Int = 0,
    val defaultDownRateLimit: Int = 0,
    val defaultHiSpeedLoad: Int = 90,
    val defaultTargetLoads: Int = 90,
    
    // CPU 参数 - 性能模式
    val perfUpRateLimit: Int = 0,
    val perfDownRateLimit: Int = 0,
    val perfHiSpeedLoad: Int = 75,
    val perfTargetLoads: Int = 70,
    
    // ========== 进程压制配置 ==========
    val suppressEnabled: Boolean = true,
    val suppressMode: String = "conservative",
    val suppressOomValue: Int = 800,
    val suppressInterval: Int = 60,
    val debounceInterval: Int = 3,
    
    // ========== 后台优化配置 ==========
    val backgroundOptimizationEnabled: Boolean = true,
    
    // ========== 其他配置 ==========
    val bootStartEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    
    // ========== 兼容旧版本 ==========
    val deepSleepEnabled: Boolean = true,
    val cpuMode: String = "daily",
    val autoCpuMode: Boolean = true
)
