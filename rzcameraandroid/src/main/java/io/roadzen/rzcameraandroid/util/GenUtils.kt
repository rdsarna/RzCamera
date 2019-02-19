package io.roadzen.rzcameraandroid.util

import android.content.Context
import android.content.res.Resources
import android.os.Environment
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.Px
import androidx.appcompat.app.AlertDialog

const val LOG_TAG = "RZCameraAndroid"
const val ERROR_SAVE_FILE = "Unable to save image to file. Please try again or contact support."
const val ERROR_CAMERA = "Unable to use your device camera. Please try again."

fun showErrorDialog(msg: String, context: Context) {
    val builder: AlertDialog.Builder? = context.let {
        AlertDialog.Builder(it)
    }
    builder
        ?.setMessage(msg)
        ?.setTitle("Error")
        ?.setPositiveButton("OK", null)

    val dialog: AlertDialog? = builder?.create()
    dialog?.show()
}

/* Checks if external storage is available for read and write */
fun isExternalStorageWritable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

val Resources.navBarHeight: Int @Px get() {
    val id = getIdentifier("navigation_bar_height", "dimen", "android")
    return when {
        id > 0 -> getDimensionPixelSize(id)
        else -> 0
    }
}

enum class Orientation {
    LANDSCAPE, LANDSCAPE_REVERSE, PORTRAIT, PORTRAIT_REVERSE
}

fun getRotation(context: Context): Orientation {
    val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val rotation = display.rotation
    return when(rotation){
        Surface.ROTATION_90 -> Orientation.LANDSCAPE
        Surface.ROTATION_270 -> Orientation.LANDSCAPE_REVERSE
        Surface.ROTATION_180 -> Orientation.PORTRAIT_REVERSE
        Surface.ROTATION_0 -> Orientation.PORTRAIT
        else ->
            Orientation.PORTRAIT
    }
}