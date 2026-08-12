package com.coder.app.features.chat.ui.state

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class ChatScreenState(
    val listState: LazyListState,
    val drawerState: DrawerState,
    val snackbarHostState: SnackbarHostState,
    val coroutineScope: CoroutineScope,
    val context: Context
) {
    var inputText by mutableStateOf("")
        private set
    var selectedUri by mutableStateOf<Uri?>(null)
        private set
    var isOcrProcessing by mutableStateOf(false)
        private set

    fun updateInputText(text: String) { inputText = text }
    fun updateSelectedUri(uri: Uri?) { selectedUri = uri }
    fun updateOcrProcessing(isProcessing: Boolean) { isOcrProcessing = isProcessing }

    fun openDrawer() { coroutineScope.launch { drawerState.open() } }
    fun closeDrawer() { coroutineScope.launch { drawerState.close() } }
    
    fun showSnackbar(message: String) { 
        coroutineScope.launch { snackbarHostState.showSnackbar(message) } 
    }

    suspend fun scrollToBottom(itemCount: Int) {
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }
}

@Composable
fun rememberChatScreenState(
    listState: LazyListState = rememberLazyListState(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current
): ChatScreenState {
    return remember(listState, drawerState, snackbarHostState, coroutineScope, context) {
        ChatScreenState(listState, drawerState, snackbarHostState, coroutineScope, context)
    }
}
