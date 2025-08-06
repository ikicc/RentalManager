package com.morgen.rentalmanager.ui.edittenant

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morgen.rentalmanager.myapplication.Tenant
import com.morgen.rentalmanager.myapplication.TenantRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditTenantViewModel(
    private val repository: TenantRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomNumber: String = checkNotNull(savedStateHandle["roomNumber"])

    val tenant: StateFlow<Tenant> = repository.getTenantById(roomNumber)
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Tenant("", "", 0.0) // Initial empty value
        )

    fun updateTenant(tenant: Tenant) {
        viewModelScope.launch {
            repository.update(tenant)
        }
    }
    
    fun recalculateAllBills(roomNumber: String) {
        viewModelScope.launch {
            repository.recalculateAllBillsForTenant(roomNumber)
        }
    }

    fun deleteTenant() {
        viewModelScope.launch {
            repository.delete(tenant.value)
        }
    }
}

class EditTenantViewModelFactory(
    private val repository: TenantRepository
) : AbstractSavedStateViewModelFactory() {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(EditTenantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditTenantViewModel(repository, handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 