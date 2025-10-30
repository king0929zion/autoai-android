package com.autoai.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.autoai.android.permission.OperationExecutor
import com.autoai.android.permission.ShizukuManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// DataStore 扩展属性
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Hilt 依赖注入模块
 * 提供应用级别的单例依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideShizukuManager(): ShizukuManager {
        return ShizukuManager()
    }

    @Provides
    @Singleton
    fun provideOperationExecutor(shizukuManager: ShizukuManager): OperationExecutor {
        return OperationExecutor(shizukuManager)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
