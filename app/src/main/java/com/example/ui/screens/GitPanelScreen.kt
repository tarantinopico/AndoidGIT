package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodels.GitFileManagerViewModel
import com.example.ui.viewmodels.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitPanelScreen(
    viewModel: GitFileManagerViewModel,
    onBack: () -> Unit
) {
    val repo by viewModel.selectedGitRepo.collectAsStateWithLifecycle()
    val status by viewModel.gitStatus.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isGitRepo = status.isNotEmpty() && status != "Not a Git repository"
    
    var commitMessage by remember { mutableStateOf("") }
    var cloneUri by remember { mutableStateOf("") }
    var cloneDirName by remember { mutableStateOf("new_repo") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repo?.name ?: "Git Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState is UiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            } else if (uiState is UiState.Success) {
                Text((uiState as UiState.Success).message, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
            } else if (uiState is UiState.Error) {
                Text((uiState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!isGitRepo) {
                Text("Current directory is not a Git repository.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { repo?.let { viewModel.initRepository(it) } }) {
                    Text("Initialize Repository")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("Or Clone from Remote:", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = cloneUri,
                    onValueChange = { cloneUri = it },
                    label = { Text("Repository URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cloneDirName,
                    onValueChange = { cloneDirName = it },
                    label = { Text("Directory Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.cloneRepository(cloneUri, cloneDirName) },
                    enabled = cloneUri.isNotBlank() && cloneDirName.isNotBlank()
                ) {
                    Text("Clone")
                }
                
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("main branch", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(status, style = MaterialTheme.typography.bodySmall, color = com.example.ui.theme.StatusGreen)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text("Commit Message") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.commitChanges(commitMessage) },
                    enabled = commitMessage.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                ) {
                    Text("Commit All")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.pullChanges() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text("Pull")
                    }
                    OutlinedButton(
                        onClick = { viewModel.pushChanges() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text("Push")
                    }
                }
            }
        }
    }
}
