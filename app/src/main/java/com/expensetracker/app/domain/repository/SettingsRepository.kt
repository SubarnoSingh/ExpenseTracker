package com.expensetracker.app.domain.repository

import com.expensetracker.app.domain.model.AppSettings
import com.expensetracker.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setUserName(name: String)
    suspend fun setCurrency(code: String)
    suspend fun setThemeMode(mode: ThemeMode)
}
