package jp.seo.uma.eventchecker.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ListView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import jp.seo.uma.eventchecker.ui.EventChoiceAdapter
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2021/07/02.
 */
@AndroidEntryPoint
class CheckerService : LifecycleService() {


    inner class StationServiceBinder : Binder() {
        fun bind(): CheckerService {
            return this@CheckerService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d("service", "onBind: client requests to bind service")
        return StationServiceBinder()
    }


    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("service", "onUnbind: client unbinds service")
        return true
    }

    override fun onRebind(intent: Intent?) {
        Log.d("service", "onRebind: client binds service again")
    }


    companion object {
        const val NOTIFICATION_TAG = 564
        const val NOTIFICATION_CHANNEL_ID = "event_checker_service"

        const val KEY_REQUEST = "request"
        const val REQUEST_EXIT_SERVICE = "exit"
    }

    @Inject
    lateinit var store: ViewModelStore

    @Inject
    lateinit var capture: ScreenCapture

    private lateinit var manager: WindowManager
    private var view: View? = null

    private val viewModel: MainViewModel by lazy {
        MainViewModel.getInstance(store)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let {
            if (it.hasExtra(KEY_REQUEST)) {
                when (it.getStringExtra(KEY_REQUEST)) {
                    REQUEST_EXIT_SERVICE -> {
                        release()
                        stopSelf()
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
        val pending =
            PendingIntent.getService(applicationContext, 1, exit, PendingIntent.FLAG_ONE_SHOT)
        val notification =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("UmaEventChecker")
                .setContentText("service is now running")
                .addAction(R.drawable.ic_launcher_foreground, "stop", pending)
                .build()
        startForeground(NOTIFICATION_TAG, notification)

        capture.callback = {
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
        val view = inflater.inflate(R.layout.overlay_main, null, false)
        view.visibility = View.VISIBLE
        this.view = view
        val textTitle = view.findViewById<TextView>(R.id.text_overlay_title)
        val listChoice = view.findViewById<ListView>(R.id.list_overlay_choices)
        listChoice.divider = null
        listChoice.dividerHeight = 0
        manager.addView(view, layoutParam)


        viewModel.currentEvent.observe(this) { event ->
            Log.d("service", event?.eventTitle ?: "none")
            view.visibility = if (event == null) View.GONE else View.VISIBLE
            textTitle.text = event?.eventTitle ?: "None"
            listChoice.adapter = event?.let {
                EventChoiceAdapter(applicationContext, it.choices)
            }
        }
    }

    private fun release() {
        capture.release()
        view?.let {
            manager.removeView(it)
            view = null
        }
        viewModel.eventCallback = null
    }
}
