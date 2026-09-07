package com.flashbar

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

class FlashService : Service() {

    private lateinit var windowManager: WindowManager
    private var flashView: View? = null

    private var brightness = 1.0f

    // true = Horizontal, false = Vertical
    private var horizontal = true

    // Bar thickness
    private var barSize = 80

    // Bar length as percentage of screen
    private var barLengthPercent = 100

    // Position
    private var posX = 0
    private var posY = 0

    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0

    companion object {

        const val CHANNEL_ID = "FlashBarChannel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_TOGGLE = "com.flashbar.TOGGLE"
        const val ACTION_BRIGHT_UP = "com.flashbar.BRIGHT_UP"
        const val ACTION_BRIGHT_DOWN = "com.flashbar.BRIGHT_DOWN"
        const val ACTION_HEIGHT_UP = "com.flashbar.HEIGHT_UP"
        const val ACTION_HEIGHT_DOWN = "com.flashbar.HEIGHT_DOWN"

        const val ACTION_ROTATE = "com.flashbar.ROTATE"
        const val ACTION_SIZE_UP = "com.flashbar.SIZE_UP"
        const val ACTION_SIZE_DOWN = "com.flashbar.SIZE_DOWN"
    }

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        loadSettings()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

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

                brightness =
                    min(1.0f, brightness + 0.1f)

                saveSettings()
                updateFlashBar()
            }

            ACTION_BRIGHT_DOWN -> {

                brightness =
                    max(0.1f, brightness - 0.1f)

                saveSettings()
                updateFlashBar()
            }

            ACTION_HEIGHT_UP -> {

                barSize =
                    min(500, barSize + 20)

                saveSettings()
                updateFlashBar()
            }

            ACTION_HEIGHT_DOWN -> {

                barSize =
                    max(20, barSize - 20)

                saveSettings()
                updateFlashBar()
            }

            ACTION_SIZE_UP -> {

                barLengthPercent =
                    min(100, barLengthPercent + 10)

                saveSettings()
                updateFlashBar()
            }

            ACTION_SIZE_DOWN -> {

                barLengthPercent =
                    max(20, barLengthPercent - 10)

                saveSettings()
                updateFlashBar()
            }

            ACTION_ROTATE -> {

                horizontal = !horizontal

                posX = 0
                posY = 0

                saveSettings()
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

        val params = createLayoutParams()

        view.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = params.x
                    startY = params.y

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx =
                        (event.rawX - downX).toInt()

                    val dy =
                        (event.rawY - downY).toInt()

                    params.x = startX + dx
                    params.y = startY + dy

                    windowManager.updateViewLayout(
                        view,
                        params
                    )

                    posX = params.x
                    posY = params.y

                    true
                }

                MotionEvent.ACTION_UP -> {

                    saveSettings()

                    true
                }

                else -> false
            }
        }

        windowManager.addView(
            view,
            params
        )

        flashView = view
    }

    private fun createLayoutParams():
            WindowManager.LayoutParams {

        val metrics =
            resources.displayMetrics

        val screenWidth =
            metrics.widthPixels

        val screenHeight =
            metrics.heightPixels

        val length =
            if (horizontal) {

                screenWidth *
                        barLengthPercent / 100

            } else {

                screenHeight *
                        barLengthPercent / 100
            }

        val width =
            if (horizontal) {

                length

            } else {

                barSize
            }

        val height =
            if (horizontal) {

                barSize

            } else {

                length
            }

        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,

            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,

            PixelFormat.TRANSLUCENT
        ).apply {

            gravity = Gravity.TOP or Gravity.START

            x = posX
            y = posY
        }
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

        flashView?.let { view ->

            view.alpha = brightness

            val params =
                createLayoutParams()

            windowManager.updateViewLayout(
                view,
                params
            )
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "FlashBar Controls",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description =
                "FlashBar persistent controls"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun actionIntent(
        action: String
    ): PendingIntent {

        val intent =
            Intent(
                this,
                ControlReceiver::class.java
            )

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

        val direction =
            if (horizontal)
                "Horizontal"
            else
                "Vertical"

        return Notification.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_menu_view
            )
            .setContentTitle(
                "FlashBar is running"
            )
            .setContentText(
                "$direction • Brightness ${(brightness * 100).toInt()}% • Size $barLengthPercent%"
            )
            .setOngoing(true)
            .setCategory(
                Notification.CATEGORY_SERVICE
            )

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
                    "Size −",
                    actionIntent(ACTION_SIZE_DOWN)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Size +",
                    actionIntent(ACTION_SIZE_UP)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "H/V",
                    actionIntent(ACTION_ROTATE)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Thickness +",
                    actionIntent(ACTION_HEIGHT_UP)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Thickness −",
                    actionIntent(ACTION_HEIGHT_DOWN)
                ).build()
            )

            .build()
    }

    private fun updateNotification() {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.notify(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    private fun saveSettings() {

        getSharedPreferences(
            "FlashBarSettings",
            MODE_PRIVATE
        )
            .edit()
            .putFloat(
                "brightness",
                brightness
            )
            .putBoolean(
                "horizontal",
                horizontal
            )
            .putInt(
                "barSize",
                barSize
            )
            .putInt(
                "barLengthPercent",
                barLengthPercent
            )
            .putInt(
                "posX",
                posX
            )
            .putInt(
                "posY",
                posY
            )
            .apply()
    }

    private fun loadSettings() {

        val prefs =
            getSharedPreferences(
                "FlashBarSettings",
                MODE_PRIVATE
            )

        brightness =
            prefs.getFloat(
                "brightness",
                1.0f
            )

        horizontal =
            prefs.getBoolean(
                "horizontal",
                true
            )

        barSize =
            prefs.getInt(
                "barSize",
                80
            )

        barLengthPercent =
            prefs.getInt(
                "barLengthPercent",
                100
            )

        posX =
            prefs.getInt(
                "posX",
                0
            )

        posY =
            prefs.getInt(
                "posY",
                0
            )
    }

    override fun onDestroy() {

        removeFlashBar()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}
