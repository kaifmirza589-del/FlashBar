package com.flashbar

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView

class FlashService : Service() {

    private lateinit var windowManager: WindowManager
    private var flashView: View? = null

    private var brightness = 1.0f
    private var barHeight = 80

    companion object {
        const val CHANNEL_ID = "FlashBarChannel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_TOGGLE = "com.flashbar.TOGGLE"
        const val ACTION_BRIGHT_UP = "com.flashbar.BRIGHT_UP"
        const val ACTION_BRIGHT_DOWN = "com.flashbar.BRIGHT_DOWN"
        const val ACTION_HEIGHT_UP = "com.flashbar.HEIGHT_UP"
        const val ACTION_HEIGHT_DOWN = "com.flashbar.HEIGHT_DOWN"
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        if (Settings.canDrawOverlays(this)) {
            showFlashBar()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_TOGGLE -> {
                if (flashView == null) {
                    showFlashBar()
                } else {
                    removeFlashBar()
                }
            }

            ACTION_BRIGHT_UP -> {
                brightness = (brightness + 0.1f).coerceAtMost(1.0f)
                updateFlashBar()
            }

            ACTION_BRIGHT_DOWN -> {
                brightness = (brightness - 0.1f).coerceAtLeast(0.1f)
                updateFlashBar()
            }

            ACTION_HEIGHT_UP -> {
                barHeight = (barHeight + 20).coerceAtMost(300)
                updateFlashBar()
            }

            ACTION_HEIGHT_DOWN -> {
                barHeight = (barHeight - 20).coerceAtLeast(20)
                updateFlashBar()
            }
        }

        updateNotification()

        return START_STICKY
    }

    private fun showFlashBar() {

        if (flashView != null) return

        val view = TextView(this)

        view.setBackgroundColor(Color.WHITE)
        view.alpha = brightness

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            barHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP

        windowManager.addView(view, params)

        flashView = view
    }

    private fun removeFlashBar() {

        flashView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        flashView = null
    }

    private fun updateFlashBar() {

        flashView?.let {

            it.alpha = brightness

            val params = it.layoutParams as WindowManager.LayoutParams

            params.height = barHeight

            windowManager.updateViewLayout(it, params)
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "FlashBar Controls",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description = "FlashBar persistent controls"

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    private fun actionIntent(action: String): PendingIntent {

        val intent = Intent(this, ControlReceiver::class.java)

        intent.action = action

        return PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotification(): Notification {

        val builder = Notification.Builder(this, CHANNEL_ID)

        builder
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("FlashBar is running")
            .setContentText(
                "Brightness: ${(brightness * 100).toInt()}% • Height: ${barHeight}px"
            )
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)

            .addAction(
                Notification.Action.Builder(
                    null,
                    "ON/OFF",
                    actionIntent(ACTION_TOGGLE)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Bright −",
                    actionIntent(ACTION_BRIGHT_DOWN)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Bright +",
                    actionIntent(ACTION_BRIGHT_UP)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Height −",
                    actionIntent(ACTION_HEIGHT_DOWN)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Height +",
                    actionIntent(ACTION_HEIGHT_UP)
                ).build()
            )

        return builder.build()
    }

    private fun updateNotification() {

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.notify(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    override fun onDestroy() {

        removeFlashBar()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
