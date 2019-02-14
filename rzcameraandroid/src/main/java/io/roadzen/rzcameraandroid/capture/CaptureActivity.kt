package io.roadzen.rzcameraandroid.capture

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.lifecycle.Observer
import com.otaliastudios.cameraview.*
import io.roadzen.rzcameraandroid.R
import io.roadzen.rzcameraandroid.util.*
import kotlinx.android.synthetic.main.activity_capture.*

internal class CaptureActivity : AppCompatActivity(), FileDirectoryProvider {

    private val viewModel: CaptureViewModel by lazy { getViewModel {
        CaptureViewModel(initCaptureViewState(), this)
    } }
    private lateinit var uiHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)

        viewModel.onEvent(CaptureEvent.ScreenLoadEvent)
        setUpViews()

        viewModel.captureViewEffect.observe(this, Observer { handleViewEffect(it) })
        viewModel.captureViewState.observe(this, Observer { render(it) })
    }

    private fun setUpViews() {
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                viewModel.onEvent(CaptureEvent.SystemUiVisibleEvent)
//                uiHandler.postDelayed({
//                    hideSystemUI()
//                }, 1000)
            } else {}
        }

        cameraView.setLifecycleOwner(this)
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM)
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER)

        cameraView.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                viewModel.onEvent(CaptureEvent.ImageCapturedEvent(result))
            }

            override fun onCameraError(exception: CameraException) {
                Log.e(LOG_TAG, exception.localizedMessage)
                viewModel.onEvent(CaptureEvent.CameraErrorEvent)
            }
        })

        aspectRatioButton?.setOnClickListener { viewModel.onEvent(CaptureEvent.ChangeAspectRatioEvent) }
        flashButton?.setOnClickListener { viewModel.onEvent(CaptureEvent.ToggleFlashEvent) }
        previewImage?.setOnClickListener { viewModel.onEvent(CaptureEvent.NavigateToPreviewEvent) }
        overlayImage?.setOnClickListener { viewModel.onEvent(CaptureEvent.EnlargeMinimiseOverlayEvent) }
        enlargedOverlayImage?.setOnClickListener { viewModel.onEvent(CaptureEvent.EnlargeMinimiseOverlayEvent) }
        captureButton?.setOnClickListener { cameraView.takePicture() }
    }

    private fun render(viewState: CaptureViewState) {

        // FLASH MODE
        when (viewState.flashMode) {
            FlashMode.ON -> {
                cameraView.flash = Flash.ON
                flashButton?.setImageResource(R.drawable.ic_flash_on)
            }
            FlashMode.OFF -> {
                cameraView.flash = Flash.OFF
                flashButton?.setImageResource(R.drawable.ic_flash_off)
            }
            FlashMode.AUTO -> {
                cameraView.flash = Flash.AUTO
                flashButton?.setImageResource(R.drawable.ic_flash_auto)
            }
        }

        // OVERLAY LABEL
        viewState.overlayLabel?.let {
            overlayLabel?.visibility = View.VISIBLE
            overlayLabel?.text = it
        }

        // OVERLAY IMAGE
        if (viewState.overlayImageUri == null && viewState.overlayImageResId == null) {
            overlayImage?.visibility = View.GONE
            enlargedOverlayImage?.visibility = View.GONE
        } else {
            if (viewState.overlayImageUri?.isNotEmpty() == true) {
                GlideApp.with(this).load(viewState.overlayImageUri).into(overlayImage as ImageButton)
                GlideApp.with(this).load(viewState.overlayImageUri).into(enlargedOverlayImage as ImageView)
            }
            viewState.overlayImageResId?.let {
                GlideApp.with(this).load(it).into(overlayImage as ImageButton)
                GlideApp.with(this).load(it).into(enlargedOverlayImage as ImageView)
            }
        }

        // PREVIEW IMAGE
        if (viewState.previewImageUri?.isNotEmpty() == true) {
            GlideApp.with(this).load(viewState.previewImageUri).into(previewImage as ImageButton)
        }

        // ENLARGE OVERLAY
        if (viewState.overlayEnlarged)
            enlargedOverlayImage?.visibility = View.VISIBLE
        else
            enlargedOverlayImage?.visibility = View.GONE

        // ERROR
        viewState.error?.let {
            showErrorDialog(it, this)
        }
    }

    private fun handleViewEffect(viewEffect: CaptureViewEffect) {
        when (viewEffect) {
            is CaptureViewEffect.MakeImmersiveEffect -> hideSystemUI()
            is CaptureViewEffect.ExpandCameraPreview -> expandPreview(viewEffect.expand)
        }
    }

    private fun expandPreview(expand: Boolean) {
        if (expand) {
            hideSystemUI()
            val params = cameraView.layoutParams
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            cameraView.layoutParams = params
        } else {
            showSystemUI()
            val params = cameraView.layoutParams
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
            cameraView.layoutParams = params
        }
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_VISIBLE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    override fun getPrivateDirectory(): String {
        return filesDir.absolutePath
    }

    override fun getPublicDirectory(): String? {
        return getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
    }
}