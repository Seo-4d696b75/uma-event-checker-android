package jp.seo.uma.eventchecker.core

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.SystemClock
import android.util.Log
import kotlin.math.abs

class ShakeDetector : SensorEventListener {

    private var lastEvent: AccelerometerEvent? = null
    private var lastDetectTime: Long? = null
    private var shakeCnt = 0

    val shakeEvent = LiveEvent<Unit>()

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val last = lastEvent
        val current = AccelerometerEvent(
            SystemClock.uptimeMillis(),
            event.values[0],
            event.values[1],
            event.values[2],
        )
        if (current.time - (lastDetectTime ?: 0L) > 500) {
            shakeCnt = 0
        }
        if (last == null) {
            lastEvent = current
            return
        }
        val dt = current.time - last.time
        if (dt < 100) return
        val dx = current.x - last.x
        val dy = current.y - last.y
        val dz = current.z - last.z
        val speed = abs(dx + dy + dz) / dt * 10000
        if (speed > 800) {
            lastDetectTime = current.time
            if (++shakeCnt >= 3) {
                Log.d("Sensor", "shake detected speed:$speed")
                shakeEvent.call(Unit)
                lastDetectTime = 0L
                shakeCnt = 0
            }
        }
        lastEvent = current
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }
}

private data class AccelerometerEvent(
    val time: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)