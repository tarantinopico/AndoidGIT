package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.GitPanelScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.GitFileManagerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GitFileManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.handleOAuthRedirect(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleOAuthRedirect(intent)
    }
}

@Composable
fun AppNavigation(viewModel: GitFileManagerViewModel) {
    val navController = rememberNavController()
    
    val startDestination = if (viewModel.sessionManager.hasToken()) "fileManager" else "auth"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate("fileManager") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("fileManager") {
            FileManagerScreen(
                viewModel = viewModel,
                onNavigateToGitPanel = { navController.navigate("gitPanel") }
            )
        }
        composable("gitPanel") {
            GitPanelScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
