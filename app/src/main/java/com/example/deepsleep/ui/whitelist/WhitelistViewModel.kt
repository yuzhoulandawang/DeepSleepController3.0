package com.example.deepsleep.ui.whitelist
import java.util.Locale
import android.graphics.drawable.Drawable
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepsleep.data.WhitelistRepository
import com.example.deepsleep.model.WhitelistItem
import com.example.deepsleep.model.WhitelistType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WhitelistUiState(
    val currentType: WhitelistType = WhitelistType.SUPPRESS,
    val items: List<WhitelistItem> = emptyList()
)

class WhitelistViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = WhitelistRepository()
    
    private val _uiState = MutableStateFlow(WhitelistUiState())
    val uiState: StateFlow<WhitelistUiState> = _uiState.asStateFlow()
    
    
    private val _apps = MutableStateFlow<List<WhitelistItem>>(emptyList())
    val apps: StateFlow<List<WhitelistItem>> = _apps.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadItems()
    }
    
    fun switchType(type: WhitelistType) {
        _uiState.value = _uiState.value.copy(currentType = type)
        loadItems()
    }
    
    private fun loadItems() {
        viewModelScope.launch {
            val items = repository.loadItems(
                getApplication(),
                _uiState.value.currentType
            )
            _uiState.value = _uiState.value.copy(items = items)
        }
    }
    
    suspend fun addItem(name: String, note: String, type: WhitelistType) {
        repository.addItem(getApplication(), name, note, type)
        loadItems()
    }
    
    suspend fun updateItem(item: WhitelistItem) {
        repository.updateItem(getApplication(), item)
        loadItems()
    }
    
    suspend fun deleteItem(item: WhitelistItem) {
        repository.deleteItem(getApplication(), item)
        loadItems()
    }

    
    fun loadInstalledApps(showSystemApps: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val packageManager = getApplication<Application>().packageManager
                val appsList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { appInfo ->
                        // 过滤系统应用
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                        showSystemApps || !isSystem
                    }
                    .map { appInfo ->
                        WhitelistItem(
                            packageName = appInfo.packageName,
                            name = appInfo.loadLabel(packageManager).toString(),
                            icon = appInfo.loadIcon(packageManager),
                            isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                        )
                    }
                    .sortedBy { it.name.lowercase(Locale.getDefault()) }
                
                _apps.value = appsList
                Logger.log(Logger.Level.INFO, "WhitelistViewModel", "加载应用列表: ${appsList.size} 个应用")
                
            } catch (e: Exception) {
                Logger.log(Logger.Level.ERROR, "WhitelistViewModel", "加载应用列表失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addToWhitelist(items: List<WhitelistItem>) {
        viewModelScope.launch {
            try {
                val currentWhitelist = whitelistRepository.getWhitelist().first()
                val packagesToAdd = items.map { it.packageName }
                val updatedWhitelist = currentWhitelist.toMutableSet().apply { addAll(packagesToAdd) }
                
                whitelistRepository.saveWhitelist(updatedWhitelist.toList())
                Logger.log(Logger.Level.SUCCESS, "WhitelistViewModel", "添加 ${items.size} 个应用到白名单")
                
                // 重新加载当前白名单状态
                loadWhitelist()
            } catch (e: Exception) {
                Logger.log(Logger.Level.ERROR, "WhitelistViewModel", "添加到白名单失败: ${e.message}")
            }
        }
    }
    
    fun addToWhitelist(item: WhitelistItem) {
        addToWhitelist(listOf(item))
    }
    
    fun removeFromWhitelist(items: List<WhitelistItem>) {
        viewModelScope.launch {
            try {
                val currentWhitelist = whitelistRepository.getWhitelist().first()
                val packagesToRemove = items.map { it.packageName }.toSet()
                val updatedWhitelist = currentWhitelist.filter { it !in packagesToRemove }
                
                whitelistRepository.saveWhitelist(updatedWhitelist)
                Logger.log(Logger.Level.INFO, "WhitelistViewModel", "从白名单移除 ${items.size} 个应用")
                
                // 重新加载当前白名单状态
                loadWhitelist()
            } catch (e: Exception) {
                Logger.log(Logger.Level.ERROR, "WhitelistViewModel", "从白名单移除失败: ${e.message}")
            }
        }
    }
    
    fun removeFromWhitelist(item: WhitelistItem) {
        removeFromWhitelist(listOf(item))
    }
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun getFilteredApps(): List<WhitelistItem> {
        val query = _searchQuery.value.lowercase(Locale.getDefault())
        return if (query.isBlank()) {
            _apps.value
        } else {
            _apps.value.filter { it.name.lowercase(Locale.getDefault()).contains(query) }
        }
    }
    
    fun isAppInWhitelist(packageName: String): Boolean {
        return _whitelist.value.contains(packageName)
    }
}
