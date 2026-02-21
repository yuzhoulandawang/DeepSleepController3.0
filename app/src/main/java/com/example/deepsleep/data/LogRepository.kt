package com.example.deepsleep.data

import com.example.deepsleep.model.LogEntry
import com.example.deepsleep.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

object LogRepository {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs
    
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    /**
     * 添加日志条目
     */
    fun addLog(level: LogLevel, tag: String, message: String, throwable: String? = null) {
        val timestamp = System.currentTimeMillis()
        val logEntry = LogEntry(
            timestamp = timestamp,
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        _logs.value = listOf(logEntry) + _logs.value.take(500) // 保留最近500条
    }
    
    fun info(tag: String, message: String) {
        addLog(LogLevel.INFO, tag, message)
    }
    
    fun debug(tag: String, message: String) {
        addLog(LogLevel.DEBUG, tag, message)
    }
    
    fun error(tag: String, message: String) {
        addLog(LogLevel.ERROR, tag, message)
    }
    
    fun success(tag: String, message: String) {
        addLog(LogLevel.SUCCESS, tag, message)
    }
    
    fun warning(tag: String, message: String) {
        addLog(LogLevel.WARNING, tag, message)
    }
    
    fun fatal(tag: String, message: String, throwable: String? = null) {
        addLog(LogLevel.FATAL, tag, message, throwable)
    }
    
    fun appendLog(message: String) {
        addLog(LogLevel.INFO, "System", message)
    }
    
    fun clearLogs() {
        _logs.value = emptyList()
    }
    
    fun getLogCount(): Int = _logs.value.size
}
