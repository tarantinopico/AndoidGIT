package com.example.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GitApplication
import com.example.auth.GitHubAuthApi
import com.example.auth.SessionManager
import com.example.files.FileItem
import com.example.files.FileManager
import com.example.git.GitManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

class GitFileManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as GitApplication).container
    val sessionManager = container.sessionManager
    private val fileManager = container.fileManager
    private val gitManager = container.gitManager
    private val api = container.gitHubAuthApi

    val patToken = sessionManager.patToken

    private val _currentDirectory = MutableStateFlow(Environment.getExternalStorageDirectory())
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedGitRepo = MutableStateFlow<File?>(null)
    val selectedGitRepo: StateFlow<File?> = _selectedGitRepo.asStateFlow()

    private val _gitStatus = MutableStateFlow<String>("")
    val gitStatus: StateFlow<String> = _gitStatus.asStateFlow()

    private val OAUTH_CLIENT_ID = "YOUR_CLIENT_ID_HERE"
    private val OAUTH_CLIENT_SECRET = "YOUR_CLIENT_SECRET_HERE"

    init {
        loadFiles(_currentDirectory.value)
    }

    fun saveToken(token: String) {
        sessionManager.saveToken(token)
    }

    fun loadFiles(directory: File) {
        _currentDirectory.value = directory
        viewModelScope.launch(Dispatchers.IO) {
            val fileList = fileManager.getFiles(directory)
            _files.value = fileList
        }
    }

    fun navigateUp() {
        val parent = _currentDirectory.value.parentFile
        if (parent != null) {
            loadFiles(parent)
        }
    }
    
    fun clearUiState() {
        _uiState.value = UiState.Idle
    }

    fun handleOAuthRedirect(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == "gitfilemanager" && uri.host == "oauth2callback") {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                exchangeCodeForToken(code)
            } else {
                _uiState.value = UiState.Error("Failed to retrieve auth code")
            }
        }
    }

    private fun exchangeCodeForToken(code: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = api.getAccessToken(
                    clientId = OAUTH_CLIENT_ID,
                    clientSecret = OAUTH_CLIENT_SECRET,
                    code = code,
                    redirectUri = "gitfilemanager://oauth2callback"
                )
                if (response.accessToken != null) {
                    sessionManager.saveToken(response.accessToken)
                    _uiState.value = UiState.Success("OAuth login successful!")
                } else {
                    _uiState.value = UiState.Error(response.errorDescription ?: "Unknown error")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun selectGitRepo(directory: File) {
        _selectedGitRepo.value = directory
        refreshGitStatus(directory)
    }

    fun refreshGitStatus(directory: File?) {
        val dir = directory ?: return
        viewModelScope.launch {
            _gitStatus.value = gitManager.getStatus(dir)
        }
    }

    fun initRepository(directory: File) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                gitManager.initRepo(directory)
                _uiState.value = UiState.Success("Repository initialized.")
                loadFiles(_currentDirectory.value)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Init failed: ${e.message}")
            }
        }
    }

    fun cloneRepository(uri: String, directoryName: String) {
        val targetDir = File(_currentDirectory.value, directoryName)
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                gitManager.clone(uri, targetDir, patToken.value)
                _uiState.value = UiState.Success("Cloned successfully.")
                loadFiles(_currentDirectory.value)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Clone failed: ${e.message}")
            }
        }
    }

    fun commitChanges(message: String) {
        val repo = _selectedGitRepo.value ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                gitManager.commit(repo, message)
                _uiState.value = UiState.Success("Changes committed.")
                refreshGitStatus(repo)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Commit failed: ${e.message}")
            }
        }
    }

    fun pullChanges() {
        val repo = _selectedGitRepo.value ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                gitManager.pull(repo, patToken.value)
                _uiState.value = UiState.Success("Pull completed.")
                refreshGitStatus(repo)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Pull failed: ${e.message}")
            }
        }
    }

    fun pushChanges() {
        val repo = _selectedGitRepo.value ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                gitManager.push(repo, patToken.value)
                _uiState.value = UiState.Success("Push completed.")
                refreshGitStatus(repo)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Push failed: ${e.message}")
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = fileManager.createFile(_currentDirectory.value, name)
            if (success) {
                _uiState.value = UiState.Success("File created.")
                loadFiles(_currentDirectory.value)
            } else {
                _uiState.value = UiState.Error("Failed to create file.")
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = fileManager.createFolder(_currentDirectory.value, name)
            if (success) {
                _uiState.value = UiState.Success("Folder created.")
                loadFiles(_currentDirectory.value)
            } else {
                _uiState.value = UiState.Error("Failed to create folder.")
            }
        }
    }

    fun addRemote(uri: String) {
        val repo = _selectedGitRepo.value ?: _currentDirectory.value
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                gitManager.addRemote(repo, "origin", uri)
                _uiState.value = UiState.Success("Remote added.")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to add remote: ${e.message}")
            }
        }
    }
}
