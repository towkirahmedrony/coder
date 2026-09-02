package com.coder.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coder.app.AppContainer
import com.coder.app.features.chat.ui.ChatScreen
import com.coder.app.features.editor.ui.EditorScreen
import com.coder.app.features.editor.ui.viewmodel.EditorViewModel
import com.coder.app.features.settings.ui.SettingsScreen
import com.coder.app.features.chat.ui.viewmodel.ChatViewModel
import com.coder.app.features.settings.ui.viewmodel.SettingsViewModel

object Routes {
    const val CHAT = "chat"
    const val EDITOR = "editor"
    const val TERMINAL = "terminal"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(appContainer: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CHAT) {

        composable(Routes.CHAT) {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.provideFactory(
                    chatRepository = appContainer.chatRepository,
                    settingsRepository = appContainer.settingsRepository
                )
            )

            ChatScreen(
                viewModel = chatViewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(
                    repository = appContainer.settingsRepository
                )
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 🚀 NEW: File Explorer (Workspace Viewer)
        composable(Routes.EDITOR) {
            val editorViewModel: EditorViewModel = viewModel()
            
            EditorScreen(
                viewModel = editorViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.TERMINAL) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Terminal Workspace (Coming Soon)", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
