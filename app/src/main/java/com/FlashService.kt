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
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min

class FlashService : Service() {

    private lateinit var windowManager: WindowManager

    private var flashView: LinearLayout? = null
    private var barParams: WindowManager.LayoutParams? = null

    private var brightness = 1.0f

    private var barWidth = 500
    private var barHeight = 80

    private var posX = 0
    private var posY = 0

    private var horizontal = true

    private var moveMode = false

    companion object {

        const val CHANNEL_ID = "FlashBarChannel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_TOGGLE = "com.flashbar.TOGGLE"

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

            ACTION_WIDTH_UP -> {

                barWidth =
                    min(
                        1000,
                        barWidth + 50
                    )

                saveSettings()
                updateFlashBar()
            }

            ACTION_WIDTH_DOWN -> {

                barWidth =
                    max(
                        100,
                        barWidth - 50
                    )

                saveSettings()
                updateFlashBar()
            }

            ACTION_HEIGHT_UP -> {

                barHeight =
                    min(
                        500,
                        barHeight + 20
                    )

                saveSettings()
                updateFlashBar()
            }

            ACTION_HEIGHT_DOWN -> {

                barHeight =
                    max(
                        20,
                        barHeight - 20
                    )

                saveSettings()
                updateFlashBar()
            }

            ACTION_ROTATE -> {

                horizontal = !horizontal

                saveSettings()
                updateFlashBar()
            }

            ACTION_MOVE -> {

                moveMode = !moveMode

                updateMoveButton()
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

        bar.alpha = brightness

        bar.orientation =
            if (horizontal)
                LinearLayout.HORIZONTAL
            else
                LinearLayout.VERTICAL

        bar.setPadding(
            8,
            8,
            8,
            8
        )

        val params =
            WindowManager.LayoutParams(

                if (horizontal)
                    barWidth
                else
                    barHeight,

                if (horizontal)
                    barHeight
                else
                    barWidth,

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

        addButtons(bar)

        windowManager.addView(
            bar,
            params
        )

        flashView = bar

        updateMoveButton()
    }

    private fun addButtons(
        bar: LinearLayout
    ) {

        val widthMinus =
            createButton("↔−")

        widthMinus.setOnClickListener {

            barWidth =
                max(
                    100,
                    barWidth - 50
                )

            saveSettings()
            updateFlashBar()
        }

        val widthPlus =
            createButton("↔+")

        widthPlus.setOnClickListener {

            barWidth =
                min(
                    1000,
                    barWidth + 50
                )

            saveSettings()
            updateFlashBar()
        }

        val heightMinus =
            createButton("↕−")

        heightMinus.setOnClickListener {

            barHeight =
                max(
                    20,
                    barHeight - 20
                )

            saveSettings()
            updateFlashBar()
        }

        val heightPlus =
            createButton("↕+")

        heightPlus.setOnClickListener {

            barHeight =
                min(
                    500,
                    barHeight + 20
                )

            saveSettings()
            updateFlashBar()
        }

        val brightMinus =
            createButton("−")

        brightMinus.setOnClickListener {

            brightness =
                max(
                    0.1f,
                    brightness - 0.1f
                )

            saveSettings()
            updateFlashBar()
        }

        val brightPlus =
            createButton("+")

        brightPlus.setOnClickListener {

            brightness =
                min(
                    1.0f,
                    brightness + 0.1f
                )

            saveSettings()
            updateFlashBar()
        }

        val rotate =
            createButton("▣")

        rotate.setOnClickListener {

            horizontal = !horizontal

            saveSettings()
            updateFlashBar()
        }

        /*
         * MOVE BUTTON
         *
         * Is button ko press karke hold/drag karne
         * se bar move hoga.
         */
        val move =
            createButton("✥")

        move.setOnClickListener {

            moveMode = !moveMode

            move.text =
                if (moveMode)
                    "✓"
                else
                    "✥"

            updateNotification()
        }

        setupMoveDrag(move)

        val off =
            createButton("⏻")

        off.setOnClickListener {

            removeFlashBar()
            stopSelf()
        }

        bar.addView(widthMinus)
        bar.addView(widthPlus)
        bar.addView(heightMinus)
        bar.addView(heightPlus)
        bar.addView(brightMinus)
        bar.addView(brightPlus)
        bar.addView(rotate)
        bar.addView(move)
        bar.addView(off)
    }

    private fun createButton(
        text: String
    ): TextView {

        val button =
            TextView(this)

        button.text = text
        button.textSize = 20f

        button.setTextColor(
            Color.WHITE
        )

        button.setBackgroundColor(
            Color.BLACK
        )

        button.gravity =
            Gravity.CENTER

        button.setPadding(
            10,
            0,
            10,
            0
        )

        val params =
            LinearLayout.LayoutParams(
                52,
                52
            )

        params.setMargins(
            6,
            0,
            6,
            0
        )

        button.layoutParams = params

        return button
    }

    /*
     * MOVE BUTTON DRAG
     *
     * MOVE (✥) press karo.
     * Button ✓ ban jayega.
     *
     * Ab isi MOVE button ko finger se
     * drag karke poori bar ko move karo.
     */
    private fun setupMoveDrag(
        moveButton: TextView
    ) {

        var startX = 0
        var startY = 0

        var touchX = 0f
        var touchY = 0f

        moveButton.setOnTouchListener { _, event ->

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

    private fun updateMoveButton() {

        val bar =
            flashView ?: return

        bar.alpha = brightness
    }

    private fun updateFlashBar() {

        val bar =
            flashView ?: return

        val params =
            barParams ?: return

        bar.alpha = brightness

        if (horizontal) {

            params.width =
                barWidth

            params.height =
                barHeight

            bar.orientation =
                LinearLayout.HORIZONTAL

        } else {

            params.width =
                barHeight

            params.height =
                barWidth

            bar.orientation =
                LinearLayout.VERTICAL
        }

        params.x = posX
        params.y = posY

        windowManager.updateViewLayout(
            bar,
            params
        )
    }

    private fun removeFlashBar() {

        flashView?.let {

            try {

                windowManager.removeView(
                    it
                )

            } catch (_: Exception) {
            }
        }

        flashView = null
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
                "Width $barWidth • Height $barHeight"
            )
            .setOngoing(true)
            .setCategory(
                Notification.CATEGORY_SERVICE
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "W−",
                    actionIntent(
                        ACTION_WIDTH_DOWN
                    )
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "W+",
                    actionIntent(
                        ACTION_WIDTH_UP
                    )
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "H−",
                    actionIntent(
                        ACTION_HEIGHT_DOWN
                    )
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "H+",
                    actionIntent(
                        ACTION_HEIGHT_UP
                    )
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Bright−",
                    actionIntent(
                        ACTION_BRIGHT_DOWN
                    )
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Bright+",
                    actionIntent(
                        ACTION_BRIGHT_UP
                    )
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Rotate",
                    actionIntent(
                        ACTION_ROTATE
                    )
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "Move",
                    actionIntent(
                        ACTION_MOVE
                    )
                ).build()
            )

            .addAction(
                Notification.Action.Builder(
                    null,
                    "OFF",
                    actionIntent(
                        ACTION_OFF
                    )
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

        val prefs =
            getSharedPreferences(
                "flashbar_settings",
                MODE_PRIVATE
            )

        prefs.edit()

            .putFloat(
                "brightness",
                brightness
            )

            .putInt(
                "barWidth",
                barWidth
            )

            .putInt(
                "barHeight",
                barHeight
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

        barWidth =
            prefs.getInt(
                "barWidth",
                500
            )

        barHeight =
            prefs.getInt(
                "barHeight",
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
