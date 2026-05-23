package com.example.di

import android.content.Context
import com.example.auth.SessionManager
import com.example.files.FileManager
import com.example.git.GitManager

class AppContainer(private val context: Context) {
    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val fileManager: FileManager by lazy {
        FileManager()
    }

    val gitManager: GitManager by lazy {
        GitManager()
    }
}
