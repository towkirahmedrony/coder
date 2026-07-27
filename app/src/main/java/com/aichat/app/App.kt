package com.aichat.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.aichat.app.data.local.ChatDatabase
import com.aichat.app.data.remote.ApiClient
import com.aichat.app.data.repository.ChatRepository
import com.aichat.app.data.repository.SettingsRepository
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppContainer(context: Context) {
    val database: ChatDatabase by lazy {
        Room.databaseBuilder(context, ChatDatabase::class.java, "chat_database")
            .fallbackToDestructiveMigration()
            .build()
    }
    
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context.dataStore)
    }

    val apiClient: ApiClient by lazy {
        ApiClient()
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(database.chatDao(), apiClient, settingsRepository)
    }
}

class App : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        
        // গ্লোবাল ক্র্যাশ হ্যান্ডলার: এটি ইন্টারনাল ফাইলে ক্র্যাশ লগ সেভ করে রাখবে
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val crashFile = File(filesDir, "crash.txt")
                val sw = StringWriter()
                exception.printStackTrace(PrintWriter(sw))
                crashFile.writeText(sw.toString())
            } catch (e: Exception) {
                // ইগনোর
            }
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        container = AppContainer(this)
    }
}
