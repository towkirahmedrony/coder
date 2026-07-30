package com.coder.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coder.app.AppContainer
import com.coder.app.ui.screens.ChatScreen
import com.coder.app.ui.screens.SettingsScreen
import com.coder.app.viewmodel.ChatViewModel
import com.coder.app.viewmodel.SettingsViewModel

object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(appContainer: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CHAT) {

        composable(Routes.CHAT) {
            // ViewModel is now properly scoped to the Chat back-stack entry
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.provideFactory(
                    chatRepository = appContainer.chatRepository,
                    settingsRepository = appContainer.settingsRepository
                )
            )

            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            // ViewModel is now properly scoped to the Settings back-stack entry
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(
                    // Fixed: Parameter name changed from settingsRepository to repository
                    repository = appContainer.settingsRepository
                )
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
