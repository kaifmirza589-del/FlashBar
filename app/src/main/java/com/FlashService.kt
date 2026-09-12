package com.flashbar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

class FlashService : Service() {

    private lateinit var windowManager: WindowManager

    private var flashView: LinearLayout? = null
    private var moveButton: TextView? = null
    private var barParams: WindowManager.LayoutParams? = null

    private var brightness = 1.0f

    private var barLength = 500
    private var barThickness = 80

    private var posX = 0
    private var posY = 0

    private var horizontal = true
    private var moveMode = false

    companion object {

        const val CHANNEL_ID = "FlashBarChannel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_TOGGLE =
            "com.flashbar.TOGGLE"

        const val ACTION_BRIGHT_UP =
            "com.flashbar.BRIGHT_UP"

        const val ACTION_BRIGHT_DOWN =
            "com.flashbar.BRIGHT_DOWN"

        const val ACTION_WIDTH_UP =
            "com.flashbar.WIDTH_UP"

        const val ACTION_WIDTH_DOWN =
            "com.flashbar.WIDTH_DOWN"

        const val ACTION_HEIGHT_UP =
            "com.flashbar.HEIGHT_UP"

        const val ACTION_HEIGHT_DOWN =
            "com.flashbar.HEIGHT_DOWN"

        const val ACTION_ROTATE =
            "com.flashbar.ROTATE"

        const val ACTION_MOVE =
            "com.flashbar.MOVE"

        const val ACTION_OFF =
            "com.flashbar.OFF"
    }

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(WINDOW_SERVICE)
                    as WindowManager

        createNotificationChannel()

        loadSettings()

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
                    min(
                        1.0f,
                        brightness + 0.1f
                    )

                saveSettings()
                updateFlashBar()
            }

            ACTION_BRIGHT_DOWN -> {

                brightness =
                    max(
                        0.1f,
                        brightness - 0.1f
                    )

                saveSettings()
                updateFlashBar()
            }

            // L = Long
            ACTION_WIDTH_UP -> {

                barLength =
                    min(
                        1500,
                        barLength + 50
                    )

                saveSettings()
                updateFlashBar()
            }

            // S = Short
            ACTION_WIDTH_DOWN -> {

                barLength =
                    max(
                        100,
                        barLength - 50
                    )

                saveSettings()
                updateFlashBar()
            }

            // B = Bigger / thicker
            ACTION_HEIGHT_UP -> {

                barThickness =
                    min(
                        500,
                        barThickness + 20
                    )

                saveSettings()
                updateFlashBar()
            }

            // P = Patla / thinner
            ACTION_HEIGHT_DOWN -> {

                barThickness =
                    max(
                        20,
                        barThickness - 20
                    )

                saveSettings()
                updateFlashBar()
            }

            // V/H
            ACTION_ROTATE -> {

                horizontal = !horizontal

                saveSettings()
                updateFlashBar()
            }

            ACTION_MOVE -> {

                moveMode = !moveMode

                moveButton?.text =
                    if (moveMode) "✓"
                    else "✥"

                updateNotification()
            }

            ACTION_OFF -> {

                removeFlashBar()

                stopSelf()

                return START_NOT_STICKY
            }
        }

        updateNotification()

        return START_STICKY
    }

    private fun showFlashBar() {

        if (flashView != null) return

        val bar =
            LinearLayout(this)

        bar.setBackgroundColor(
            Color.WHITE
        )

        bar.alpha =
            brightness

        bar.orientation =
            LinearLayout.HORIZONTAL

        val params =
            WindowManager.LayoutParams(

                if (horizontal)
                    barLength
                else
                    barThickness,

                if (horizontal)
                    barThickness
                else
                    barLength,

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,

                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,

                PixelFormat.TRANSLUCENT
            )

        params.gravity =
            Gravity.TOP or Gravity.START

        params.x = posX
        params.y = posY

        barParams = params

        addMoveButton(bar)

        windowManager.addView(
            bar,
            params
        )

        flashView = bar
    }

    private fun addMoveButton(
        bar: LinearLayout
    ) {

        val button =
            TextView(this)

        moveButton = button

        button.text =
            if (moveMode)
                "✓"
            else
                "✥"

        button.textSize = 20f

        button.setTextColor(
            Color.WHITE
        )

        button.setBackgroundColor(
            Color.BLACK
        )

        button.gravity =
            Gravity.CENTER

        val buttonParams =
            LinearLayout.LayoutParams(
                60,
                60
            )

        button.layoutParams =
            buttonParams

        button.setOnClickListener {

            moveMode = !moveMode

            button.text =
                if (moveMode)
                    "✓"
                else
                    "✥"

            updateNotification()
        }

        setupMoveDrag(button)

        bar.addView(button)
    }

    private fun setupMoveDrag(
        button: TextView
    ) {

        var startX = 0
        var startY = 0

        var touchX = 0f
        var touchY = 0f

        button.setOnTouchListener { _, event ->

            if (!moveMode) {
                return@setOnTouchListener false
            }

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    val params =
                        barParams
                            ?: return@setOnTouchListener false

                    startX = params.x
                    startY = params.y

                    touchX = event.rawX
                    touchY = event.rawY

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val params =
                        barParams
                            ?: return@setOnTouchListener false

                    params.x =
                        startX +
                                (
                                    event.rawX -
                                            touchX
                                    ).toInt()

                    params.y =
                        startY +
                                (
                                    event.rawY -
                                            touchY
                                    ).toInt()

                    posX = params.x
                    posY = params.y

                    val bar =
                        flashView
                            ?: return@setOnTouchListener false

                    windowManager.updateViewLayout(
                        bar,
                        params
                    )

                    true
                }

                MotionEvent.ACTION_UP -> {

                    saveSettings()

                    true
                }

                else -> false
            }
        }
    }

    private fun updateFlashBar() {

        val bar =
            flashView ?: return

        val params =
            barParams ?: return

        bar.alpha =
            brightness

        if (horizontal) {

            params.width =
                barLength

            params.height =
                barThickness

        } else {

            params.width =
                barThickness

            params.height =
                barLength
        }

        params.x = posX
        params.y = posY

        windowManager.updateViewLayout(
            bar,
            params
        )

        updateNotification()
    }

    private fun removeFlashBar() {

        flashView?.let {

            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        flashView = null
        moveButton = null
        barParams = null
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "FlashBar Controls",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "FlashBar Controls"

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
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

        return Notification.Builder(
            this,
            CHANNEL_ID
        )

            .setSmallIcon(
                android.R.drawable.ic_menu_view
            )

            .setContentTitle(
                "FlashBar"
            )

            .setContentText(
                if (flashView != null)
                    "ON"
                else
                    "OFF"
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
                    "D-",
                    actionIntent(ACTION_BRIGHT_DOWN)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "D+",
                    actionIntent(ACTION_BRIGHT_UP)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "S",
                    actionIntent(ACTION_WIDTH_DOWN)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "L",
                    actionIntent(ACTION_WIDTH_UP)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "B",
                    actionIntent(ACTION_HEIGHT_UP)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "P",
                    actionIntent(ACTION_HEIGHT_DOWN)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "V",
                    actionIntent(ACTION_ROTATE)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "H",
                    actionIntent(ACTION_ROTATE)
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "MOVE",
                    actionIntent(ACTION_MOVE)
                ).build()
            )

            .build()
    }

    private fun updateNotification() {

        getSystemService(
            NotificationManager::class.java
        ).notify(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    private fun saveSettings() {

        getSharedPreferences(
            "flashbar_settings",
            MODE_PRIVATE
        )
            .edit()

            .putFloat(
                "brightness",
                brightness
            )

            .putInt(
                "barLength",
                barLength
            )

            .putInt(
                "barThickness",
                barThickness
            )

            .putInt(
                "posX",
                posX
            )

            .putInt(
                "posY",
                posY
            )

            .putBoolean(
                "horizontal",
                horizontal
            )

            .apply()
    }

    private fun loadSettings() {

        val prefs =
            getSharedPreferences(
                "flashbar_settings",
                MODE_PRIVATE
            )

        brightness =
            prefs.getFloat(
                "brightness",
                1.0f
            )

        barLength =
            prefs.getInt(
                "barLength",
                500
            )

        barThickness =
            prefs.getInt(
                "barThickness",
                80
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

        horizontal =
            prefs.getBoolean(
                "horizontal",
                true
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
