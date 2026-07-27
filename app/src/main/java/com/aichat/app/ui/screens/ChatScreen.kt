package com.aichat.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.aichat.app.ui.components.ChatInput
import com.aichat.app.ui.components.MessageBubble
import com.aichat.app.ui.components.TypingIndicator
import com.aichat.app.viewmodel.ChatViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch

fun readTextFromFile(context: Context, uri: Uri): String {
    return try { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: "" } catch (e: Exception) { "Error reading file content." }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onNavigateToSettings: () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val conversations by viewModel.conversations.collectAsState()
    val currentConvId by viewModel.currentConversationId.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val streamingMsg by viewModel.streamingMessage.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // 🚀 NEW: ViewModel থেকে useCloudAi স্টেট রিড করছি
    val useCloudAi by viewModel.useCloudAi.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isOcrProcessing by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { selectedUri = it } }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let { selectedUri = it } }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentTitle = conversations.find { it.id == currentConvId }?.title ?: "AIChat"

    LaunchedEffect(messages.size, streamingMsg) { if (messages.isNotEmpty() || streamingMsg != null) listState.animateScrollToItem(messages.size + (if(streamingMsg != null) 1 else 0)) }
    LaunchedEffect(error) { error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    conversations = conversations, currentId = currentConvId,
                    onSelect = { viewModel.selectConversation(it); scope.launch { drawerState.close() } },
                    onNewChat = { viewModel.createNewChat(); scope.launch { drawerState.close() } },
                    onDelete = { viewModel.deleteConversation(it) },
                    onRename = { conv, title -> viewModel.renameConversation(conv, title) },
                    onSettingsClick = { scope.launch { drawerState.close() }; onNavigateToSettings() }
                )
            }
        }
    ) {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text(currentTitle) }, navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Menu") } }) },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            ) {
                if (currentConvId == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) { Text("No chat selected.") }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                        items(messages) { msg ->
                            MessageBubble(
                                role = msg.role, content = msg.content,
                                onEdit = { inputText = msg.content },
                                onRegenerate = {
                                    val lastUserMsg = messages.lastOrNull { it.role == "user" }?.content
                                    if(lastUserMsg != null) viewModel.sendMessage(lastUserMsg)
                                }
                            )
                        }
                        if (streamingMsg != null) item { MessageBubble(role = "assistant", content = streamingMsg!!) }
                        else if (isLoading || isOcrProcessing) item { TypingIndicator() }
                    }
                    
                    // 🚀 NEW: AI Model Switcher UI (ChatInput এর ঠিক উপরে)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (useCloudAi) "☁️ Cloud AI (Groq)" else "💻 Local AI (Qwen)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = useCloudAi,
                            onCheckedChange = { viewModel.toggleAiMode(it) },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    ChatInput(
                        text = inputText, onTextChange = { inputText = it },
                        isLoading = isLoading || isOcrProcessing, selectedUri = selectedUri,
                        onImageAttachClick = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onFileAttachClick = { fileLauncher.launch(arrayOf("*/*")) },
                        onRemoveAttachment = { selectedUri = null },
                        onStopGenerating = { viewModel.stopGenerating() },
                        onSendMessage = {
                            if (inputText.isNotBlank() || selectedUri != null) {
                                val textToSend = inputText
                                inputText = ""
                                focusManager.clearFocus()
                                if (selectedUri != null) {
                                    val mimeType = context.contentResolver.getType(selectedUri!!)
                                    if (mimeType?.startsWith("image/") == true) {
                                        isOcrProcessing = true
                                        try {
                                            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromFilePath(context, selectedUri!!))
                                                .addOnSuccessListener { visionText ->
                                                    isOcrProcessing = false; selectedUri = null
                                                    viewModel.sendMessage("Extracted image text:\n```\n${visionText.text}\n```\n$textToSend")
                                                }.addOnFailureListener { isOcrProcessing = false; selectedUri = null; viewModel.sendMessage(textToSend) }
                                        } catch (e: Exception) { isOcrProcessing = false; selectedUri = null; viewModel.sendMessage(textToSend) }
                                    } else {
                                        val fileContent = readTextFromFile(context, selectedUri!!)
                                        selectedUri = null; viewModel.sendMessage("File content:\n```\n$fileContent\n```\n$textToSend")
                                    }
                                } else { viewModel.sendMessage(textToSend) }
                            }
                        }
                    )
                }
            }
        }
    }
}
