package com.coder.app.features.editor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.app.features.editor.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBackClick: () -> Unit
) {
    val repos by viewModel.repositories.collectAsState()
    val selectedRepo by viewModel.selectedRepo.collectAsState()
    val visibleNodes by viewModel.visibleNodes.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()
    val currentFilePath by viewModel.currentFilePath.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val scope = rememberCoroutineScope()

    // 🚀 স্মার্ট ব্যাক হ্যান্ডলার: সাইডবার ওপেন থাকলে আগে সাইডবার ক্লোজ হবে
    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (fileContent != null) {
            viewModel.closeFile()
            scope.launch { drawerState.open() } // ফাইল ক্লোজ হলে আবার ট্রি ওপেন হবে
        } else if (selectedRepo != null) {
            viewModel.navigateBack() // রিপো থেকে বের হয়ে রিপো লিস্টে যাবে
        } else {
            onBackClick() // একদম বের হয়ে যাবে
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp), // সাইডবারের স্ট্যান্ডার্ড সাইজ
                drawerContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                // ==========================================
                // 📂 SIDEBAR VIEW: REPOSITORIES OR FILE TREE
                // ==========================================
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sidebar Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedRepo != null) {
                            IconButton(
                                onClick = { viewModel.navigateBack() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = selectedRepo ?: "EXPLORER",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    if (selectedRepo == null) {
                        // 1. Repo List
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(repos) { repo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectRepository(repo) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(repo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    } else {
                        // 2. VS Code Style File Tree
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(visibleNodes, key = { it.file.path }) { node ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (node.file.isDirectory) {
                                                viewModel.toggleDirectory(node.file.path)
                                            } else {
                                                viewModel.openFile(node.file.path)
                                                scope.launch { drawerState.close() } // 🚀 ফাইল ওপেন হলে সাইডবার অটো বন্ধ হবে
                                            }
                                        }
                                        .padding(
                                            start = (node.level * 16 + 12).dp, // ডায়নামিক ইন্ডেন্টেশন
                                            top = 8.dp, 
                                            bottom = 8.dp, 
                                            end = 12.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (node.file.isDirectory) {
                                        Icon(
                                            imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                    
                                    Spacer(modifier = Modifier.width(6.dp))
                                    
                                    Icon(
                                        imageVector = if (node.file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (node.file.isDirectory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = node.file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (currentFilePath == node.file.path) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        // ==========================================
        // 💻 MAIN VIEW: CODE EDITOR (Full Screen)
        // ==========================================
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description, 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentFilePath?.substringAfterLast("/") ?: "Welcome",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar")
                        }
                    },
                    actions = {
                        // 🚀 Placeholder for future actions (like Save or Run)
                        if (fileContent != null) {
                            IconButton(onClick = { /* TODO: Run/Save Action */ }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color(0xFF4CAF50))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                if (fileContent != null) {
                    // Code Viewer
                    SelectionContainer {
                        Text(
                            text = fileContent!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        )
                    }
                } else {
                    // Empty State
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Select a file from the sidebar to view code",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { scope.launch { drawerState.open() } }) {
                            Text("Open Explorer")
                        }
                    }
                }
            }
        }
    }
}
