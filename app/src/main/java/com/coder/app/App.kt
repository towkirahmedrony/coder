package com.coder.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.coder.app.core.database.ChatDatabase
import com.coder.app.core.network.ApiClient
import com.coder.app.features.chat.data.ChatRepository
import com.coder.app.features.settings.data.SettingsRepository
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppContainer(context: Context) {
    val database: ChatDatabase by lazy { Room.databaseBuilder(context, ChatDatabase::class.java, "chat_database").fallbackToDestructiveMigration().build() }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context.dataStore) }
    val apiClient: ApiClient by lazy { ApiClient() }
    val chatRepository: ChatRepository by lazy { ChatRepository(database.chatDao(), apiClient, settingsRepository) }
}

class App : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        
        // 🚀 NEW: Token এবং Workspace ম্যানেজার ইনিশিয়ালাইজ
        com.coder.app.core.network.TokenManager.init(this)
        com.coder.app.features.workspace.data.WorkspaceManager.init(this)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val crashFile = File(filesDir, "crash.txt")
                val sw = StringWriter()
                exception.printStackTrace(PrintWriter(sw))
                crashFile.writeText(sw.toString())
            } catch (e: Exception) { }
            defaultHandler?.uncaughtException(thread, exception)
        }

        container = AppContainer(this)
    }
}
