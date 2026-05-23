package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.background
import com.example.files.FileItem
import com.example.ui.viewmodels.GitFileManagerViewModel
import com.example.ui.viewmodels.UiState
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: GitFileManagerViewModel,
    onNavigateToGitPanel: () -> Unit
) {
    val context = LocalContext.current
    val currentDir by viewModel.currentDirectory.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showBottomSheet by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar((uiState as UiState.Success).message)
                viewModel.clearUiState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar((uiState as UiState.Error).message)
                viewModel.clearUiState()
            }
            else -> {}
        }
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                false // Handled by Accompanist for older versions
            }
        )
    }

    val storagePermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        emptyList()
    }

    val permissionState = rememberMultiplePermissionsState(permissions = storagePermissions)
    
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        hasStoragePermission = permissionState.allPermissionsGranted
    }

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                if (createType.isEmpty()) {
                    Text("Create New", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    ListItem(
                        headlineContent = { Text("Folder") },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = FolderBg) },
                        modifier = Modifier.clickable { createType = "Folder" }
                    )
                    ListItem(
                        headlineContent = { Text("File") },
                        leadingContent = { Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = BuildBg) },
                        modifier = Modifier.clickable { createType = "File" }
                    )
                    ListItem(
                        headlineContent = { Text("Initialize Git Repository") },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = StatusGreen) },
                        modifier = Modifier.clickable { createType = "Git" }
                    )
                } else {
                    Text("New $createType", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(if (createType == "Git") "Remote URL (Optional)" else "Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            when (createType) {
                                "Folder" -> viewModel.createFolder(nameInput)
                                "File" -> viewModel.createFile(nameInput)
                                "Git" -> {
                                    viewModel.initRepository(currentDir)
                                    if (nameInput.isNotBlank()) viewModel.addRemote(nameInput)
                                }
                            }
                            showBottomSheet = false
                            createType = ""
                            nameInput = ""
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(currentDir.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (currentDir.parentFile != null) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (hasStoragePermission) {
                FloatingActionButton(onClick = { 
                    createType = ""
                    nameInput = ""
                    showBottomSheet = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        if (!hasStoragePermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Storage permission is required to manage files.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        }
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                }) {
                    Text("Grant Permission")
                }
            }
        } else {
            LaunchedEffect(currentDir, hasStoragePermission) {
                if (hasStoragePermission) {
                    viewModel.loadFiles(currentDir)
                }
            }
            
            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("This folder is empty.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(files) { file ->
                        FileRow(file = file, onClick = {
                            if (file.isDirectory) {
                                viewModel.loadFiles(java.io.File(file.path))
                            }
                        }, onGitSelected = {
                            if (file.isDirectory) {
                                viewModel.selectGitRepo(java.io.File(file.path))
                                onNavigateToGitPanel()
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun FileRow(file: FileItem, onClick: () -> Unit, onGitSelected: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val formattedDate = remember(file.lastModified) { dateFormat.format(Date(file.lastModified)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconBgColor = if (file.isDirectory) FolderBg else BuildBg
        val iconColor = if (file.isDirectory) FolderText else BuildText
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconBgColor,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp, top = 8.dp)
                .drawBehind {
                     val strokeWidth = 1.dp.toPx()
                     val y = size.height - strokeWidth / 2
                     drawLine(
                         color = androidx.compose.ui.graphics.Color(0xFFCAC4D0),
                         start = androidx.compose.ui.geometry.Offset(0f, y),
                         end = androidx.compose.ui.geometry.Offset(size.width, y),
                         strokeWidth = strokeWidth
                     )
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    val detailText = if (file.isDirectory) "Folder • $formattedDate" else "${file.size / 1024} KB • $formattedDate"
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    if (file.isGitRepo) {
                        Text("Git Repository", style = MaterialTheme.typography.bodySmall, color = StatusGreen)
                    }
                }
                
                if (file.isDirectory) {
                    TextButton(onClick = onGitSelected) {
                        Text("GIT", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
