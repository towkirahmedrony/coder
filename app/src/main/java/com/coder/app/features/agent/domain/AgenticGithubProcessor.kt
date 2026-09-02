package com.coder.app.features.agent.domain

import com.coder.app.core.model.ChatMessage
import com.coder.app.features.workspace.data.WorkspaceManager

class AgenticGithubProcessor {

    suspend fun process(
        repoName: String,
        userQuery: String,
        onUpdate: (String) -> Unit,
        aiPromptRunner: suspend (List<ChatMessage>) -> String
    ): String {
        onUpdate("Scanning local workspace tree...")
        val treeContext = WorkspaceManager.getProjectTree(repoName)

        val systemPrompt = """
            You are an autonomous AI software engineer working in a local workspace. 
            The repository has been cloned locally. Here is the file structure:
            
            <workspace_tree>
            $treeContext
            </workspace_tree>
            
            CRITICAL INSTRUCTIONS - YOU ARE A WORKSPACE AGENT:
            To interact with the workspace, you MUST output ONLY ONE of the following XML tool tags per turn. 
            Do NOT wrap the XML tags in markdown blocks. Just output the raw XML.

            1. Search Code:
            <search_code>your query</search_code>

            2. Read File:
            <read_file>path/to/file.ext</read_file>

            3. Edit File:
            <edit_file path="path/to/file.ext">
            [PUT THE ENTIRE NEW FILE CONTENT HERE]
            </edit_file>

            Work iteratively. Use <search_code> to find implementations, <read_file> to understand logic, and <edit_file> to modify.
            Once the task is fully complete and you don't need any more tools, provide your final response to the user.
        """.trimIndent()

        val messages = mutableListOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userQuery)
        )

        var stepCount = 0
        val maxSteps = 10 // Local execution is fast, so giving more steps to the agent

        // Regex for Tools
        val readRegex = "<read_file>\\s*(.*?)\\s*</read_file>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val searchRegex = "<search_code>\\s*(.*?)\\s*</search_code>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val editRegex = "<edit_file\\s+path=\"(.*?)\">\\s*(.*?)\\s*</edit_file>".toRegex(RegexOption.DOT_MATCHES_ALL)

        while (stepCount < maxSteps) {
            onUpdate(if (stepCount == 0) "Thinking..." else "Analyzing workspace & deciding next tool...")
            
            val aiResponse = aiPromptRunner(messages)
            messages.add(ChatMessage(role = "assistant", content = aiResponse))

            var toolUsed = false

            // 1. Check for Read File Tool
            val readMatch = readRegex.find(aiResponse)
            if (readMatch != null) {
                val path = readMatch.groupValues[1].trim()
                onUpdate("Reading $path...")
                val content = WorkspaceManager.readFile(repoName, path) ?: "Error: File not found in workspace."
                messages.add(ChatMessage(role = "user", content = "Tool Output (Content of $path):\n```\n$content\n```\nContinue."))
                toolUsed = true
            } 
            // 2. Check for Search Tool
            else if (searchRegex.find(aiResponse) != null) {
                val searchMatch = searchRegex.find(aiResponse)!!
                val query = searchMatch.groupValues[1].trim()
                onUpdate("Searching workspace for '$query'...")
                val results = WorkspaceManager.searchCode(repoName, query)
                messages.add(ChatMessage(role = "user", content = "Tool Output (Search results for '$query'):\n```\n$results\n```\nContinue."))
                toolUsed = true
            } 
            // 3. Check for Edit Tool
            else if (editRegex.find(aiResponse) != null) {
                val editMatch = editRegex.find(aiResponse)!!
                val path = editMatch.groupValues[1].trim()
                val content = editMatch.groupValues[2]
                onUpdate("Writing changes to $path...")
                val success = WorkspaceManager.writeToFile(repoName, path, content)
                val msg = if (success) "Successfully updated $path." else "Error: Failed to write to $path."
                messages.add(ChatMessage(role = "user", content = "Tool Output: $msg\nContinue."))
                toolUsed = true
            }

            if (!toolUsed) {
                return aiResponse // If no tool tags are found, assume it's the final answer
            }
            stepCount++
        }

        return "Agent reached maximum execution steps (10). Last output:\n${messages.last().content}"
    }
}
