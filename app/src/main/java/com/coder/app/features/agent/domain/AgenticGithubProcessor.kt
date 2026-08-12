package com.coder.app.features.agent.domain

import com.coder.app.core.model.ChatMessage
import com.coder.app.core.network.GithubClient

class AgenticGithubProcessor(private val githubClient: GithubClient) {

    suspend fun process(
        repoOwner: String,
        repoName: String,
        userQuery: String,
        onUpdate: (String) -> Unit,
        aiPromptRunner: suspend (List<ChatMessage>) -> String
    ): String {
        onUpdate("Fetching repository info...")
        
        // ডায়নামিক ব্রাঞ্চ ডিটেকশন
        val branch = try {
            githubClient.getDefaultBranch(repoOwner, repoName)
        } catch (e: Exception) { "main" }

        onUpdate("Analyzing $branch branch tree...")
        val treeContext = try {
            githubClient.getRepositoryTree(repoOwner, repoName, branch)
        } catch (e: Exception) {
            return "Failed to fetch repository structure: ${e.message}"
        }

        // 🚀 NEW: শক্তিশালী System Prompt
        val systemPrompt = """
            You are an expert AI software engineer analyzing a GitHub repository ($repoOwner/$repoName).
            Here is the complete file structure of the repository:
            
            <repository_tree>
            $treeContext
            </repository_tree>
            
            CRITICAL INSTRUCTIONS:
            1. You MUST NOT guess or hallucinate the content of the files.
            2. If you need to see what is inside a file, you MUST reply with EXACTLY this tag:
               <read_file>path/to/file.ext</read_file>
            3. Do not add markdown code blocks around the tag. Just output the tag.
            4. The system will intercept the tag and provide you with the file's code. You can do this multiple times.
            5. Once you have read enough files and know the exact answer, provide your final response to the user.
        """.trimIndent()

        // 🚀 NEW: ইউজারের প্রশ্নের সাথে কড়া নির্দেশ
        val enhancedUserQuery = "$userQuery\n\n(Reminder: Do NOT guess the code. Use the <read_file> tag to read actual files from the tree provided in the system prompt before answering!)"

        val messages = mutableListOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = enhancedUserQuery)
        )

        var stepCount = 0
        val maxSteps = 6 

        while (stepCount < maxSteps) {
            onUpdate(if (stepCount == 0) "Thinking..." else "Analyzing file & deciding next step...")
            
            val aiResponse = aiPromptRunner(messages)
            messages.add(ChatMessage(role = "assistant", content = aiResponse))

            // Regex আপডেট করা হলো যেন স্পেস বা নতুন লাইন থাকলেও ট্যাগ ধরতে পারে
            val readFileRegex = "<read_file>\\s*(.*?)\\s*</read_file>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matchResult = readFileRegex.find(aiResponse)

            if (matchResult != null) {
                val filePath = matchResult.groupValues[1].trim()
                onUpdate("Reading $filePath...")
                
                val fileContent = try {
                    githubClient.getFileContent(repoOwner, repoName, filePath)
                } catch (e: Exception) {
                    "Error reading file: ${e.message}"
                }

                messages.add(ChatMessage(
                    role = "user", 
                    content = "System provided content of $filePath:\n```\n$fileContent\n```\nNow continue your analysis or provide the final answer."
                ))
                stepCount++
            } else {
                return aiResponse // <read_file> ট্যাগ না থাকলে ধরে নেব কাজ শেষ
            }
        }

        return "Agent reached maximum execution steps. Last output:\n${messages.last().content}"
    }
}
