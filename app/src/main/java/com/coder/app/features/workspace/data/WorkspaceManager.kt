package com.coder.app.features.workspace.data

import android.content.Context
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

@Keep
data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean
)

object WorkspaceManager {

    private lateinit var workspaceRoot: File
    private val ignoreDirs = listOf(".git", "build", ".gradle", ".idea", "node_modules")
    private val binaryExts = listOf("png", "jpg", "jpeg", "gif", "webp", "jar", "class", "apk", "dex", "zip", "ttf", "woff", "mp3", "mp4")

    fun init(context: Context) {
        workspaceRoot = File(context.filesDir, "workspace")
        if (!workspaceRoot.exists()) workspaceRoot.mkdirs()
    }

    suspend fun cloneRepository(repoUrl: String, repoName: String, token: String?, onProgress: (String) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        val targetDir = File(workspaceRoot, repoName)
        try {
            if (targetDir.exists()) {
                onProgress("Workspace exists. Syncing latest changes...")
                Git.open(targetDir).use { git ->
                    val pullCommand = git.pull()
                    if (!token.isNullOrBlank()) pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider("TOKEN", token))
                    pullCommand.call()
                }
                return@withContext Result.success(targetDir)
            }

            onProgress("Cloning repository into local workspace...")
            targetDir.mkdirs()
            val cloneCommand = Git.cloneRepository().setURI(repoUrl).setDirectory(targetDir).setCloneAllBranches(true)
            if (!token.isNullOrBlank()) cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider("TOKEN", token))
            
            cloneCommand.call().use { onProgress("Clone successful! Local Workspace ready.") }
            Result.success(targetDir)
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            Result.failure(e)
        }
    }

    suspend fun getProjectTree(repoName: String): String = withContext(Dispatchers.IO) {
        val root = File(workspaceRoot, repoName)
        if (!root.exists()) return@withContext "Repository not found locally."
        
        val treeBuilder = java.lang.StringBuilder()
        root.walkTopDown().forEach { file ->
            val relativePath = file.relativeTo(root).path
            if (relativePath.isNotEmpty()) {
                if (ignoreDirs.any { relativePath.contains(it) }) return@forEach
                val type = if (file.isDirectory) "dir" else "file"
                treeBuilder.append("- $relativePath ($type)\n")
            }
        }
        treeBuilder.toString()
    }

    suspend fun searchCode(repoName: String, query: String): String = withContext(Dispatchers.IO) {
        val root = File(workspaceRoot, repoName)
        if (!root.exists()) return@withContext "Repository not found."
        
        val results = StringBuilder()
        var matchCount = 0
        
        root.walkTopDown().forEach { file ->
            val relativePath = file.relativeTo(root).path
            if (file.isDirectory || ignoreDirs.any { relativePath.contains(it) } || binaryExts.contains(file.extension.lowercase())) return@forEach
            
            try {
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    if (line.contains(query, ignoreCase = true)) {
                        results.append("$relativePath:${index + 1}: ${line.trim()}\n")
                        matchCount++
                        if (matchCount > 50) return@forEach
                    }
                }
            } catch (e: Exception) { }
        }
        if (results.isEmpty()) "No matches found for '$query'." else results.toString()
    }

    suspend fun readFile(repoName: String, relativePath: String): String? = withContext(Dispatchers.IO) {
        val file = File(File(workspaceRoot, repoName), relativePath)
        if (file.exists() && file.isFile) file.readText() else null
    }

    suspend fun writeToFile(repoName: String, relativePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(File(workspaceRoot, repoName), relativePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) { false }
    }

    // =========================================================================
    // 🚀 NEW: UI-এর জন্য Workspace Explorer মেথডসমূহ
    // =========================================================================
    
    fun listRepositories(): List<String> {
        return workspaceRoot.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    }

    fun listFiles(repoName: String, relativePath: String = ""): List<FileNode> {
        val targetDir = File(File(workspaceRoot, repoName), relativePath)
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        return targetDir.listFiles()
            ?.filter { !ignoreDirs.contains(it.name) }
            ?.map {
                FileNode(
                    name = it.name,
                    path = it.relativeTo(File(workspaceRoot, repoName)).path,
                    isDirectory = it.isDirectory
                )
            }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
    }
}
