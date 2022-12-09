package jp.seo.uma.eventchecker.data

import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import androidx.annotation.DimenRes

fun Resources.readFloat(@DimenRes id: Int): Float {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.getFloat(id)
    } else {
        val value = TypedValue()
        this.getValue(id, value, true)
        value.float
    }
}
