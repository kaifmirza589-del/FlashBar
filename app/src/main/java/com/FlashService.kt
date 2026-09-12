package com.flashbar

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat

class FlashService : Service() {

    companion object {
        const val ACTION_OFF = "com.flashbar.ACTION_OFF"
        const val ACTION_ON = "com.flashbar.ACTION_ON"
        const val ACTION_RESET = "com.flashbar.ACTION_RESET"

        const val ACTION_BRIGHTNESS_DOWN = "com.flashbar.BRIGHTNESS_DOWN"
        const val ACTION_BRIGHTNESS_UP = "com.flashbar.BRIGHTNESS_UP"
        const val ACTION_LENGTH_DOWN = "com.flashbar.LENGTH_DOWN"
        const val ACTION_LENGTH_UP = "com.flashbar.LENGTH_UP"
        const val ACTION_THICK_DOWN = "com.flashbar.THICK_DOWN"
        const val ACTION_THICK_UP = "com.flashbar.THICK_UP"
        const val ACTION_VERTICAL = "com.flashbar.VERTICAL"
        const val ACTION_HORIZONTAL = "com.flashbar.HORIZONTAL"
        const val ACTION_MOVE = "com.flashbar.MOVE"

        private const val CHANNEL_ID = "flashbar_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var windowManager: WindowManager

    private var overlayView: LinearLayout? = null
    private var flashBar: View? = null
    private var controlRow: LinearLayout? = null

    private var barWidth = 500
    private var barHeight = 35
    private var brightness = 255

    private var isVertical = false
    private var isBarOn = true
    private var moveMode = false

    private var overlayParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        if (Settings.canDrawOverlays(this)) {
            createOverlay()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_OFF -> {
                isBarOn = false
                updateFlashBar()
            }

            ACTION_ON -> {
                isBarOn = true
                updateFlashBar()
            }

            ACTION_RESET -> {
                resetSettings()
            }

            ACTION_BRIGHTNESS_DOWN -> {
                brightness = (brightness - 25).coerceAtLeast(50)
                updateFlashBar()
            }

            ACTION_BRIGHTNESS_UP -> {
                brightness = (brightness + 25).coerceAtMost(255)
                updateFlashBar()
            }

            ACTION_LENGTH_DOWN -> {
                barWidth = (barWidth - 50).coerceAtLeast(150)
                updateFlashBar()
            }

            ACTION_LENGTH_UP -> {
                barWidth = (barWidth + 50).coerceAtMost(1000)
                updateFlashBar()
            }

            ACTION_THICK_DOWN -> {
                barHeight = (barHeight - 5).coerceAtLeast(10)
                updateFlashBar()
            }

            ACTION_THICK_UP -> {
                barHeight = (barHeight + 5).coerceAtMost(100)
                updateFlashBar()
            }

            ACTION_VERTICAL -> {
                isVertical = true
                updateFlashBar()
            }

            ACTION_HORIZONTAL -> {
                isVertical = false
                updateFlashBar()
            }

            ACTION_MOVE -> {
                moveMode = !moveMode
                updateMoveButton()
            }
        }

        return START_STICKY
    }

    private fun createOverlay() {

        if (overlayView != null) return

        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 4)
            setBackgroundColor(Color.TRANSPARENT)
        }

        controlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val buttons = listOf(
            "ON/OFF" to null,
            "D-" to ACTION_BRIGHTNESS_DOWN,
            "D+" to ACTION_BRIGHTNESS_UP,
            "S" to ACTION_LENGTH_DOWN,
            "L" to ACTION_LENGTH_UP,
            "B" to ACTION_THICK_UP,
            "P" to ACTION_THICK_DOWN,
            "V" to ACTION_VERTICAL,
            "H" to ACTION_HORIZONTAL,
            "MOVE" to ACTION_MOVE
        )

        buttons.forEach { (text, action) ->

            val button = Button(this).apply {
                this.text = text
                textSize = 10f
                setPadding(2, 0, 2, 0)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    42
                ).apply {
                    setMargins(1, 1, 1, 1)
                }

                setOnClickListener {

                    if (text == "ON/OFF") {
                        isBarOn = !isBarOn
                        updateFlashBar()
                    } else if (action != null) {
                        handleAction(action)
                    }
                }
            }

            controlRow?.addView(button)
        }

        flashBar = View(this).apply {
            setBackgroundColor(Color.WHITE)
        }

        overlayView?.addView(
            controlRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        overlayView?.addView(
            flashBar,
            LinearLayout.LayoutParams(
                barWidth,
                barHeight
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        val params = WindowManager.LayoutParams(
            if (isVertical) barHeight + 80 else barWidth,
            if (isVertical) barWidth else barHeight + 50,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.x = 0
        params.y = 80

        overlayParams = params

        windowManager.addView(overlayView, params)

        updateFlashBar()
    }

    private fun handleAction(action: String) {

        when (action) {

            ACTION_BRIGHTNESS_DOWN -> {
                brightness = (brightness - 25).coerceAtLeast(50)
            }

            ACTION_BRIGHTNESS_UP -> {
                brightness = (brightness + 25).coerceAtMost(255)
            }

            ACTION_LENGTH_DOWN -> {
                barWidth = (barWidth - 50).coerceAtLeast(150)
            }

            ACTION_LENGTH_UP -> {
                barWidth = (barWidth + 50).coerceAtMost(1000)
            }

            ACTION_THICK_DOWN -> {
                barHeight = (barHeight - 5).coerceAtLeast(10)
            }

            ACTION_THICK_UP -> {
                barHeight = (barHeight + 5).coerceAtMost(100)
            }

            ACTION_VERTICAL -> {
                isVertical = true
            }

            ACTION_HORIZONTAL -> {
                isVertical = false
            }

            ACTION_MOVE -> {
                moveMode = !moveMode
            }
        }

        updateFlashBar()
        updateMoveButton()
    }

    private fun updateFlashBar() {

        val bar = flashBar ?: return
        val root = overlayView ?: return
        val params = overlayParams ?: return

        bar.setBackgroundColor(
            if (isBarOn) {
                Color.rgb(brightness, brightness, brightness)
            } else {
                Color.TRANSPARENT
            }
        )

        if (isVertical) {

            bar.layoutParams = LinearLayout.LayoutParams(
                barHeight,
                barWidth
            ).apply {
                gravity = Gravity.CENTER
            }

            params.width = barHeight + 100
            params.height = barWidth + 60

        } else {

            bar.layoutParams = LinearLayout.LayoutParams(
                barWidth,
                barHeight
            ).apply {
                gravity = Gravity.CENTER
            }

            params.width = barWidth
            params.height = barHeight + 60
        }

        root.alpha = if (isBarOn) 1f else 0.65f

        windowManager.updateViewLayout(root, params)
        bar.requestLayout()
    }

    private fun updateMoveButton() {

        val row = controlRow ?: return

        for (i in 0 until row.childCount) {

            val button = row.getChildAt(i)

            if (button is Button && button.text.toString() == "MOVE") {
                button.text = if (moveMode) "MOVE✓" else "MOVE"

                if (moveMode) {
                    enableDragging()
                } else {
                    disableDragging()
                }
            }
        }
    }

    private fun enableDragging() {

        val row = controlRow ?: return

        row.setOnTouchListener(object : View.OnTouchListener {

            private var downX = 0
            private var downY = 0
            private var startX = 0
            private var startY = 0

            override fun onTouch(
                v: View?,
                event: MotionEvent?
            ): Boolean {

                val params = overlayParams ?: return false

                when (event?.actionMasked) {

                    MotionEvent.ACTION_DOWN -> {

                        downX = event.rawX.toInt()
                        downY = event.rawY.toInt()

                        startX = params.x
                        startY = params.y

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {

                        params.x =
                            startX + (event.rawX.toInt() - downX)

                        params.y =
                            startY + (event.rawY.toInt() - downY)

                        windowManager.updateViewLayout(
                            overlayView,
                            params
                        )

                        return true
                    }
                }

                return true
            }
        })
    }

    private fun disableDragging() {
        controlRow?.setOnTouchListener(null)
    }

    private fun resetSettings() {

        barWidth = 500
        barHeight = 35
        brightness = 255
        isVertical = false
        isBarOn = true
        moveMode = false

        overlayParams?.x = 0
        overlayParams?.y = 80

        updateFlashBar()
        updateMoveButton()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "FlashBar",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        val offIntent = Intent(this, ControlReceiver::class.java).apply {
            action = ACTION_OFF
        }

        val onIntent = Intent(this, ControlReceiver::class.java).apply {
            action = ACTION_ON
        }

        val offPendingIntent = PendingIntent.getBroadcast(
            this,
            10,
            offIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val onPendingIntent = PendingIntent.getBroadcast(
            this,
            11,
            onIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("FlashBar")
            .setContentText("FlashBar is running")
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "OFF",
                offPendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_play,
                "ON",
                onPendingIntent
            )
            .build()
    }

    override fun onDestroy() {

        try {
            overlayView?.let {
                windowManager.removeView(it)
            }
        } catch (_: Exception) {
        }

        overlayView = null
        flashBar = null
        controlRow = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
