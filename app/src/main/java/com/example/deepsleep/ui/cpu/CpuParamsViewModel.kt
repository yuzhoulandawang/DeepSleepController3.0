package com.example.deepsleep.ui.cpu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsleep.model.CpuParams
import com.example.deepsleep.model.CpuGovernor
import com.example.deepsleep.utils.Logger
import com.example.deepsleep.root.WaltOptimizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * CPU 参数设置 ViewModel
 */
class CpuParamsViewModel(application: Application) : AndroidViewModel(application) {
    private val waltOptimizer = WaltOptimizer()
    
    // 当前 CPU 参数
    private val _currentParams = MutableStateFlow<CpuParams>(CpuParams.DEFAULT_MODE)
    val currentParams: StateFlow<CpuParams> = _currentParams.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 操作结果提示
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()
    
    // 可用频率列表
    private val _availableFreqs = MutableStateFlow<List<Int>>(emptyList())
    val availableFreqs: StateFlow<List<Int>> = _availableFreqs.asStateFlow()
    
    // 可用调度器列表
    private val _availableGovernors = MutableStateFlow<List<String>>(emptyList())
    val availableGovernors: StateFlow<List<String>> = _availableGovernors.asStateFlow()
    
    init {
        loadCurrentParams()
        loadAvailableFreqs()
        loadAvailableGovernors()
    }
    
    /**
     * 加载当前 CPU 参数
     */
    fun loadCurrentParams() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val params = waltOptimizer.getCurrentCpuParams()
                
                // 解析当前参数
                val minFreq = params["min_freq"]?.toIntOrNull() ?: 500000
                val maxFreq = params["max_freq"]?.toIntOrNull() ?: 2000000
                val governor = params["governor"] ?: "schedutil"
                
                // 更新当前参数
                _currentParams.value = _currentParams.value.copy(
                    minFreq = minFreq,
                    maxFreq = maxFreq,
                    scalingGovernor = governor
                )
                
                Logger.log(Logger.Level.INFO, "CpuParamsViewModel", "加载当前CPU参数: $params")
                
            } catch (e: Exception) {
                Logger.log(Logger.Level.ERROR, "CpuParamsViewModel", "加载CPU参数失败: ${e.message}")
                showToast("加载失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 加载可用频率列表
     */
    private fun loadAvailableFreqs() {
        viewModelScope.launch {
            try {
                val freqs = waltOptimizer.getAvailableFreqs()
                _availableFreqs.value = freqs
                Logger.log(Logger.Level.DEBUG, "CpuParamsViewModel", "加载频率列表: ${freqs.size}个")
            } catch (e: Exception) {
                Logger.log(Logger.Level.ERROR, "CpuParamsViewModel", "加载频率列表失败: ${e.message}")
            }
        }
    }
    
    /**
     * 加载可用调度器列表
     */
    private fun loadAvailableGovernors() {
        viewModelScope.launch {
            try {
                val governors = waltOptimizer.getAvailableGovernors()
                _availableGovernors.value = governors
                Logger.log(Logger.Level.DEBUG, "CpuParamsViewModel", "加载调度器列表: $governors")
            } catch (e: Exception) {
                Logger.log(Logger.Level.ERROR, "CpuParamsViewModel", "加载调度器列表失败: ${e.message}")
            }
        }
    }
    
    /**
     * 应用自定义 CPU 参数
     */
    fun applyCustomParams() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val params = _currentParams.value
                
                if (!params.isValid()) {
                    showToast("参数无效，请检查配置")
                    _isLoading.value = false
                    return@launch
                }
                
                // 应用 WALT 参数
                val waltSuccess = waltOptimizer.applyCustomWaltParams(
                    utilThreshold = params.utilThreshold,
                    utilEstFactor = params.utilEstFactor,
                    utilFilter = params.utilFilter,
                    windowSize = params.windowSize,
                    decayRate = params.decayRate,
                    minLoad = params.minLoad
                )
                
                // 应用 CPU 参数
                val cpuSuccess = waltOptimizer.applyCustomCpuParams(
                    minFreq = params.minFreq,
                    maxFreq = params.maxFreq,
                    scalingGovernor = params.scalingGovernor,
                    upThreshold = params.upThreshold,
                    downThreshold = params.downThreshold,
                    samplingRate = params.samplingRate
                )
                
                if (waltSuccess && cpuSuccess) {
                    Logger.log(Logger.Level.SUCCESS, "CpuParamsViewModel", "自定义CPU参数已应用")
                    showToast("✓ 参数应用成功")
                } else {
                    Logger.log(Logger.Level.WARNING, "CpuParamsViewModel", "参数应用部分失败")
                    showToast("⚠ 部分参数应用失败")
                }
                
            } catch (e: Exception) {
                Logger.log(Logger.Level.ERROR, "CpuParamsViewModel", "应用参数失败: ${e.message}")
                showToast("✗ 应用失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 恢复默认参数
     */
    fun restoreDefault() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = waltOptimizer.resetToDefaultParams()
                
                if (success) {
                    _currentParams.value = CpuParams.DEFAULT_MODE
                    Logger.log(Logger.Level.SUCCESS, "CpuParamsViewModel", "已恢复默认参数")
                    showToast("✓ 已恢复默认参数")
                } else {
                    Logger.log(Logger.Level.ERROR, "CpuParamsViewModel", "恢复默认参数失败")
                    showToast("✗ 恢复失败")
                }
                
            } catch (e: Exception) {
                Logger.log(Logger.Level.ERROR, "CpuParamsViewModel", "恢复默认参数异常: ${e.message}")
                showToast("✗ 恢复失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 应用预设模式
     */
    fun applyPreset(preset: CpuParams) {
        viewModelScope.launch {
            _currentParams.value = preset
            Logger.log(Logger.Level.INFO, "CpuParamsViewModel", "应用预设模式")
            showToast("已选择预设，点击应用以生效")
        }
    }
    
    /**
     * 更新参数
     */
    fun updateParams(params: CpuParams) {
        _currentParams.value = params
    }
    
    /**
     * 更新最小频率
     */
    fun updateMinFreq(freq: Int) {
        _currentParams.value = _currentParams.value.copy(
            minFreq = freq,
            maxFreq = maxOf(freq, _currentParams.value.maxFreq)
        )
    }
    
    /**
     * 更新最大频率
     */
    fun updateMaxFreq(freq: Int) {
        _currentParams.value = _currentParams.value.copy(
            minFreq = minOf(freq, _currentParams.value.minFreq),
            maxFreq = freq
        )
    }
    
    /**
     * 更新调度器
     */
    fun updateGovernor(governor: String) {
        _currentParams.value = _currentParams.value.copy(
            scalingGovernor = governor
        )
    }
    
    /**
     * 显示提示消息
     */
    private fun showToast(message: String) {
        _toastMessage.value = message
        // 自动清除
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _toastMessage.value = null
        }
    }
    
    /**
     * 清除提示消息
     */
    fun clearToast() {
        _toastMessage.value = null
    }
}
