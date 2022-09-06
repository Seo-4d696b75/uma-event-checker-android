package jp.seo.uma.eventchecker.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.img.ImageProcess
import jp.seo.uma.eventchecker.ui.DebugDialogLaunchActivity
import jp.seo.uma.eventchecker.ui.overlay.createOverlayView
import javax.inject.Inject

/**
 * バックグラウンドでの処理が必要なタスクを担当
 *
 * - MediaProjectionAPIでキャプチャした画像を受け取り処理
 * - 検索結果をUIに反映
 * @author Seo-4d696b75
 * @version 2021/07/02.
 */
@AndroidEntryPoint
class CheckerService : LifecycleService() {

    companion object {
        const val NOTIFICATION_TAG = 564
        const val NOTIFICATION_CHANNEL_ID = "event_checker_service"

        const val KEY_REQUEST = "request"
        const val REQUEST_EXIT_SERVICE = "exit"
    }

    @Inject
    lateinit var capture: ScreenCapture

    @Inject
    lateinit var dataRepository: DataRepository

    @Inject
    lateinit var imageProcess: ImageProcess

    @Inject
    lateinit var settingRepository: SettingRepository

    private lateinit var windowManager: WindowManager
    private lateinit var sensorManager: SensorManager

    private var view: View? = null

    private val viewModel: MainViewModel by lazy {
        MainViewModel.getInstance(ViewModelStore(), dataRepository, imageProcess, settingRepository, capture)
    }

    private val shakeDetector = ShakeDetector()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let {
            if (it.hasExtra(KEY_REQUEST)) {
                when (it.getStringExtra(KEY_REQUEST)) {
                    REQUEST_EXIT_SERVICE -> stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "MainNotification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "This is main notification"
            notificationManager.createNotificationChannel(channel)
        }

        val exit = Intent(applicationContext, CheckerService::class.java)
            .putExtra(KEY_REQUEST, REQUEST_EXIT_SERVICE)
        val pending = PendingIntent.getService(
            applicationContext,
            1,
            exit,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_custom_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .addAction(
                    R.drawable.ic_launcher_custom_foreground,
                    getString(R.string.notification_action_stop),
                    pending
                )
                .build()
        startForeground(NOTIFICATION_TAG, notification)

        viewModel.setScreenCallback {
            viewModel.updateScreen(it)
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager


        // init overlay view
        val layerType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val layoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0,
            applicationContext.resources.getDimensionPixelSize(R.dimen.overlay_margin_top),
            layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParam.gravity = Gravity.END or Gravity.TOP
        layoutParam.screenBrightness = -1f
        val overlay = createOverlayView(dataRepository, settingRepository, this)
        this.view = overlay
        windowManager.addView(overlay, layoutParam)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(
            shakeDetector,
            sensor,
            SensorManager.SENSOR_DELAY_GAME,
        )

        shakeDetector.shakeEvent.observe(this, "checker-service") {
            if (settingRepository.isDebugDialogShown.value) return@observe
            settingRepository.isDebugDialogShown.value = true
            val intent = Intent(applicationContext, DebugDialogLaunchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopCapture()
        sensorManager.unregisterListener(shakeDetector)
        view?.let {
            windowManager.removeView(it)
            view = null
        }
    }
}
