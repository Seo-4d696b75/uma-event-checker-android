package jp.seo.uma.eventchecker.ui.checker

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.data.repository.ScreenRepository
import jp.seo.uma.eventchecker.data.repository.SearchRepository
import jp.seo.uma.eventchecker.data.repository.SettingRepository
import jp.seo.uma.eventchecker.databinding.OverlayMainBinding
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
        const val REQUEST_START_MEDIA_PROJECTION = "start_media_projection"
        const val KEY_MEDIA_PROJECTION_DATA = "media_projection_data"
    }

    @Inject
    lateinit var capture: ScreenRepository

    @Inject
    lateinit var repository: SearchRepository

    @Inject
    lateinit var setting: SettingRepository

    private val viewModel: CheckerViewModel by lazy {
        CheckerViewModel(repository, setting, capture)
    }

    private lateinit var manager: WindowManager
    private var view: View? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let {
            if (it.hasExtra(KEY_REQUEST)) {
                when (it.getStringExtra(KEY_REQUEST)) {
                    REQUEST_EXIT_SERVICE -> stopSelf()
                    REQUEST_START_MEDIA_PROJECTION -> {
                        Log.d("onStartCommand", "start media projection")
                        val projectionManager =
                            getSystemService(Service.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        val data = it.getParcelable<Intent>(KEY_MEDIA_PROJECTION_DATA)
                            ?: throw IllegalArgumentException()
                        val projection =
                            projectionManager.getMediaProjection(Activity.RESULT_OK, data)
                        viewModel.startCapture(projection)
                    }
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
                .setContentTitle("UmaEventChecker")
                .setContentText("service is now running")
                .addAction(R.drawable.ic_launcher_custom_foreground, "stop", pending)
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

inline fun <reified T : Parcelable> Intent.getParcelable(key: String): T? =
    if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra<T>(key)
    }
