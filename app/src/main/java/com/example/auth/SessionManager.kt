package com.example.auth

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("git_prefs", Context.MODE_PRIVATE)
    private val _patToken = MutableStateFlow(prefs.getString("github_pat", "") ?: "")
    val patToken: StateFlow<String> = _patToken.asStateFlow()

    fun saveToken(token: String) {
        prefs.edit { putString("github_pat", token) }
        _patToken.value = token
    }

    fun hasToken(): Boolean {
        return patToken.value.isNotEmpty()
    }
}
