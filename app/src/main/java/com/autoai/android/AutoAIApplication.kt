package com.autoai.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * AI 自主控机系统应用入口
 * 负责初始化全局配置和依赖注入
 */
@HiltAndroidApp
class AutoAIApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Timber 日志
        initTimber()
        
        Timber.i("AutoAI Application 启动")
    }

    /**
     * 初始化日志系统
     */
    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            // Debug 模式：输出详细日志
            Timber.plant(Timber.DebugTree())
        } else {
            // Release 模式：只记录警告和错误
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority >= android.util.Log.WARN) {
                        // 可以在这里添加崩溃日志上报
                        // 例如: Crashlytics.log(message)
                    }
                }
            })
        }
    }
}
