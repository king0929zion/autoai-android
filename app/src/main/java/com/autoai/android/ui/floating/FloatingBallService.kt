package com.autoai.android.ui.floating

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.autoai.android.R
import timber.log.Timber

/**
 * 悬浮球服务
 * 在任务执行时显示悬浮控制球,提供快捷操作入口
 */
class FloatingBallService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isFloatingWindowShown = false

    companion object {
        private const val CHANNEL_ID = "floating_ball_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("FloatingBallService 创建")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("FloatingBallService 启动")

        if (!isFloatingWindowShown) {
            showFloatingWindow()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("FloatingBallService 销毁")
        hideFloatingWindow()
    }

    /**
     * 显示悬浮窗
     */
    private fun showFloatingWindow() {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // 创建悬浮窗视图 (暂时使用简单视图)
            floatingView = View(this).apply {
                // TODO: 替换为自定义悬浮球布局
                setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
            }

            // 设置悬浮窗参数
            val params = WindowManager.LayoutParams().apply {
                width = 120
                height = 120
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 100
            }

            // 添加触摸监听
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            floatingView?.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        // 检测是否为点击 (移动距离小于阈值)
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {
                            // 点击事件 - 打开主界面
                            openMainActivity()
                        }
                        true
                    }
                    else -> false
                }
            }

            // 添加到窗口管理器
            windowManager?.addView(floatingView, params)
            isFloatingWindowShown = true

            Timber.d("悬浮窗显示成功")
        } catch (e: Exception) {
            Timber.e(e, "显示悬浮窗失败")
        }
    }

    /**
     * 隐藏悬浮窗
     */
    private fun hideFloatingWindow() {
        try {
            if (isFloatingWindowShown && floatingView != null) {
                windowManager?.removeView(floatingView)
                floatingView = null
                isFloatingWindowShown = false
                Timber.d("悬浮窗已隐藏")
            }
        } catch (e: Exception) {
            Timber.e(e, "隐藏悬浮窗失败")
        }
    }

    /**
     * 打开主界面
     */
    private fun openMainActivity() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "打开主界面失败")
        }
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮球服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示任务执行状态的悬浮球"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Timber.d("通知渠道已创建")
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoAI 运行中")
            .setContentText("点击返回应用")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 临时图标
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
