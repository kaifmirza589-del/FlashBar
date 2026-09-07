package com.flashbar

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import kotlin.math.max
import kotlin.math.min

class FlashService : Service() {

    private lateinit var windowManager: WindowManager

    private var overlayView: LinearLayout? = null
    private var flashView: View? = null

    private var brightness = 1.0f
    private var sizeDp = 80

    // true = Horizontal, false = Vertical
    private var horizontal = true

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
        const val ACTION_SIZE_UP = "com.flashbar.SIZE_UP"
        const val ACTION_SIZE_DOWN = "com.flashbar.SIZE_DOWN"
        const val ACTION_ROTATE = "com.flashbar.ROTATE"
    }

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        loadSettings()

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

                if (overlayView == null) {
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

            ACTION_SIZE_UP -> {

                sizeDp =
                    min(300, sizeDp + 10)

                saveSettings()
                updateFlashBar()
            }

            ACTION_SIZE_DOWN -> {

                sizeDp =
                    max(20, sizeDp - 10)

                saveSettings()
                updateFlashBar()
            }

            ACTION_HEIGHT_UP -> {

                sizeDp =
                    min(300, sizeDp + 10)

                saveSettings()
                updateFlashBar()
            }

            ACTION_HEIGHT_DOWN -> {

                sizeDp =
                    max(20, sizeDp - 10)

                saveSettings()
                updateFlashBar()
            }

            ACTION_ROTATE -> {

                horizontal = !horizontal

                saveSettings()
                updateFlashBar()
            }
        }

        updateNotification()

        return START_STICKY
    }

    private fun showFlashBar() {

        if (overlayView != null) return

        val root = LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            4,
            4,
            4,
            4
        )

        root.setBackgroundColor(
            Color.TRANSPARENT
        )

        // =========================
        // CONTROL BUTTON ROW
        // =========================

        val controls = LinearLayout(this)

        controls.orientation =
            LinearLayout.HORIZONTAL

        controls.gravity =
            Gravity.CENTER

        controls.setBackgroundColor(
            Color.argb(230, 20, 20, 20)
        )

        addButton(
            controls,
            "↔/↕",
            60
        ) {
            horizontal = !horizontal
            saveSettings()
            updateFlashBar()
        }

        addButton(
            controls,
            "☀−",
            60
        ) {
            brightness =
                max(0.1f, brightness - 0.1f)

            saveSettings()
            updateFlashBar()
            updateNotification()
        }

        addButton(
            controls,
            "☀+",
            60
        ) {
            brightness =
                min(1.0f, brightness + 0.1f)

            saveSettings()
            updateFlashBar()
            updateNotification()
        }

        addButton(
            controls,
            "Size−",
            70
        ) {
            sizeDp =
                max(20, sizeDp - 10)

            saveSettings()
            updateFlashBar()
            updateNotification()
        }

        addButton(
            controls,
            "Size+",
            70
        ) {
            sizeDp =
                min(300, sizeDp + 10)

            saveSettings()
            updateFlashBar()
            updateNotification()
        }

        addButton(
            controls,
            "OFF",
            55
        ) {
            removeFlashBar()
            updateNotification()
        }

        root.addView(
            controls,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(45)
            )
        )

        // =========================
        // WHITE FLASH BAR
        // =========================

        val flash = View(this)

        flash.setBackgroundColor(
            Color.WHITE
        )

        flash.alpha = brightness

        flashView = flash

        root.addView(flash)

        // =========================
        // DRAG
        // =========================

        root.setOnTouchListener { _, event ->

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = posX
                    startY = posY

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    posX =
                        startX +
                                (event.rawX - downX).toInt()

                    posY =
                        startY +
                                (event.rawY - downY).toInt()

                    updatePosition()

                    true
                }

                MotionEvent.ACTION_UP -> {
                    saveSettings()
                    true
                }

                else -> false
            }
        }

        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

        params.gravity = Gravity.TOP or Gravity.START

        params.x = posX
        params.y = posY

        windowManager.addView(
            root,
            params
        )

        overlayView = root

        updateFlashBar()
    }

    private fun addButton(
        parent: LinearLayout,
        text: String,
        width: Int,
        action: () -> Unit
    ) {

        val button = TextView(this)

        button.text = text

        button.textSize = 13f

        button.setTextColor(
            Color.WHITE
        )

        button.gravity =
            Gravity.CENTER

        button.setPadding(
            6,
            0,
            6,
            0
        )

        button.setBackgroundColor(
            Color.rgb(45, 45, 45)
        )

        button.setOnClickListener {
            action()
        }

        val params =
            LinearLayout.LayoutParams(
                dp(width),
                dp(45)
            )

        params.setMargins(
            2,
            0,
            2,
            0
        )

        parent.addView(
            button,
            params
        )
    }

    private fun updateFlashBar() {

        val root = overlayView ?: return
        val flash = flashView ?: return

        flash.alpha = brightness

        val screenWidth =
            resources.displayMetrics.widthPixels

        val screenHeight =
            resources.displayMetrics.heightPixels

        if (horizontal) {

            val width =
                (screenWidth * 0.80f).toInt()

            val height =
                dp(sizeDp)

            flash.layoutParams =
                LinearLayout.LayoutParams(
                    width,
                    height
                )

        } else {

            val width =
                dp(sizeDp)

            val height =
                (screenHeight * 0.60f).toInt()

            flash.layoutParams =
                LinearLayout.LayoutParams(
                    width,
                    height
                )
        }

        root.requestLayout()

        updatePosition()
    }

    private fun updatePosition() {

        val root = overlayView ?: return

        val params =
            root.layoutParams as WindowManager.LayoutParams

        params.x = posX
        params.y = posY

        try {
            windowManager.updateViewLayout(
                root,
                params
            )
        } catch (_: Exception) {
        }
    }

    private fun removeFlashBar() {

        overlayView?.let {

            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }

        overlayView = null
        flashView = null
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "FlashBar Controls",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "FlashBar persistent controls"

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
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
                "Brightness: ${(brightness * 100).toInt()}% • Size: ${sizeDp}dp"
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
                    "Rotate",
                    actionIntent(ACTION_ROTATE)
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
            "flash_settings",
            MODE_PRIVATE
        )
            .edit()
            .putFloat(
                "brightness",
                brightness
            )
            .putInt(
                "sizeDp",
                sizeDp
            )
            .putBoolean(
                "horizontal",
                horizontal
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
                "flash_settings",
                MODE_PRIVATE
            )

        brightness =
            prefs.getFloat(
                "brightness",
                1.0f
            )

        sizeDp =
            prefs.getInt(
                "sizeDp",
                80
            )

        horizontal =
            prefs.getBoolean(
                "horizontal",
                true
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

    private fun dp(value: Int): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
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
