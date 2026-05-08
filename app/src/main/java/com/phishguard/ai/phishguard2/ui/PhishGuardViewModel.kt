package com.phishguard.ai.phishguard2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.phishguard.ai.phishguard2.data.PhishGuardRepository
import com.phishguard.ai.phishguard2.data.ScanResultEntity
import com.phishguard.ai.phishguard2.network.PredictionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the PhishGuard AI app.
 * It manages UI state and survives configuration changes (like screen rotation).
 * It uses the Repository to fetch and save data.
 */
class PhishGuardViewModel(private val repository: PhishGuardRepository) : ViewModel() {

    // UI state for the current scan
    private val _scanResult = MutableStateFlow<PredictionResponse?>(null)
    val scanResult: StateFlow<PredictionResponse?> = _scanResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Observes the local scan history from the repository
    val scanHistory: Flow<List<ScanResultEntity>> = repository.allScans

    /**
     * Triggers a new scan.
     * Uses Coroutines (viewModelScope) to perform network and DB operations off the main thread.
     */
    fun performScan(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _scanResult.value = null

            val result = repository.scanAndSave(url)
            
            _isLoading.value = false
            result.onSuccess {
                _scanResult.value = it
            }.onFailure {
                _errorMessage.value = "Scan failed: ${it.message}"
            }
        }
    }

    /**
     * Clears the entire local scan history.
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    /**
     * Resets the current scan result and error message.
     */
    fun resetScanState() {
        _scanResult.value = null
        _errorMessage.value = null
    }

    /**
     * Factory class to instantiate the ViewModel with the Repository dependency.
     */
    class Factory(private val repository: PhishGuardRepository) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PhishGuardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PhishGuardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
