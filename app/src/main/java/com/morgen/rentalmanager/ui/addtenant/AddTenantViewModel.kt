package com.morgen.rentalmanager.ui.addtenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.morgen.rentalmanager.myapplication.Tenant
import com.morgen.rentalmanager.myapplication.TenantRepository
import kotlinx.coroutines.launch

class AddTenantViewModel(private val repository: TenantRepository) : ViewModel() {

    fun addTenant(tenant: Tenant) {
        viewModelScope.launch {
            repository.insert(tenant)
        }
    }
}

class AddTenantViewModelFactory(private val repository: TenantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTenantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddTenantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 