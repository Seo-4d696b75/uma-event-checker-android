package jp.seo.uma.eventchecker.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.databinding.OverlayMainBinding
import jp.seo.uma.eventchecker.img.ImageProcess
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
    lateinit var repository: DataRepository

    @Inject
    lateinit var imageProcess: ImageProcess

    @Inject
    lateinit var setting: SettingRepository

    private lateinit var manager: WindowManager
    private var view: View? = null

    private val viewModel: MainViewModel by lazy {
        MainViewModel.getInstance(ViewModelStore(), repository, imageProcess, setting, capture)
    }

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
        val pending =
            PendingIntent.getService(applicationContext, 1, exit, PendingIntent.FLAG_ONE_SHOT)
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

        manager = getSystemService(WINDOW_SERVICE) as WindowManager


        // init overlay view
        val inflater = LayoutInflater.from(applicationContext)
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
        val binding = OverlayMainBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        this.view = binding.root
        binding.listOverlayChoices.apply {
            divider = null
            dividerHeight = 0
        }
        manager.addView(binding.root, layoutParam)


    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopCapture()
        view?.let {
            manager.removeView(it)
            view = null
        }
    }
}
