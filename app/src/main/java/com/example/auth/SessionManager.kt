package com.example.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_git_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
