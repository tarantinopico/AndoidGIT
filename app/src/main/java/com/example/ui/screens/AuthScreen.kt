package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodels.GitFileManagerViewModel
import com.example.ui.viewmodels.UiState

@Composable
fun AuthScreen(
    viewModel: GitFileManagerViewModel,
    onAuthSuccess: () -> Unit
) {
    val patToken by viewModel.patToken.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tokenInput by remember(patToken) { mutableStateOf(patToken.ifEmpty { "" }) }

    LaunchedEffect(patToken) {
        if (patToken.isNotEmpty()) {
            onAuthSuccess()
        }
    }

    Scaffold(
        snackbarHost = { /* Handle snackbars if needed */ }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Auth",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "GitHub Authentication",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("Personal Access Token (PAT)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.saveToken(tokenInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = tokenInput.isNotBlank()
            ) {
                Text("Save Token & Continue")
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("OR", style = MaterialTheme.typography.labelLarge)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = { launchGitHubOAuth(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login with Browser (OAuth)")
            }

            if (uiState is UiState.Loading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator()
            }
            if (uiState is UiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun launchGitHubOAuth(context: Context) {
    val clientId = "YOUR_CLIENT_ID_HERE" // Ideally loaded securely
    val redirectUri = "gitfilemanager://oauth2callback"
    val url = "https://github.com/login/oauth/authorize?client_id=$clientId&redirect_uri=$redirectUri&scope=repo"
    
    val intent = CustomTabsIntent.Builder().build()
    intent.launchUrl(context, Uri.parse(url))
}
