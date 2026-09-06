package com.expensetracker.app.presentation.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.transfer.DataTransferManager
import com.expensetracker.app.domain.model.AppSettings
import com.expensetracker.app.domain.model.ThemeMode
import com.expensetracker.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dataTransferManager: DataTransferManager,
) : ViewModel() {

    val uiState: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings("There", "INR", ThemeMode.DARK))

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() { _message.value = null }

    fun setUserName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { settingsRepository.setUserName(name.trim()) }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { settingsRepository.setCurrency(code) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun exportExpensesCsv(uri: Uri) {
        viewModelScope.launch {
            dataTransferManager.exportExpensesCsv(uri)
                .onSuccess { _message.value = "Expenses exported to CSV" }
                .onFailure { _message.value = "Export failed: ${it.message}" }
        }
    }

    fun exportSubscriptionsCsv(uri: Uri) {
        viewModelScope.launch {
            dataTransferManager.exportSubscriptionsCsv(uri)
                .onSuccess { _message.value = "Subscriptions exported to CSV" }
                .onFailure { _message.value = "Export failed: ${it.message}" }
        }
    }

    fun exportJsonBackup(uri: Uri) {
        viewModelScope.launch {
            dataTransferManager.exportJsonBackup(uri)
                .onSuccess { _message.value = "Backup saved" }
                .onFailure { _message.value = "Backup failed: ${it.message}" }
        }
    }

    fun importJsonBackup(uri: Uri) {
        viewModelScope.launch {
            dataTransferManager.importJsonBackup(uri)
                .onSuccess { _message.value = "Backup restored" }
                .onFailure { _message.value = "Import failed: ${it.message}" }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            dataTransferManager.deleteAllData()
                .onSuccess {
                    // The SMS watermark lives outside the database, so without this
                    // the import screen would keep saying "nothing to import" against
                    // a database that no longer holds any of those entries.
                    settingsRepository.setLastSmsImportAt(0)
                    _message.value = "All data deleted"
                }
                .onFailure { _message.value = "Delete failed: ${it.message}" }
        }
    }
}
