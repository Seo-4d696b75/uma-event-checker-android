package jp.seo.uma.eventchecker.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.uma.eventchecker.R
import javax.inject.Inject

/**
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
    lateinit var store: ViewModelStore

    @Inject
    lateinit var capture: ScreenCapture

    private val viewModel: MainViewModel by lazy {
        MainViewModel.getInstance(store)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let {
            if (it.hasExtra(KEY_REQUEST)) {
                when (it.getStringExtra(KEY_REQUEST)) {
                    REQUEST_EXIT_SERVICE -> {
                        stopSelf()
                        capture.release()
                    }
                }
            }
        }
        return START_STICKY
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

        viewModel.currentEvent.observe(this) {
            Log.d("event", it?.eventTitle ?: "none")
        }
    }
}
