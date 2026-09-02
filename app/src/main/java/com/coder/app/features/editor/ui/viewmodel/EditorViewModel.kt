package com.coder.app.features.editor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coder.app.features.workspace.data.FileNode
import com.coder.app.features.workspace.data.WorkspaceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// UI-তে দেখানোর জন্য নতুন ডেটা ক্লাস (ইন্ডেন্টেশন লেভেল সহ)
data class UiNode(
    val file: FileNode,
    val level: Int,
    val isExpanded: Boolean
)

class EditorViewModel : ViewModel() {

    private val _repositories = MutableStateFlow<List<String>>(emptyList())
    val repositories = _repositories.asStateFlow()

    private val _selectedRepo = MutableStateFlow<String?>(null)
    val selectedRepo = _selectedRepo.asStateFlow()

    // 🚀 NEW: কোন কোন ফোল্ডার ওপেন করা আছে তার ট্র্যাক রাখা
    private val _expandedDirs = MutableStateFlow<Set<String>>(emptySet())
    // 🚀 NEW: কোন ফোল্ডারের ভেতর কী কী ফাইল আছে তার ক্যাশ (Cache)
    private val _dirContents = MutableStateFlow<Map<String, List<FileNode>>>(emptyMap())

    private val _fileContent = MutableStateFlow<String?>(null)
    val fileContent = _fileContent.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath = _currentFilePath.asStateFlow()

    // 🚀 NEW: ম্যাজিক লজিক! এটি ফোল্ডার স্ট্রাকচারকে একটি ফ্ল্যাট লিস্টে রূপান্তর করে UI-কে দিবে
    val visibleNodes = combine(_selectedRepo, _expandedDirs, _dirContents) { repo, expanded, contents ->
        if (repo == null) return@combine emptyList()
        val result = mutableListOf<UiNode>()

        fun buildTree(currentPath: String, level: Int) {
            val children = contents[currentPath] ?: return
            for (child in children) {
                val isExpanded = expanded.contains(child.path)
                result.add(UiNode(child, level, isExpanded))
                
                // ফোল্ডারটি যদি এক্সপ্যান্ডেড থাকে, তাহলে তার ভেতরের ফাইলগুলোও লিস্টে যুক্ত হবে
                if (child.isDirectory && isExpanded) {
                    buildTree(child.path, level + 1)
                }
            }
        }

        buildTree("", 0) // Root থেকে ট্রি বানানো শুরু
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadRepositories()
    }

    fun loadRepositories() {
        _repositories.value = WorkspaceManager.listRepositories()
    }

    fun selectRepository(repoName: String) {
        _selectedRepo.value = repoName
        _expandedDirs.value = emptySet()
        _dirContents.value = emptyMap()
        _fileContent.value = null
        _currentFilePath.value = null
        loadDir("") // রুট ডিরেক্টরি লোড করা
    }

    private fun loadDir(path: String) {
        val repo = _selectedRepo.value ?: return
        val files = WorkspaceManager.listFiles(repo, path)
        _dirContents.update { it + (path to files) }
    }

    // 🚀 NEW: ফোল্ডারে ক্লিক করলে এক্সপ্যান্ড বা কোলাপ্স করার ফাংশন
    fun toggleDirectory(path: String) {
        _expandedDirs.update { current ->
            if (current.contains(path)) {
                current - path // কোলাপ্স
            } else {
                if (!_dirContents.value.containsKey(path)) {
                    loadDir(path) // আগে লোড না হলে লোড করে নিবে
                }
                current + path // এক্সপ্যান্ড
            }
        }
    }

    fun openFile(path: String) {
        val repo = _selectedRepo.value ?: return
        viewModelScope.launch {
            _currentFilePath.value = path
            _fileContent.value = WorkspaceManager.readFile(repo, path) ?: "Error loading file."
        }
    }

    fun closeFile() {
        _fileContent.value = null
        _currentFilePath.value = null
    }

    fun navigateBack(): Boolean {
        if (_fileContent.value != null) {
            closeFile()
            return true
        }
        if (_selectedRepo.value != null) {
            _selectedRepo.value = null
            _dirContents.value = emptyMap()
            return true
        }
        return false
    }
}
