package com.coder.app

import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coder.app.navigation.AppNavigation
import com.coder.app.ui.theme.AIChatTheme
import com.coder.app.viewmodel.SettingsViewModel
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. আগে চেক করবে কোনো ক্র্যাশ ফাইল আছে কিনা
        val crashFile = File(filesDir, "crash.txt")
        if (crashFile.exists()) {
            val crashText = crashFile.readText()
            crashFile.delete() // ডিলিট করা হচ্ছে যাতে বারবার না দেখায়
            
            // Compose বাদ দিয়ে সাধারণ Android View দিয়ে এরর দেখানো হচ্ছে
            val scrollView = ScrollView(this)
            val textView = TextView(this).apply {
                text = "CRASH LOG:\n\n$crashText"
                setTextColor(Color.RED)
                setPadding(32, 64, 32, 32)
                textSize = 14f
            }
            scrollView.addView(textView)
            setContentView(scrollView)
            return
        }

        // 2. কোনো ক্র্যাশ না থাকলে সাধারণ Compose লোড হবে
        try {
            enableEdgeToEdge()
            val appContainer = (application as App).container
            
            setContent {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(appContainer.settingsRepository)
                )
                
                val settings by settingsViewModel.settings.collectAsState()
                val useDarkTheme = when (settings.theme) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                }
                
                AIChatTheme(darkTheme = useDarkTheme) {
                    AppNavigation(appContainer)
                }
            }
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            
            val scrollView = ScrollView(this)
            val textView = TextView(this).apply {
                text = "CRASH DURING SETUP:\n\n${sw.toString()}"
                setTextColor(Color.RED)
                setPadding(32, 64, 32, 32)
                textSize = 14f
            }
            scrollView.addView(textView)
            setContentView(scrollView)
        }
    }
}
