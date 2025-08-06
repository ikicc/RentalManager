package com.morgen.rentalmanager.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.morgen.rentalmanager.myapplication.TenantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingViewModel(private val repository: TenantRepository): ViewModel() {
    private val _water = MutableStateFlow(0.0) // 初始值设为0，等待从数据库加载
    private val _electricity = MutableStateFlow(0.0) // 初始值设为0，等待从数据库加载
    val water: StateFlow<Double> = _water
    val electricity: StateFlow<Double> = _electricity

    // Privacy protection state
    private val _privacyKeywords = MutableStateFlow<List<String>>(emptyList())
    val privacyKeywords: StateFlow<List<String>> = _privacyKeywords

    init {
        // 直接监听价格Flow，确保获取最新价格
        viewModelScope.launch {
            repository.priceFlow.collect { price ->
                android.util.Log.d("SettingViewModel", "从数据库加载价格: 水价=${price.water}, 电价=${price.electricity}")
                _water.value = price.water
                _electricity.value = price.electricity
            }
        }
        
        // 使用Flow实时监听隐私关键字变化
        viewModelScope.launch {
            repository.privacyKeywordsFlow.collect { keywords ->
                _privacyKeywords.value = keywords
                android.util.Log.d("SettingViewModel", "隐私关键字Flow更新: $keywords")
            }
        }
    }

    fun onWaterChange(txt:String){ _water.value = txt.toDoubleOrNull() ?: _water.value }
    fun onElecChange(txt:String){ _electricity.value = txt.toDoubleOrNull() ?: _electricity.value }

    fun save(onSaved:()->Unit){
        viewModelScope.launch {
            android.util.Log.d("SettingViewModel", "开始保存设置，水价: ${_water.value}, 电价: ${_electricity.value}, 隐私关键字: ${_privacyKeywords.value}")
            
            try {
                // 保存价格数据（这会自动重新计算所有账单）
                repository.savePrice(_water.value, _electricity.value)
                android.util.Log.d("SettingViewModel", "价格数据保存完成")
                
                // 保存隐私关键字
                repository.savePrivacyKeywords(_privacyKeywords.value)
                android.util.Log.d("SettingViewModel", "隐私关键字保存完成")
                
                // 额外延迟确保所有数据库操作完成
                kotlinx.coroutines.delay(200)
                
                android.util.Log.d("SettingViewModel", "设置保存完成，所有相关数据已更新")
                
                onSaved()
                
            } catch (e: Exception) {
                android.util.Log.e("SettingViewModel", "保存设置失败", e)
                onSaved() // 即使失败也要回调，让用户知道操作完成
            }
        }
    }

    // Privacy protection methods
    fun addPrivacyKeyword(keyword: String) {
        if (keyword.isNotBlank() && keyword.length <= 20 && !_privacyKeywords.value.contains(keyword)) {
            val currentKeywords = _privacyKeywords.value.toMutableList()
            if (currentKeywords.size < 10) { // Maximum 10 keywords as per design
                currentKeywords.add(keyword)
                _privacyKeywords.value = currentKeywords
            }
        }
    }

    fun removePrivacyKeyword(keyword: String) {
        val currentKeywords = _privacyKeywords.value.toMutableList()
        currentKeywords.remove(keyword)
        _privacyKeywords.value = currentKeywords
    }

    fun previewRoomNumberWithPrivacy(roomNumber: String): String {
        return repository.applyPrivacyProtection(roomNumber, _privacyKeywords.value)
    }

    fun import(context: android.content.Context, uri: android.net.Uri, onDone:(Boolean)->Unit){
        viewModelScope.launch {
            runCatching {
                com.morgen.rentalmanager.utils.ImportUtils.importFromJson(context, uri, repository)
            }.onSuccess { onDone(true) }
             .onFailure { onDone(false) }
        }
    }
}

class SettingVMFactory(private val repo: TenantRepository): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(SettingViewModel::class.java))
            return SettingViewModel(repo) as T
        throw IllegalArgumentException()
    }
} 